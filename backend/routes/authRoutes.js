const express = require("express");
const router = express.Router();
const authController = require("../controllers/authController");

//Endpoint: POST /api/auths/dang-nhap
router.post("/dang-nhap", authController.login);
//Endpoint: POST /api/auths/doi-mat-khau
router.post("/doi-mat-khau", authController.changePassword);
//Endpoint: POST /api/auths/refresh-token
router.post("/refresh-token", authController.refreshToken);
//Endpoint: POST /api/auths/dang-xuat
router.post("/dang-xuat", authController.logout);

module.exports = router;
