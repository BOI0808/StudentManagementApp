const express = require("express");
const router = express.Router();
const semesterController = require("../controllers/semesterController");

// Endpoint: POST /api/semesters/
router.post("/", semesterController.createHocKyNamHoc);

module.exports = router;
