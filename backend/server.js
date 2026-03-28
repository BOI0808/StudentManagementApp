// Import các thư viện cần thiết
const express = require("express");
const mysql = require("mysql2");
const cors = require("cors");
require("dotenv").config();

// Tạo ứng dụng Express
const app = express();

// Sử dụng middleware
app.use(cors());
app.use(express.json());

// Tạo pool kết nối
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "quan_ly_hoc_sinh",
  waitForConnections: true,
  connectionLimit: process.env.DB_CONN_LIMIT || 10,
  queueLimit: 0,
});

// Tạo đối tượng db từ pool
const db = pool.promise();
pool.getConnection((err, connection) => {
  if (err) {
    console.error("Kết nối Database thất bại: " + err.message);
    return;
  }
  console.log("Đã kết nối MySQL qua Connection Pool thành công!");
  connection.release();
});

// API kiểm tra kết nối
app.get("/", (req, res) => {
  res.send("Server Backend đã được tối ưu!");
});

//Khởi động server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server đang chạy tại: http://localhost:${PORT}`);
});

// API lấy quy định
app.get("/api/quy-dinh", async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM thamso");
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi lấy quy định" });
  }
});

// API lấy danh sách môn học (Dùng cho BM4, BM9)
app.get("/api/mon-hoc", async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM monhoc");
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi lấy danh sách môn học" });
  }
});

// API lấy danh sách lớp học (Dùng cho BM6)
app.get("/api/lop", async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM lop");
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi lấy danh sách lớp" });
  }
});

// API lấy danh sách giới tính (Dùng cho BM5)
app.get("/api/gioi-tinh", async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM gioitinh");
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi lấy danh mục giới tính" });
  }
});

// API Lấy danh sách Học kỳ & Năm học
app.get("/api/hoc-ky-nam-hoc", async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM hocky_namhoc");
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi lấy danh sách học kỳ" });
  }
});

// Lấy danh sách Loại hình kiểm tra (để biết hệ số)
app.get("/api/loai-kiem-tra", async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM loaihinhkiemtra");
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi lấy loại hình kiểm tra" });
  }
});

// API Lấy danh sách học sinh của lớp
app.get("/api/lop/:maLop/hoc-sinh", async (req, res) => {
  try {
    const { maLop } = req.params;
    const query = `
      SELECT hs.MaHocSinh, hs.HoTen 
      FROM hocsinh hs 
      JOIN chitietlop ctl ON hs.MaHocSinh = ctl.MaHocSinh 
      WHERE ctl.MaLop = ?`;
    const [rows] = await db.query(query, [maLop]);
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi lấy danh sách học sinh của lớp" });
  }
});

// API Tra cứu học sinh
app.get("/api/tra-cuu-hoc-sinh", async (req, res) => {
  const { ten } = req.query;
  try {
    const query = `
      SELECT hs.MaHocSinh, hs.HoTen, l.TenLop, hs.Email
      FROM hocsinh hs
      LEFT JOIN chitietlop ctl ON hs.MaHocSinh = ctl.MaHocSinh
      LEFT JOIN lop l ON ctl.MaLop = l.MaLop
      WHERE hs.HoTen LIKE ?`;
    const [rows] = await db.query(query, [`%${ten}%`]);
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi tra cứu học sinh" });
  }
});

// API Báo cáo tổng kết môn (Hiện thực hóa BM10 & QĐ10)
app.get("/api/bao-cao-mon", async (req, res) => {
  const { MaMon, MaHocKyNamHoc } = req.query; // Nhận tham số lọc từ Android

  try {
    const query = `
      SELECT 
        l.TenLop, 
        COUNT(ctl.MaHocSinh) as SiSo, 
        SUM(CASE WHEN kq.DiemTrungBinhMon >= 5 THEN 1 ELSE 0 END) as SoLuongDat
      FROM lop l
      JOIN chitietlop ctl ON l.MaLop = ctl.MaLop
      LEFT JOIN ketqua_monhoc kq ON ctl.MaHocSinh = kq.MaHocSinh 
        AND kq.MaLop = l.MaLop 
        AND kq.MaMonHoc = ? 
        AND kq.MaHocKyNamHoc = ?
      GROUP BY l.MaLop, l.TenLop`;

    const [rows] = await db.query(query, [MaMon, MaHocKyNamHoc]);

    // Tính tỉ lệ % ngay trên Server để Android chỉ việc hiển thị
    const reportData = rows.map((row) => ({
      ...row,
      TiLe:
        row.SiSo > 0
          ? ((row.SoLuongDat / row.SiSo) * 100).toFixed(2) + "%"
          : "0%",
    }));

    res.json(reportData);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi khi lập báo cáo tổng kết môn" });
  }
});

