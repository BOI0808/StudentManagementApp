const express = require("express");
const router = express.Router();
const userController = require("../controllers/userController");

//Endpoint: GET /api/users/danh-sach-tai-khoan
router.get("/danh-sach-tai-khoan", userController.getAllAccounts);
//Endpoint: POST /api/users/tao-tai-khoan
router.post("/tao-tai-khoan", userController.createUser);
//Endpoint: PUT /api/users/cap-nhat-tai-khoan/:id
router.put("/cap-nhat-tai-khoan/:id", userController.updateAccount);
//Endpoint: DELETE /api/users/xoa-tai-khoan/:id
router.delete("/xoa-tai-khoan/:id", userController.softDeleteAccount);

module.exports = router;
