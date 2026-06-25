const db = require("../config/db");
const xlsx = require("xlsx");

const generateMaHocSinh = async (connection) => {
  const prefix = "HS";
  const year = new Date().getFullYear().toString().slice(-2);
  const searchPattern = `${prefix}${year}%`;
  const [rows] = await connection.query(
    "SELECT MaHocSinh FROM hocsinh WHERE MaHocSinh LIKE ? ORDER BY MaHocSinh DESC LIMIT 1",
    [searchPattern]
  );
  let nextNumber = 1;
  if (rows.length > 0) {
    const lastNumber = parseInt(rows[0].MaHocSinh.slice(-4));
    nextNumber = lastNumber + 1;
  }
  return `${prefix}${year}${nextNumber.toString().padStart(4, "0")}`;
};

exports.tiepNhanHocSinh = async (req, res) => {
  const { HoTen, NgaySinh, MaGioiTinh, DiaChi, Email } = req.body;
  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();
    if (!HoTen || !NgaySinh || !MaGioiTinh || !DiaChi) {
      return res
        .status(400)
        .json({ error: "Vui lòng nhập đầy đủ thông tin bắt buộc." });
    }
    const dateObj = new Date(NgaySinh);
    const formattedDate = dateObj.toISOString().split("T")[0];
    const [config] = await connection.query(
      "SELECT ten_tham_so, gia_tri FROM thamso WHERE ten_tham_so IN ('TuoiToiThieu', 'TuoiToiDa')"
    );
    const minAge =
      config.find((c) => c.ten_tham_so === "TuoiToiThieu")?.gia_tri || 15;
    const maxAge =
      config.find((c) => c.ten_tham_so === "TuoiToiDa")?.gia_tri || 20;
    const age = new Date().getFullYear() - dateObj.getFullYear();
    if (age < minAge || age > maxAge) {
      await connection.rollback();
      return res.status(400).json({
        error: `Tuổi (${age}) không hợp lệ (QĐ: ${minAge}-${maxAge}).`,
      });
    }
    const MaHocSinh = await generateMaHocSinh(connection);
    await connection.query(
      `INSERT INTO hocsinh (MaHocSinh, HoTen, NgaySinh, MaGioiTinh, DiaChi, Email) VALUES (?, ?, ?, ?, ?, ?)`,
      [MaHocSinh, HoTen.trim(), formattedDate, MaGioiTinh, DiaChi.trim(), Email]
    );
    await connection.commit();
    res.json({ message: "Thành công!", MaHocSinh: MaHocSinh });
  } catch (err) {
    await connection.rollback();
    res.status(500).json({ error: "Lỗi hệ thống." });
  } finally {
    connection.release();
  }
};

