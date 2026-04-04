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
// Endpoint: DELETE /api/hocsinh/xoa-hoc-sinh
router.delete("/xoa-hoc-sinh", studentController.xoaHocSinhKhoiLop);
// Endpoint: GET /api/hocsinh/search-ma-hoc-sinh?key=HS260001
router.get("/search-ma-hoc-sinh", studentController.searchMaHocSinh);
// Endpoint: GET /api/hocsinh/search-ten-hoc-sinh?key=Nguyen
router.get("/search-ten-hoc-sinh", studentController.searchTenHocSinh);
// Endpoint: GET /api/hocsinh/ket-qua-tra-cuu?maLop=?&hoten=?
router.get("/ket-qua-tra-cuu", studentController.traCuuHocSinh);

module.exports = router;
