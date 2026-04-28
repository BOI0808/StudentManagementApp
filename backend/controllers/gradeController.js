const db = require("../config/db");
const xlsx = require("xlsx");

// TÁCH HÀM TÍNH ĐIỂM TRUNG BÌNH (GPA) RA NGOÀI ĐỂ CHUNG DÙNG
const calculateGPA = async (conn, MaHocSinh, MaMonHoc, MaLop) => {
  const [stats] = await conn.query(
    `SELECT 
       SUM(bd.Diem * lkt.HeSo) AS numerator,
       SUM(lkt.HeSo) AS denominator
     FROM bangdiem bd
     JOIN loaihinhkiemtra lkt ON bd.MaLoaiKiemTra = lkt.MaLoaiKiemTra
     WHERE bd.MaHocSinh = ? AND bd.MaLop = ? AND bd.MaMonHoc = ? AND bd.Diem IS NOT NULL`,
    [MaHocSinh, MaLop, MaMonHoc]
  );

  const row = stats[0];
  if (
    !row ||
    row.denominator === null ||
    row.denominator == 0 ||
    row.numerator === null
  ) {
    return null;
  }
  const gpa = Number(row.numerator) / Number(row.denominator);
  // round to 1 decimal
  return Math.round(gpa * 10) / 10;
};
// export nếu cần tái sử dụng ở nơi khác
exports.calculateGPA = calculateGPA;

//Lấy danh sách học sinh nhập điểm
exports.getHocSinhNhapDiem = async (req, res) => {
  // Giang gửi về 4 mã này từ các Dropdown
  const { MaLop, MaMonHoc, MaLoaiKiemTra, MaHocKyNamHoc } = req.query;

  if (!MaLop || !MaMonHoc || !MaLoaiKiemTra || !MaHocKyNamHoc) {
    return res
      .status(400)
      .json({ error: "Thiếu thông tin bộ lọc để lấy danh sách." });
  }

  try {
    // Dùng LEFT JOIN để lấy tên học sinh TRƯỚC, sau đó "ướm" điểm vào (nếu có)
    const query = `
      SELECT 
        hs.MaHocSinh, 
        hs.HoTen, 
        bd.Diem, 
        bd.GhiChu
      FROM hocsinh hs
      JOIN chitietlop ctl ON hs.MaHocSinh = ctl.MaHocSinh
      LEFT JOIN bangdiem bd ON hs.MaHocSinh = bd.MaHocSinh 
        AND bd.MaLop = ? 
        AND bd.MaMonHoc = ? 
        AND bd.MaLoaiKiemTra = ?
      WHERE ctl.MaLop = ?
    `;

    const [rows] = await db.query(query, [
      MaLop,
      MaMonHoc,
      MaLoaiKiemTra,
      MaLop,
    ]);

    // Format lại dữ liệu cho Giang dễ đổ vào Table
    const result = rows.map((item) => ({
      maHocSinh: item.MaHocSinh,
      hoTen: item.HoTen,
      diem: item.Diem !== null ? item.Diem : "", // Để chuỗi rỗng nếu chưa có điểm
      ghiChu: item.GhiChu || "",
    }));

    res.json(result);
  } catch (err) {
    console.error(err);
    res
      .status(500)
      .json({ error: "Lỗi hệ thống khi lấy danh sách nhập điểm." });
  }
};

