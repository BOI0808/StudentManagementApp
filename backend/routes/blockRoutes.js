const express = require("express");
const router = express.Router();
const blockController = require("../controllers/blockController");

// Endpoint: POST /api/classes/lap-khoi-lop
router.post("/lap-khoi-lop", blockController.createBlock);
// Endpoint: PUT /api/blocks/cap-nhat-khoi-lop/:MaKhoiLop
router.put("/cap-nhat-khoi-lop/:MaKhoiLop", blockController.toggleBlockStatus);

module.exports = router;
