const express = require("express");
const router = express.Router();
const configController = require("../controllers/configController");

router.get("/danh-sach-tham-so", configController.getThamSo);
router.post("/cap-nhat-tham-so", configController.updateThamSo);

module.exports = router;