exports.importStudentsExcel = async (req, res) => {
  if (!req.file || !req.file.buffer) {
    return res.status(400).json({ error: "Vui lòng đính kèm file Excel." });
  }

  let rows;
  try {
    const workbook = xlsx.read(req.file.buffer, { type: "buffer" });
    const worksheet = workbook.Sheets[workbook.SheetNames[0]];
    const rawData = xlsx.utils.sheet_to_json(worksheet, { defval: "" });
    rows = rawData.map((row) => {
      const newRow = {};
      Object.keys(row).forEach((key) => {
        newRow[key.trim()] = row[key];
      });
      return newRow;
    });
  } catch (err) {
    return res.status(400).json({ error: "Không thể đọc file Excel." });
  }

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    const [configRows] = await connection.query(
      "SELECT ten_tham_so, gia_tri FROM thamso WHERE ten_tham_so IN ('TuoiToiThieu','TuoiToiDa')"
    );
    const minAge =
      Number(
        configRows.find((c) => c.ten_tham_so === "TuoiToiThieu")?.gia_tri
      ) || 0;
    const maxAge =
      Number(configRows.find((c) => c.ten_tham_so === "TuoiToiDa")?.gia_tri) ||
      200;

    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];
      const excelRowNumber = i + 2;

      const HoTen = String(
        row["Họ và Tên"] || row["Họ và tên"] || row.HoTen || row["Họ Tên"] || ""
      ).trim();
      const NgaySinhRaw = row["Ngày sinh"] || row.NgaySinh || "";
      const DiaChi = String(row["Địa chỉ"] || row.DiaChi || "").trim();
      const Email = String(row.Email || row["Email"] || "").trim();
      const GioiTinhRaw = String(
        row["Giới tính"] || row.GioiTinh || row.MaGioiTinh || ""
      ).trim();

      let MaGioiTinh = "";
      if (/nam/i.test(GioiTinhRaw)) MaGioiTinh = "GT1";
      else if (/nữ|nu/i.test(GioiTinhRaw)) MaGioiTinh = "GT2";
      else if (/khác|khac/i.test(GioiTinhRaw)) MaGioiTinh = "GT3";

      if (!HoTen || !NgaySinhRaw || !MaGioiTinh || !DiaChi) {
        throw new Error(
          `Dòng ${excelRowNumber}: Thiếu thông tin bắt buộc hoặc Giới tính không hợp lệ.`
        );
      }

      let dateObj;
      if (typeof NgaySinhRaw === "number") {
        dateObj = new Date(Math.round((NgaySinhRaw - 25569) * 86400 * 1000));
      } else {
        dateObj = new Date(NgaySinhRaw);
      }

      if (isNaN(dateObj))
        throw new Error(
          `Dòng ${excelRowNumber}: Ngày sinh không đúng định dạng.`
        );
      const formattedDate = dateObj.toISOString().split("T")[0];

      const currentYear = new Date().getFullYear();
      const birthYear = dateObj.getFullYear();
      const age = currentYear - birthYear;
      if (age < minAge || age > maxAge) {
        throw new Error(
          `Dòng ${excelRowNumber}: Học sinh ${HoTen} có tuổi là ${age}, không nằm trong quy định (${minAge}-${maxAge}).`
        );
      }

      const MaHocSinh = await generateMaHocSinh(connection);
      await connection.query(
        "INSERT INTO hocsinh (MaHocSinh, HoTen, NgaySinh, MaGioiTinh, DiaChi, Email) VALUES (?, ?, ?, ?, ?, ?)",
        [MaHocSinh, HoTen, formattedDate, MaGioiTinh, DiaChi, Email || null]
      );
    }

    await connection.commit();
    res.json({
      success: true,
      message: `Đã nhập thành công ${rows.length} học sinh.`,
    });
  } catch (err) {
    await connection.rollback();
    res.status(400).json({ success: false, error: err.message });
  } finally {
    connection.release();
  }
};

exports.searchHocSinh = async (req, res) => {
  const { key } = req.query;
  const [rows] = await db.query(
    "SELECT MaHocSinh, HoTen, DATE_FORMAT(NgaySinh, '%Y-%m-%d') AS NgaySinh, MaGioiTinh FROM hocsinh WHERE HoTen LIKE ? OR MaHocSinh LIKE ? LIMIT 15",
    [`%${key}%`, `%${key}%`]
  );
  res.json(rows);
};

exports.updateHocSinh = async (req, res) => {
  const { MaHocSinh, HoTen, NgaySinh, MaGioiTinh, DiaChi, Email } = req.body;
  try {
    const formattedDate = new Date(NgaySinh).toISOString().split("T")[0];
    await db.query(
      "UPDATE hocsinh SET HoTen = ?, NgaySinh = ?, MaGioiTinh = ?, DiaChi = ?, Email = ? WHERE MaHocSinh = ?",
      [HoTen.trim(), formattedDate, MaGioiTinh, DiaChi.trim(), Email, MaHocSinh]
    );
    res.json({ message: "Thành công!" });
  } catch (err) {
    res.status(400).json({ error: "Lỗi cập nhật." });
  }
};

exports.xoaHocSinhKhoiLop = async (req, res) => {
  const { MaLop, MaHocSinh } = req.body;
  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();
    await connection.query(
      "DELETE FROM chitietlop WHERE MaLop = ? AND MaHocSinh = ?",
      [MaLop, MaHocSinh]
    );
    await connection.query("UPDATE lop SET SiSo = SiSo - 1 WHERE MaLop = ?", [
      MaLop,
    ]);
    await connection.commit();
    res.json({ message: "Thành công!" });
  } catch (err) {
    await connection.rollback();
    res.status(500).json({ error: "Lỗi hệ thống." });
  } finally {
    connection.release();
  }
};

