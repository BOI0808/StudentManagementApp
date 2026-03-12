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
    const [rows] = await db.query("SELECT * FROM ThamSo");
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
