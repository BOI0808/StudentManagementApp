const express = require("express");
const router = express.Router();
const testTypeController = require("../controllers/testTypeController");

router.post("/lap-loai-hinh-kiem-tra", testTypeController.createTestType);
router.get("/danh-sach-loai-hinh-kiem-tra", testTypeController.getAllTestTypes);

module.exports = router;
