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
    // Chuyển đổi sang JSON và chuẩn hóa key (xóa khoảng trắng thừa ở tiêu đề)
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

      // ÁNH XẠ THÔNG MINH (Case-insensitive & hỗ trợ cả Tiếng Việt/Anh)
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

      // Xử lý ngày sinh linh hoạt
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

      // Kiểm tra tuổi
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

exports.traCuuHocSinh = async (req, res) => {
  const { maLop, maHocSinh, hoTen } = req.query;
  let query = `
    SELECT 
      hs.MaHocSinh, 
      hs.HoTen, 
      l.TenLop, 
      CONCAT(hn.NamHocBatDau, '-', hn.NamHocKetThuc) AS NamHoc,
      -- Tính điểm trung bình HK1 của học sinh trong năm học đó
      (SELECT ROUND(AVG(km1.DiemTrungBinhMon), 1) 
       FROM ketqua_monhoc km1 
       JOIN hocky_namhoc hn1 ON km1.MaHocKyNamHoc = hn1.MaHocKyNamHoc 
       WHERE km1.MaHocSinh = hs.MaHocSinh 
         AND hn1.NamHocBatDau = hn.NamHocBatDau 
         AND hn1.TenHocKy = 'Học kỳ 1') AS DiemHK1,
      -- Tính điểm trung bình HK2
      (SELECT ROUND(AVG(km2.DiemTrungBinhMon), 1) 
       FROM ketqua_monhoc km2 
       JOIN hocky_namhoc hn2 ON km2.MaHocKyNamHoc = hn2.MaHocKyNamHoc 
       WHERE km2.MaHocSinh = hs.MaHocSinh 
         AND hn2.NamHocBatDau = hn.NamHocBatDau 
         AND hn2.TenHocKy = 'Học kỳ 2') AS DiemHK2,
      -- Tính điểm trung bình Cả năm
      (SELECT ROUND(AVG(km3.DiemTrungBinhMon), 1) 
       FROM ketqua_monhoc km3 
       JOIN hocky_namhoc hn3 ON km3.MaHocKyNamHoc = hn3.MaHocKyNamHoc 
       WHERE km3.MaHocSinh = hs.MaHocSinh 
         AND hn3.NamHocBatDau = hn.NamHocBatDau) AS DiemCaNam
    FROM hocsinh hs 
    JOIN chitietlop ctl ON hs.MaHocSinh = ctl.MaHocSinh 
    JOIN lop l ON ctl.MaLop = l.MaLop 
    JOIN hocky_namhoc hn ON l.MaHocKyNamHoc = hn.MaHocKyNamHoc 
    WHERE 1=1`;

  let params = [];
  if (maLop) {
    query += " AND l.MaLop = ?";
    params.push(maLop);
  }
  if (maHocSinh) {
    query += " AND hs.MaHocSinh = ?";
    params.push(maHocSinh);
  }
  if (hoTen) {
    query += " AND hs.HoTen LIKE ?";
    params.push(`%${hoTen}%`);
  }

  try {
    const [rows] = await db.query(query, params);
    res.json(rows);
  } catch (error) {
    res
      .status(500)
      .json({ error: "Lỗi truy vấn cơ sở dữ liệu: " + error.message });
  }
};
