const express = require("express");
const router = express.Router();
const subjectController = require("../controllers/subjectController");

// Endpoint: GET /api/subjects
router.get("/", subjectController.getMonHoc);

module.exports = router;
