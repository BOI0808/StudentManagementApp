const express = require("express");
const router = express.Router();
const subjectController = require("../controllers/subjectController");

// Endpoint: POST /api/subjects/lap-mon-hoc
router.post("/lap-mon-hoc", subjectController.createSubject);

module.exports = router;
