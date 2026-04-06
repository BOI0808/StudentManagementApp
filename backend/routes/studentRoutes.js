const express = require("express");
const router = express.Router();
const studentController = require("../controllers/studentController");

// Endpoint: POST /api/students/tiep-nhan-hoc-sinh
router.post("/tiep-nhan-hoc-sinh", studentController.tiepNhanHocSinh);
// Endpoint: GET /api/students/search?key=Nguyen
router.get("/search", studentController.searchHocSinh);
// Endpoint: GET /api/students/ket-qua-tra-cuu?maLop=?&hoten=?
router.get("/ket-qua-tra-cuu", studentController.traCuuHocSinh);
// Endpoint: PUT /api/students/cap-nhat-hoc-sinh
router.put("/cap-nhat-hoc-sinh", studentController.updateHocSinh);
// Endpoint: DELETE /api/students/xoa-hoc-sinh
router.delete("/xoa-hoc-sinh", studentController.xoaHocSinhKhoiLop);

module.exports = router;
