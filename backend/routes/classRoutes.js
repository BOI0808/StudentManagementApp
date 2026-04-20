const express = require("express");
const multer = require("multer");
const router = express.Router();
const classController = require("../controllers/classController");

const upload = multer({ storage: multer.memoryStorage() });

// Endpoint: POST /api/classes/lap-danh-sach-lop
router.post("/lap-danh-sach-lop", classController.taoMoiLop);
// Endpoint: POST /api/classes/luu-danh-sach-lop
router.post("/luu-danh-sach-lop", classController.luuDanhSachLop);
// Endpoint: GET /api/classes/danh-sach-lop
router.get("/danh-sach-lop", classController.getLopHoc);
// Endpoint: GET /api/classes/tim-kiem-ma-lop?key=10A1HK1-2627
router.get("/tim-kiem-ma-lop", classController.searchMaLop);
// Endpoint: GET /api/students/danh-sach-hoc-sinh-theo-lop/:MaLop
router.get(
  "/danh-sach-hoc-sinh-theo-lop/:MaLop",
  classController.getHocSinhTheoLop
);

module.exports = router;
