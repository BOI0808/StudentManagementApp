const express = require("express");
const router = express.Router();
const gradeController = require("../controllers/gradeController");

// Nhập điểm
router.post("/nhap-diem", gradeController.nhapDiem);

module.exports = router;
