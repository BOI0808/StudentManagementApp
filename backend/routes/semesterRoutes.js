const express = require("express");
const router = express.Router();
const semesterController = require("../controllers/semesterController");

// Endpoint: POST /api/semesters/
router.post("/tao-hoc-ky-nam-hoc", semesterController.createHocKyNamHoc);

module.exports = router;
