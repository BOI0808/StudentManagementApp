const express = require("express");
const router = express.Router();
const testTypeController = require("../controllers/testTypeController");

router.post("/lap-loai-kiem-tra", testTypeController.createLoaiKT);
router.get("/danh-sach-loai-kiem-tra", testTypeController.getAllActiveLoaiKT);
router.patch(
  "/xoa-loai-kiem-tra/:MaLoaiKiemTra",
  testTypeController.softDeleteLoaiKT
);
router.patch(
  "/cap-nhat-he-so/:MaLoaiKiemTra",
  testTypeController.updateHeSoLoaiKT
);

module.exports = router;
