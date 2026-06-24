const express = require("express");
const router = express.Router();
const authController = require("../controllers/authController");
const { loginLimiter } = require("../middlewares/rateLimitMiddleware");
const authenticateToken = require("../middlewares/authMiddleware");

//Endpoint: POST /api/auths/dang-nhap
router.post("/dang-nhap", loginLimiter, authController.login);
//Endpoint: POST /api/auths/refresh-token
router.post("/refresh-token", authController.refreshToken);
//Endpoint: POST /api/auths/doi-mat-khau
router.post("/doi-mat-khau", authController.changePassword);
router.use(authenticateToken);
//Endpoint: POST /api/auths/dang-xuat
router.post("/dang-xuat", authController.logout);

module.exports = router;
