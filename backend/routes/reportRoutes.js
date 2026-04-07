const express = require("express");
const router = express.Router();
const reportController = require("../controllers/reportController");

// Endpoint: GET /api/reports/bao-cao-mon?MaHocKyNamHoc=...&MaMonHoc=...
router.get("/bao-cao-mon", reportController.getBaoCaoTongKetMon);
// Endpoint: GET /api/reports/bao-cao-hoc-ky?MaHocKyNamHoc=...
router.get("/bao-cao-hoc-ky", reportController.getBaoCaoTongKetHocKy);
module.exports = router;
