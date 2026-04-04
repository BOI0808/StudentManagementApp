const express = require("express");
const router = express.Router();
const gradeController = require("../controllers/gradeController");

router.get("/nhap-diem/danh-sach", gradeController.getHocSinhNhapDiem);
// Nhập điểm
router.post("/nhap-diem", gradeController.luuBangDiem);

module.exports = router;
