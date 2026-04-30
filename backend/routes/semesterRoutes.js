const express = require("express");
const router = express.Router();
const semesterController = require("../controllers/semesterController");

// Endpoint: POST /api/semesters/tao-hoc-ky-nam-hoc
router.post("/tao-hoc-ky-nam-hoc", semesterController.createHocKyNamHoc);
// Endpoint: GET /api/semesters/danh-sach-hoc-ky-nam-hoc
router.get("/danh-sach-hoc-ky-nam-hoc", semesterController.getAllHocKyNamHoc);
// Endpoint: DELETE /api/semesters/xoa-hoc-ky-nam-hoc/:MaHocKyNamHoc
router.delete(
  "/xoa-hoc-ky-nam-hoc/:MaHocKyNamHoc",
  semesterController.deleteHocKyNamHoc
);

module.exports = router;
