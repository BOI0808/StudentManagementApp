const express = require("express");
const router = express.Router();
const configController = require("../controllers/configController");

router.get("/danh-sach-quy-dinh", configController.getThamSo);
router.put("/cap-nhat-quy-dinh", configController.updateThamSo);

module.exports = router;
