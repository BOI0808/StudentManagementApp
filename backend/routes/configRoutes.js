const express = require("express");
const router = express.Router();
const configController = require("../controllers/configController");

// Endpoint: POST /api/config/cap-nhat-tham-so
router.post("/cap-nhat-tham-so", configController.updateThamSo);
// Endpoint: GET /api/config/danh-sach-tham-so
router.get("/danh-sach-tham-so", configController.getThamSo);

module.exports = router;
