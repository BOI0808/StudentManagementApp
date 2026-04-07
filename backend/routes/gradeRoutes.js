const express = require("express");
const router = express.Router();
const gradeController = require("../controllers/gradeController");

// Endpoint: POST /api/grades/nhap-diem
router.post("/nhap-diem", gradeController.luuBangDiem);
// Endpoint: GET /api/grades/nhap-diem/danh-sach?MaLop=...&MaMonHoc=...&MaLoaiKiemTra=...&MaHocKyNamHoc=...
router.get("/nhap-diem/danh-sach", gradeController.getHocSinhNhapDiem);

module.exports = router;
