const express = require("express");
const multer = require("multer");
const router = express.Router();
const userController = require("../controllers/userController");
const fileValidation = require("../middlewares/fileValidationMiddleware");

const upload = multer({ storage: multer.memoryStorage() });

//Endpoint: POST /api/users/tao-tai-khoan
router.post("/tao-tai-khoan", userController.createUser);
//Endpoint: POST /api/users/import-excel
router.post(
  "/import-excel",
  upload.single("file"),
  fileValidation,
  userController.importUsersExcel
);
//Endpoint: GET /api/users/danh-sach-tai-khoan
router.get("/danh-sach-tai-khoan", userController.getAllAccounts);
//Endpoint: PUT /api/users/cap-nhat-tai-khoan/:id
router.put("/cap-nhat-tai-khoan/:id", userController.updateAccount);
//Endpoint: DELETE /api/users/xoa-tai-khoan/:id
router.delete("/xoa-tai-khoan/:id", userController.softDeleteAccount);

module.exports = router;