exports.traCuuHocSinhTheoTenHoacMa = async (req, res) => {
  const { maHocSinh, hoTen } = req.query;

  if (!maHocSinh && !hoTen) {
    return res.status(400).json({
      error: "Vui lòng nhập mã học sinh hoặc họ tên",
    });
  }

  try {
    let searchQuery = "SELECT * FROM hocsinh WHERE 1=1";
    let searchParams = [];

    if (maHocSinh) {
      searchQuery += " AND MaHocSinh LIKE ?";
      searchParams.push(`%${maHocSinh.trim()}%`);
    }
    if (hoTen) {
      searchQuery += " AND HoTen LIKE ?";
      searchParams.push(`%${hoTen}%`);
    }

    searchQuery += " LIMIT 10";

    const [students] = await db.query(searchQuery, searchParams);

    if (!students || students.length === 0) {
      return res.status(404).json({
        error: "Không tìm thấy học sinh phù hợp",
      });
    }

    const result = await Promise.all(
      students.map(async (student) => {
        const maHS = student.MaHocSinh;

        const [classes] = await db.query(
          `
          SELECT 
            l.MaLop,
            l.TenLop,
            hn.NamHocBatDau,
            hn.NamHocKetThuc,
            CONCAT(hn.NamHocBatDau, '-', hn.NamHocKetThuc) AS NamHoc
          FROM chitietlop ctl
          JOIN lop l ON ctl.MaLop = l.MaLop
          JOIN hocky_namhoc hn ON l.MaHocKyNamHoc = hn.MaHocKyNamHoc
          WHERE ctl.MaHocSinh = ?
          GROUP BY l.TenLop, hn.NamHocBatDau -- Nhóm lại để tránh lặp lớp giữa các học kỳ trên danh sách search
          ORDER BY hn.NamHocBatDau DESC
          `,
          [maHS]
        );

        return {
          ...student,
          classes: classes,
        };
      })
    );

    res.json(result);
  } catch (error) {
    console.error("Lỗi tra cứu:", error);
    res.status(500).json({
      error: "Lỗi truy vấn: " + error.message,
    });
  }
};

