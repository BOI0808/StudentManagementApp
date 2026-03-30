const express = require("express");
const router = express.Router();
const configController = require("../controllers/configController");

router.get("/quy-dinh", configController.getQuyDinh);
router.put("/cap-nhat-quy-dinh", configController.updateQuyDinh);

module.exports = router;