exports.luuBangDiem = async (req, res) => {
  const { MaLop, MaMonHoc, MaLoaiKiemTra, DanhSachDiem } = req.body;
  if (
    !MaLop ||
    !MaMonHoc ||
    !MaLoaiKiemTra ||
    !DanhSachDiem ||
    !Array.isArray(DanhSachDiem)
  ) {
    return res.status(400).json({ error: "Dữ liệu gửi lên không hợp lệ." });
  }
  const connection = await db.getConnection();

  try {
    await connection.beginTransaction();

    // 1. Lấy quy định và thông tin lớp
    const [classInfo] = await connection.query(
      "SELECT MaHocKyNamHoc FROM lop WHERE MaLop = ?",
      [MaLop]
    );
    if (classInfo.length === 0)
      throw new Error("Mã lớp không tồn tại trong hệ thống.");
    const maHKNH = classInfo[0].MaHocKyNamHoc;

    const [range] = await connection.query(
      "SELECT ten_tham_so, gia_tri FROM thamso WHERE ten_tham_so IN ('DiemToiThieu', 'DiemToiDa')"
    );
    const limits = range.reduce((obj, item) => {
      obj[item.ten_tham_so] = item.gia_tri;
      return obj;
    }, {});

    // 2. Duyệt danh sách điểm (Sử dụng entries() để có biến i sinh ID)
    for (const [i, record] of DanhSachDiem.entries()) {
      const { maHocSinh, diem, ghiChu } = record;

      // --- LOGIC XỬ LÝ XÓA ĐIỂM / Ô TRỐNG ---
      // Nếu điểm là null, undefined hoặc chuỗi rỗng
      if (diem === null || diem === undefined || diem === "") {
        await connection.query(
          "DELETE FROM bangdiem WHERE MaLop = ? AND MaMonHoc = ? AND MaLoaiKiemTra = ? AND MaHocSinh = ?",
          [MaLop, MaMonHoc, MaLoaiKiemTra, maHocSinh]
        );
      }
      // --- LOGIC LƯU ĐIỂM NHƯ CŨ ---
      else {
        const numDiem = parseFloat(diem);
        if (numDiem < limits.DiemToiThieu || numDiem > limits.DiemToiDa) {
          throw new Error(
            `Học sinh ${maHocSinh} có điểm ${numDiem} không hợp lệ.`
          );
        }

        const [existing] = await connection.query(
          "SELECT MaBangDiem FROM bangdiem WHERE MaLop = ? AND MaMonHoc = ? AND MaLoaiKiemTra = ? AND MaHocSinh = ?",
          [MaLop, MaMonHoc, MaLoaiKiemTra, maHocSinh]
        );

        if (existing.length > 0) {
          await connection.query(
            "UPDATE bangdiem SET Diem = ?, GhiChu = ? WHERE MaBangDiem = ?",
            [numDiem, ghiChu, existing[0].MaBangDiem]
          );
        } else {
          // Sinh mã ID an toàn hơn
          const MaBangDiem = `BD${Date.now().toString().slice(-6)}${i
            .toString()
            .padStart(2, "0")}`;
          await connection.query(
            "INSERT INTO bangdiem (MaBangDiem, MaLop, MaMonHoc, MaLoaiKiemTra, MaHocSinh, Diem, GhiChu) VALUES (?, ?, ?, ?, ?, ?, ?)",
            [
              MaBangDiem,
              MaLop,
              MaMonHoc,
              MaLoaiKiemTra,
              maHocSinh,
              numDiem,
              ghiChu,
            ]
          );
        }
      }

      // 3. TÍNH LẠI ĐIỂM TRUNG BÌNH MÔN (Sau khi đã Insert/Update/Delete)
      const gpa = await calculateGPA(connection, maHocSinh, MaMonHoc, MaLop);
      await connection.query(
        `INSERT INTO ketqua_monhoc (MaHocSinh, MaMonHoc, MaHocKyNamHoc, DiemTrungBinhMon) 
         VALUES (?, ?, ?, ?) 
         ON DUPLICATE KEY UPDATE DiemTrungBinhMon = VALUES(DiemTrungBinhMon)`,
        [maHocSinh, MaMonHoc, maHKNH, gpa || 0]
      );
    }

    await connection.commit();
    res.json({ message: "Cập nhật bảng điểm thành công!" });
  } catch (err) {
    await connection.rollback();
    res.status(400).json({ error: err.message });
  } finally {
    connection.release();
  }
};

