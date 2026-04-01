const express = require("express");
const router = express.Router();
const reportController = require("../controllers/reportController");

// Báo cáo tổng kết môn
router.post("/bao-cao-mon", reportController.getBaoCaoMon);

module.exports = router;
