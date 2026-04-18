const express = require("express");
const multer = require("multer");
const router = express.Router();
const studentController = require("../controllers/studentController");

// Cấu hình Multer để nhận file Excel gửi từ Android
const upload = multer({ storage: multer.memoryStorage() });

// Endpoint: POST /api/students/tiep-nhan-hoc-sinh
router.post("/tiep-nhan-hoc-sinh", studentController.tiepNhanHocSinh);

// Endpoint: POST /api/students/import-excel
// Thêm middleware upload.single("file") ở đây
router.post(
  "/import-excel",
  upload.single("file"),
  studentController.importStudentsExcel
);

// Endpoint: GET /api/students/search?key=Nguyen
router.get("/search", studentController.searchHocSinh);

// Endpoint: GET /api/students/ket-qua-tra-cuu?maLop=...&hoTen=...&maHocSinh=...
router.get("/ket-qua-tra-cuu", studentController.traCuuHocSinh);

// Endpoint: PUT /api/students/cap-nhat-hoc-sinh
router.put("/cap-nhat-hoc-sinh", studentController.updateHocSinh);

// Endpoint: DELETE /api/students/xoa-hoc-sinh
router.delete("/xoa-hoc-sinh", studentController.xoaHocSinhKhoiLop);

module.exports = router;
