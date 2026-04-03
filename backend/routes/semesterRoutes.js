const express = require("express");
const router = express.Router();
const semesterController = require("../controllers/semesterController");

// Endpoint: POST /api/semesters/
router.post("/tao-hoc-ky-nam-hoc", semesterController.createHocKyNamHoc);
router.delete(
  "/xoa-hoc-ky-nam-hoc/:MaHocKyNamHoc",
  semesterController.deleteHocKyNamHoc
);
router.get("/danh-sach-hoc-ky-nam-hoc", semesterController.getAllHocKyNamHoc);

module.exports = router;
