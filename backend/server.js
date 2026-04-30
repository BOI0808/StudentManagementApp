const express = require("express");
const cors = require("cors");
const studentRoutes = require("./routes/studentRoutes"); // Kết nối file route bạn đã tạo
const configRoutes = require("./routes/configRoutes");
const gradeRoutes = require("./routes/gradeRoutes");
const reportRoutes = require("./routes/reportRoutes");
const authRoutes = require("./routes/authRoutes");
const classRoutes = require("./routes/classRoutes");
const subjectRoutes = require("./routes/subjectRoutes");
const semesterRoutes = require("./routes/semesterRoutes");
const testTypeRoutes = require("./routes/testTypeRoutes");
const userRoutes = require("./routes/userRoutes");
const blockRoutes = require("./routes/blockRoutes");

const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// Kết nối các nhóm Route (Đây là nơi Vinh/Giang sẽ gọi API)
// Đường dẫn sẽ là: http://localhost:3000/api/students/tiep-nhan-hoc-sinh và tương tự
app.use("/api/students", studentRoutes);
app.use("/api/configs", configRoutes);
app.use("/api/grades", gradeRoutes);
app.use("/api/reports", reportRoutes);
app.use("/api/auths", authRoutes);
app.use("/api/classes", classRoutes);
app.use("/api/subjects", subjectRoutes);
app.use("/api/semesters", semesterRoutes);
app.use("/api/test-types", testTypeRoutes);
app.use("/api/users", userRoutes);
app.use("/api/blocks", blockRoutes);

const pool = require("./config/db"); // Import pool từ file db.js

async function startServer() {
  try {
    // Thử thực hiện một truy vấn đơn giản để kiểm tra kết nối
    await pool.query("SELECT 1");
    console.log("✅ Kết nối Database thành công!");

    const PORT = process.env.PORT || 3000;
    app.listen(PORT, () => {
      console.log(`🚀 Server đang chạy tại: http://localhost:${PORT}`);
    });
  } catch (error) {
    console.error("❌ Không thể kết nối Database. Server không thể khởi động.");
    console.error(error.message);
    process.exit(1); // Dừng tiến trình Node.js với mã lỗi
  }
}

startServer();
