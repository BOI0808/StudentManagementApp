const express = require("express");
const router = express.Router();
const authController = require("../controllers/authController");

// Đăng nhập
router.post("/dang-nhap", authController.login);

module.exports = router;
