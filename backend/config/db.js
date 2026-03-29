const mysql = require("mysql2");
require("dotenv").config();

// Tạo pool kết nối
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "quan_ly_hoc_sinh",
  waitForConnections: true,
  connectionLimit: parseInt(process.env.DB_CONN_LIMIT, 10) || 10,
  queueLimit: 0,
});

//
module.exports = pool.promise();
