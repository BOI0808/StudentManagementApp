const express = require("express");
const mysql = require("mysql2");
const cors = require("cors");
require("dotenv").config();

const app = express();

app.use(cors());
app.use(express.json());

const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "quan_ly_hoc_sinh",
  waitForConnections: true,
  connectionLimit: process.env.DB_CONN_LIMIT || 10,
  queueLimit: 0,
});

const db = pool.promise();
pool.getConnection((err, connection) => {
  if (err) {
    console.error("Kết nối Database thất bại: " + err.message);
    return;
  }
  console.log("Đã kết nối MySQL qua Connection Pool thành công!");
  connection.release();
});

app.get("/", (req, res) => {
  res.send("Server Backend đã được tối ưu!");
});

app.get("/api/quy-dinh", async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM thamso");
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi truy vấn cơ sở dữ liệu" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server đang chạy tại: http://localhost:${PORT}`);
});

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
      return res
        .status(400)
        .json({
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