exports.importGradesExcel = async (req, res) => {
  const { MaLop, MaMonHoc, MaLoaiKiemTra, MaHocKyNamHoc } = req.body;

  if (!MaLop || !MaMonHoc || !MaLoaiKiemTra || !MaHocKyNamHoc) {
    return res.status(400).json({
      success: false,
      error: "Thiếu thông tin ngữ cảnh (Lớp, Môn, Loại KT, Học kỳ).",
    });
  }

  if (!req.file || !req.file.buffer) {
    return res
      .status(400)
      .json({ success: false, error: "Vui lòng đính kèm file Excel." });
  }

  // Parse Excel
  let rows;
  try {
    const workbook = xlsx.read(req.file.buffer, { type: "buffer" });
    const sheetName = workbook.SheetNames[0];
    if (!sheetName) {
      return res
        .status(400)
        .json({ success: false, error: "File Excel không có sheet nào." });
    }
    const worksheet = workbook.Sheets[sheetName];
    // Dùng header: 1 để lấy dòng đầu tiên làm mảng, sau đó tự mapping
    const rawData = xlsx.utils.sheet_to_json(worksheet, { header: 1 });
    if (!rawData || rawData.length < 2) {
      return res
        .status(400)
        .json({ success: false, error: "File Excel không có dữ liệu." });
    }

    const headers = rawData[0].map((h) =>
      String(h || "")
        .trim()
        .toLowerCase()
    );
    let idxMaHS = -1,
      idxDiem = -1,
      idxGhiChu = -1;

    headers.forEach((h, i) => {
      if (h.includes("Mã học sinh") || h === "mã học sinh" || h === "mã hs")
        idxMaHS = i;
      else if (h.includes("Điểm") || h === "điểm" || h === "grade") idxDiem = i;
      else if (h.includes("Ghi chú") || h === "ghi chú" || h === "note")
        idxGhiChu = i;
    });

    if (idxMaHS === -1 || idxDiem === -1) {
      return res.status(400).json({
        success: false,
        error: "File Excel thiếu cột 'Mã học sinh' hoặc 'Điểm'.",
      });
    }

    rows = [];
    for (let i = 1; i < rawData.length; i++) {
      const row = rawData[i];
      if (!row[idxMaHS]) continue;
      rows.push({
        MaHocSinh: String(row[idxMaHS]).trim(),
        Diem: row[idxDiem],
        GhiChu: idxGhiChu !== -1 ? row[idxGhiChu] : "",
      });
    }
  } catch (err) {
    console.error("Lỗi đọc file Excel:", err);
    return res
      .status(400)
      .json({ success: false, error: "Không thể đọc file Excel." });
  }

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    const [rangeRows] = await connection.query(
      "SELECT ten_tham_so, gia_tri FROM thamso WHERE ten_tham_so IN ('DiemToiThieu','DiemToiDa')"
    );
    const limits = rangeRows.reduce((acc, it) => {
      acc[it.ten_tham_so] = Number(it.gia_tri);
      return acc;
    }, {});
    const minScore = Number(limits.DiemToiThieu ?? 0);
    const maxScore = Number(limits.DiemToiDa ?? 10);

    const generateMaBangDiem = async (conn) => {
      let id;
      let exists = [];
      do {
        id = `BD${Date.now().toString().slice(-6)}${Math.floor(
          Math.random() * 900 + 100
        )}`;
        const [r] = await conn.query(
          "SELECT 1 FROM bangdiem WHERE MaBangDiem = ? LIMIT 1",
          [id]
        );
        exists = r;
      } while (exists.length > 0);
      return id;
    };

    let processed = 0;
    for (const item of rows) {
      const { MaHocSinh, Diem, GhiChu } = item;

      const [inClass] = await connection.query(
        "SELECT 1 FROM chitietlop WHERE MaLop = ? AND MaHocSinh = ? LIMIT 1",
        [MaLop, MaHocSinh]
      );
      if (inClass.length === 0) continue; // Bỏ qua nếu HS không thuộc lớp

      let scoreValue = null;
      if (Diem !== "" && Diem !== null && Diem !== undefined) {
        scoreValue = parseFloat(String(Diem).replace(",", "."));
        if (isNaN(scoreValue) || scoreValue < minScore || scoreValue > maxScore)
          continue;
      }

      const [existing] = await connection.query(
        "SELECT MaBangDiem FROM bangdiem WHERE MaLop = ? AND MaMonHoc = ? AND MaLoaiKiemTra = ? AND MaHocSinh = ? LIMIT 1",
        [MaLop, MaMonHoc, MaLoaiKiemTra, MaHocSinh]
      );

      if (existing.length > 0) {
        await connection.query(
          "UPDATE bangdiem SET Diem = ?, GhiChu = ? WHERE MaBangDiem = ?",
          [scoreValue, GhiChu || null, existing[0].MaBangDiem]
        );
      } else {
        const MaBangDiem = await generateMaBangDiem(connection);
        await connection.query(
          "INSERT INTO bangdiem (MaBangDiem, MaHocSinh, MaLop, MaMonHoc, MaLoaiKiemTra, Diem, GhiChu) VALUES (?, ?, ?, ?, ?, ?, ?)",
          [
            MaBangDiem,
            MaHocSinh,
            MaLop,
            MaMonHoc,
            MaLoaiKiemTra,
            scoreValue,
            GhiChu || null,
          ]
        );
      }

      const gpa = await calculateGPA(connection, MaHocSinh, MaMonHoc, MaLop);
      await connection.query(
        `INSERT INTO ketqua_monhoc (MaHocSinh, MaMonHoc, MaHocKyNamHoc, DiemTrungBinhMon)
         VALUES (?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE DiemTrungBinhMon = VALUES(DiemTrungBinhMon)`,
        [MaHocSinh, MaMonHoc, MaHocKyNamHoc, gpa || 0]
      );
      processed++;
    }

    await connection.commit();
    return res.json({
      success: true,
      message: `Đã import thành công ${processed} học sinh.`,
    });
  } catch (err) {
    await connection.rollback();
    res.status(400).json({ success: false, error: err.message });
  } finally {
    connection.release();
  }
};
