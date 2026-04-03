const express = require("express");
const router = express.Router();
const studentController = require("../controllers/studentController");

// API Tiếp nhận học sinh (BM6)
router.post("/tiep-nhan-hoc-sinh", studentController.tiepNhanHocSinh);
// API Tra cứu học sinh (BM7)
router.get("/tra-cuu", studentController.traCuuHocSinh);

module.exports = router;