// API Báo cáo tổng kết học kỳ (Hiện thực hóa BM11)
app.get("/api/bao-cao-hoc-ky", async (req, res) => {
  const { MaHocKyNamHoc } = req.query;

  try {
    const query = `
      SELECT 
        l.TenLop, 
        COUNT(ctl.MaHocSinh) as SiSo,
        (
          SELECT COUNT(*) 
          FROM (
            SELECT MaHocSinh 
            FROM ketqua_monhoc 
            WHERE MaHocKyNamHoc = ? AND MaLop = l.MaLop
            GROUP BY MaHocSinh 
            HAVING MIN(DiemTrungBinhMon) >= 5
          ) as HocSinhDatHK
        ) as SoLuongDat
      FROM lop l
      JOIN chitietlop ctl ON l.MaLop = ctl.MaLop
      WHERE l.MaHocKyNamHoc = ?
      GROUP BY l.MaLop, l.TenLop`;

    const [rows] = await db.query(query, [MaHocKyNamHoc, MaHocKyNamHoc]);

    const reportData = rows.map((row) => ({
      ...row,
      TiLe:
        row.SiSo > 0
          ? ((row.SoLuongDat / row.SiSo) * 100).toFixed(2) + "%"
          : "0%",
    }));

    res.json(reportData);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi khi lập báo cáo tổng kết học kỳ" });
  }
});

// CÁC API QUẢN LÝ HỌC SINH VÀ LỚP HỌC SẼ ĐƯỢC THÊM VÀO DƯỚI ĐÂY
// API Tiếp nhận học sinh (Hiện thực hóa Biểu mẫu 5 & Quy định 4, 5)
app.post("/api/tiep-nhan-hoc-sinh", async (req, res) => {
  const { HoTen, NgaySinh, MaGioiTinh, DiaChi, Email } = req.body;

  try {
    // 1. Lấy quy định về tuổi từ bảng ThamSo
    const [config] = await db.query(
      "SELECT ten_tham_so, gia_tri FROM thamso WHERE ten_tham_so IN ('TuoiToiThieu', 'TuoiToiDa')"
    );
    const minAge = config.find((c) => c.ten_tham_so === "TuoiToiThieu").gia_tri;
    const maxAge = config.find((c) => c.ten_tham_so === "TuoiToiDa").gia_tri;

    // 2. Tính tuổi học sinh (B6 trong thuật toán báo cáo)
    const birthYear = new Date(NgaySinh).getFullYear();
    const currentYear = new Date().getFullYear();
    const age = currentYear - birthYear;

    // 3. Kiểm tra điều kiện tuổi (QĐ4)
    if (age < minAge || age > maxAge) {
      return res.status(400).json({
        error: `Tuổi học sinh phải từ ${minAge} đến ${maxAge}. Hiện tại là ${age} tuổi.`,
      });
    }

    // 4. Tạo mã học sinh tự động (Ví dụ: HS + timestamp)
    const MaHocSinh = "HS" + Date.now().toString().slice(-8);

    // 5. Lưu vào bảng hocsinh (B9 trong thuật toán báo cáo)
    const query =
      "INSERT INTO hocsinh (MaHocSinh, HoTen, NgaySinh, MaGioiTinh, DiaChi, Email) VALUES (?, ?, ?, ?, ?, ?)";
    await db.query(query, [
      MaHocSinh,
      HoTen,
      NgaySinh,
      MaGioiTinh,
      DiaChi,
      Email,
    ]);

    res.json({ message: "Tiếp nhận học sinh thành công!", MaHocSinh });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi tiếp nhận hồ sơ" });
  }
});

// API Lập danh sách lớp (Hiện thực hóa Biểu mẫu 6 & Quy định 6)
app.post("/api/lap-danh-sach-lop", async (req, res) => {
  const { MaLop, MaHocSinh } = req.body;

  try {
    // 1. Lấy sĩ số tối đa từ bảng thamso
    const [config] = await db.query(
      "SELECT gia_tri FROM thamso WHERE ten_tham_so = 'SiSoToiDa'"
    );
    const maxSiSo = config[0].gia_tri;

    // 2. Tính sĩ số hiện tại của lớp (B7 trong thuật toán)
    const [currentSiSo] = await db.query(
      "SELECT COUNT(*) as count FROM chitietlop WHERE MaLop = ?",
      [MaLop]
    );

    // 3. Kiểm tra sĩ số (QĐ6)
    if (currentSiSo[0].count >= maxSiSo) {
      return res
        .status(400)
        .json({ error: `Lớp đã đầy! Sĩ số tối đa là ${maxSiSo} học sinh.` });
    }

    // 4. Lưu vào bảng chitietlop (B10 trong thuật toán)
    await db.query("INSERT INTO chitietlop (MaLop, MaHocSinh) VALUES (?, ?)", [
      MaLop,
      MaHocSinh,
    ]);

    res.json({ message: "Xếp lớp cho học sinh thành công!" });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi lập danh sách lớp" });
  }
});