exports.getStudentHistory = async (req, res) => {
  const { maHocSinh } = req.params;
  const query = `
    SELECT 
      l.TenLop, 
      hn.TenHocKy, 
      CONCAT(hn.NamHocBatDau, '-', hn.NamHocKetThuc) AS NamHoc
    FROM chitietlop ctl
    JOIN lop l ON ctl.MaLop = l.MaLop
    JOIN hocky_namhoc hn ON l.MaHocKyNamHoc = hn.MaHocKyNamHoc
    WHERE ctl.MaHocSinh = ?
    ORDER BY hn.NamHocBatDau DESC, hn.TenHocKy DESC`;

  try {
    const [rows] = await db.query(query, [maHocSinh]);
    res.json(rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.getStudentScoreDetails = async (req, res) => {
  const { maHocSinh } = req.query;

  if (!maHocSinh) {
    return res.status(400).json({ error: "Thiếu mã học sinh." });
  }

  try {
    const [yearsRows] = await db.query(
      `SELECT DISTINCT hn.NamHocBatDau, hn.NamHocKetThuc, l.TenLop
       FROM chitietlop ctl
       JOIN lop l ON ctl.MaLop = l.MaLop
       JOIN hocky_namhoc hn ON l.MaHocKyNamHoc = hn.MaHocKyNamHoc
       WHERE ctl.MaHocSinh = ?
       ORDER BY hn.NamHocBatDau DESC`,
      [maHocSinh]
    );

    if (yearsRows.length === 0) {
      return res
        .status(404)
        .json({ error: "Học sinh này hiện chưa được xếp vào lớp nào." });
    }

    const getScoresByClassId = async (maLop) => {
      const [subjects] = await db.query(
        `SELECT DISTINCT mh.MaMonHoc, mh.TenMonHoc 
         FROM monhoc mh
         JOIN bangdiem bd ON mh.MaMonHoc = bd.MaMonHoc
         WHERE bd.MaHocSinh = ? AND bd.MaLop = ? AND mh.TrangThai = 1`,
        [maHocSinh, maLop]
      );

      return await Promise.all(
        subjects.map(async (subject) => {
          const [scores] = await db.query(
            `SELECT lkt.TenLoaiKiemTra, lkt.HeSo, bd.Diem, bd.GhiChu
             FROM bangdiem bd
             JOIN loaihinhkiemtra lkt ON bd.MaLoaiKiemTra = lkt.MaLoaiKiemTra
             WHERE bd.MaHocSinh = ? AND bd.MaLop = ? AND bd.MaMonHoc = ? AND lkt.TrangThai = 1
             ORDER BY lkt.HeSo ASC`,
            [maHocSinh, maLop, subject.MaMonHoc]
          );

          const [avgRows] = await db.query(
            `SELECT DiemTrungBinhMon FROM ketqua_monhoc 
             WHERE MaHocSinh = ? AND MaMonHoc = ? AND MaHocKyNamHoc = (SELECT MaHocKyNamHoc FROM lop WHERE MaLop = ?)`,
            [maHocSinh, subject.MaMonHoc, maLop]
          );
          return {
            MaMonHoc: subject.MaMonHoc,
            TenMonHoc: subject.TenMonHoc,
            DiemTrungBinhMon:
              avgRows.length > 0 ? avgRows[0].DiemTrungBinhMon : null,
            DanhSachDiemChiTiet: scores,
          };
        })
      );
    };

    const classHistoryDetails = await Promise.all(
      yearsRows.map(async (yearRow) => {
        const { NamHocBatDau, NamHocKetThuc, TenLop } = yearRow;

        const [hk1Class] = await db.query(
          `SELECT l.MaLop FROM lop l JOIN hocky_namhoc hn ON l.MaHocKyNamHoc = hn.MaHocKyNamHoc 
           WHERE l.TenLop = ? AND hn.NamHocBatDau = ? AND hn.TenHocKy = 'Học kỳ 1' LIMIT 1`,
          [TenLop, NamHocBatDau]
        );
        const scoresHK1 =
          hk1Class.length > 0
            ? await getScoresByClassId(hk1Class[0].MaLop)
            : [];

        const [hk2Class] = await db.query(
          `SELECT l.MaLop FROM lop l JOIN hocky_namhoc hn ON l.MaHocKyNamHoc = hn.MaHocKyNamHoc 
           WHERE l.TenLop = ? AND hn.NamHocBatDau = ? AND hn.TenHocKy = 'Học kỳ 2' LIMIT 1`,
          [TenLop, NamHocBatDau]
        );
        const scoresHK2 =
          hk2Class.length > 0
            ? await getScoresByClassId(hk2Class[0].MaLop)
            : [];

        const [termScores] = await db.query(
          `SELECT 
             ROUND(AVG(CASE WHEN hn.TenHocKy = 'Học kỳ 1' THEN km.DiemTrungBinhMon END), 1) AS DiemHK1,
             ROUND(AVG(CASE WHEN hn.TenHocKy = 'Học kỳ 2' THEN km.DiemTrungBinhMon END), 1) AS DiemHK2,
             ROUND(AVG(km.DiemTrungBinhMon), 1) AS DiemCaNam
           FROM ketqua_monhoc km
           JOIN hocky_namhoc hn ON km.MaHocKyNamHoc = hn.MaHocKyNamHoc
           WHERE km.MaHocSinh = ? AND hn.NamHocBatDau = ?`,
          [maHocSinh, NamHocBatDau]
        );

        return {
          TenLop,
          NamHoc: `${NamHocBatDau}-${NamHocKetThuc}`,
          MonHocHocKy1: scoresHK1,
          MonHocHocKy2: scoresHK2,
          TongKetChung: termScores[0] || {
            DiemHK1: null,
            DiemHK2: null,
            DiemCaNam: null,
          },
        };
      })
    );

    res.json(classHistoryDetails);
  } catch (error) {
    console.error("Lỗi lấy điểm gộp năm học:", error);
    res.status(500).json({ error: "Lỗi hệ thống: " + error.message });
  }
};
