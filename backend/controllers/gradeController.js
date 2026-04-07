const db = require("../config/db");

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
      const [stats] = await connection.query(
        `SELECT SUM(bd.Diem * lkt.HeSo) / SUM(lkt.HeSo) AS DiemTB 
         FROM bangdiem bd 
         JOIN loaihinhkiemtra lkt ON bd.MaLoaiKiemTra = lkt.MaLoaiKiemTra 
         WHERE bd.MaHocSinh = ? AND bd.MaLop = ? AND bd.MaMonHoc = ?`,
        [maHocSinh, MaLop, MaMonHoc]
      );

      // Nếu stats[0].DiemTB là null (tức là đã xóa hết sạch điểm của môn đó), để là 0 hoặc null
      const diemTB =
        stats[0].DiemTB !== null ? parseFloat(stats[0].DiemTB).toFixed(1) : 0;

      await connection.query(
        `INSERT INTO ketqua_monhoc (MaHocSinh, MaMonHoc, MaHocKyNamHoc, DiemTrungBinhMon) 
         VALUES (?, ?, ?, ?) 
         ON DUPLICATE KEY UPDATE DiemTrungBinhMon = VALUES(DiemTrungBinhMon)`,
        [maHocSinh, MaMonHoc, maHKNH, diemTB]
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
