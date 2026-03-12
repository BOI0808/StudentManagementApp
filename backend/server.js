const express = require("express");
const mysql = require("mysql2");
const cors = require("cors");
const bodyParser = require("body-parser");
require("dotenv").config();

const app = express();
app.use(cors());
app.use(bodyParser.json());

const db = mysql.createConnection({
  host: "localhost",
  user: "root",
  password: "",
  database: "quan_ly_hoc_sinh",
});

db.connect((err) => {
  if (err) {
    console.error("Kết nối Database thất bại: " + err.message);
    return;
  }
  console.log("Đã kết nối MySQL thành công!");
});

app.get("/", (req, res) => {
  res.send("Server Backend đã sẵn sàng!");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server đang chạy tại: http://localhost:${PORT}`);
});
