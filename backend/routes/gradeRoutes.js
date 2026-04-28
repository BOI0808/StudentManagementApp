const express = require("express");
const multer = require("multer");
const router = express.Router();
const gradeController = require("../controllers/gradeController");

const upload = multer({ storage: multer.memoryStorage() });

// Endpoint: POST /api/grades/nhap-diem
router.post("/nhap-diem", gradeController.luuBangDiem);
// Endpoint: POST /api/grades/import-excel
router.post(
  "/import-excel",
  upload.single("file"),
  gradeController.importGradesExcel
);
// Endpoint: GET /api/grades/nhap-diem/danh-sach?MaLop=...&MaMonHoc=...&MaLoaiKiemTra=...&MaHocKyNamHoc=...
router.get("/nhap-diem/danh-sach", gradeController.getHocSinhNhapDiem);

module.exports = router;
