const express = require("express");
const router = express.Router();
const configController = require("../controllers/configController");

router.get("/quy-dinh", configController.getQuyDinh);
router.put("/cap-nhat-quy-dinh", configController.updateQuyDinh);
router.get("/mon-hoc", configController.getMonHoc);
// Thêm các route get cho lop, gioi-tinh, hoc-ky... tại đây

module.exports = router;
