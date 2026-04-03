const express = require("express");
const router = express.Router();
const studentController = require("../controllers/studentController");

// API Tiếp nhận học sinh (BM6)
router.post("/tiep-nhan-hoc-sinh", studentController.tiepNhanHocSinh);
// Endpoint: GET /api/hocsinh/search?key=Nguyen
router.get("/search", studentController.searchHocSinh);
// API lấy danh sách học sinh của một lớp cụ thể
router.get(
  "/danh-sach-hoc-sinh-theo-lop/:MaLop",
  studentController.getHocSinhTheoLop
);
// Endpoint: POST /api/hocsinh/them-hoc-sinh
router.post("/them-hoc-sinh", studentController.themHocSinhVaoLop);

router.delete("/xoa-hoc-sinh", studentController.xoaHocSinhKhoiLop);
// API Tra cứu học sinh (BM7)
router.get("/tra-cuu", studentController.traCuuHocSinh);

module.exports = router;
