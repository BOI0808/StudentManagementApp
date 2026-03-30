const express = require("express");
const router = express.Router();
const classController = require("../controllers/classController");

// Endpoint: POST /api/classes/lap-danh-sach
router.post("/lap-danh-sach", classController.lapDanhSachLop);

module.exports = router;
