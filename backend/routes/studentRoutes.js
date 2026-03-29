const express = require("express");
const router = express.Router();
const studentController = require("../controllers/studentController");

router.post("/tiep-nhan-hoc-sinh", studentController.tiepNhanHocSinh);
// router.get("/tra-cuu", studentController.traCuuHocSinh);

module.exports = router;
