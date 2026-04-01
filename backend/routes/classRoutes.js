const express = require("express");
const router = express.Router();
const classController = require("../controllers/classController");

// Endpoint: POST /api/classes/lap-danh-sach-lop
router.post("/lap-danh-sach-lop", classController.lapDanhSachLop);

module.exports = router;
