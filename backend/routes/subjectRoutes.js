const express = require("express");
const router = express.Router();
const subjectController = require("../controllers/subjectController");

// Endpoint: POST /api/subjects/lap-mon-hoc
router.post("/lap-mon-hoc", subjectController.createSubject);
// Endpoint: PUT /api/subjects/cap-nhat-mon-hoc/:MaMonHoc
router.put(
  "/cap-nhat-mon-hoc/:MaMonHoc",
  subjectController.toggleSubjectStatus
);
// Endpoint: GET /api/subjects/danh-sach-mon-hoc
router.get("/danh-sach-mon-hoc", subjectController.getActiveSubjects);

module.exports = router;
