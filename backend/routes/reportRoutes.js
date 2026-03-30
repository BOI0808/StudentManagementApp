const express = require("express");
const router = express.Router();
const reportController = require("../controllers/reportController");

// Báo cáo tổng kết môn
router.get("/bao-cao-mon", reportController.getBaoCaoMon);
// Báo cáo tổng kết học kỳ
router.get("/bao-cao-hoc-ky", reportController.getBaoCaoHocKy);
module.exports = router;
