const express = require("express");
const router = express.Router();
const testTypeController = require("../controllers/testTypeController");

// Endpoint: POST /api/test-types/lap-loai-kiem-tra
router.post("/lap-loai-kiem-tra", testTypeController.createLoaiKT);
// Endpoint: GET /api/test-types/danh-sach-loai-kiem-tra
router.get("/danh-sach-loai-kiem-tra", testTypeController.getAllActiveLoaiKT);
// Endpoint: PATCH /api/test-types/xoa-loai-kiem-tra/:MaLoaiKiemTra
router.patch(
  "/xoa-loai-kiem-tra/:MaLoaiKiemTra",
  testTypeController.softDeleteLoaiKT
);
// Endpoint: PATCH /api/test-types/cap-nhat-he-so/:MaLoaiKiemTra
router.patch(
  "/cap-nhat-he-so/:MaLoaiKiemTra",
  testTypeController.updateHeSoLoaiKT
);

module.exports = router;
