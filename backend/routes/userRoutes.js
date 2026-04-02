const express = require("express");
const router = express.Router();
const userController = require("../controllers/userController");

// BM1: Tạo tài khoản mới cho Giáo viên (Admin thực hiện)
// Endpoint: POST /api/users/tao-tai-khoan
router.post("/tao-tai-khoan", userController.createUser);

module.exports = router;
