const express = require("express");
const router = express.Router();
const classController = require("../controllers/classController");

// Endpoint: POST /api/classes/lap-danh-sach-lop
router.post("/lap-danh-sach-lop", classController.taoMoiLop);
// Endpoint: GET /api/classes/danh-sach-lop
router.get("/danh-sach-lop", classController.getLopHoc);
// Endpoint: GET /api/classes/tim-kiem-lop
router.get("/search", classController.searchLopHoc);

module.exports = router;
