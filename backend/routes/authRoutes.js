const express = require("express");
const router = express.Router();
const authController = require("../controllers/authController");

////Endpoint: POST /api/auths/dang-nhap
router.post("/dang-nhap", authController.login);
////Endpoint: POST /api/auths/doi-mat-khau
router.post("/doi-mat-khau", authController.changePassword);

module.exports = router;
