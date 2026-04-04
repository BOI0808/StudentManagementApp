const db = require("../config/db");

exports.login = async (req, res) => {
  const { TenDangNhap, MatKhau } = req.body;

  try {
    // 1. Kiểm tra tài khoản trong database
    const [user] = await db.query(
      "SELECT MaSo, HoTen, PhanQuyen FROM nguoidung WHERE TenDangNhap = ? AND MatKhau = ?",
      [TenDangNhap, MatKhau]
    );

    if (user.length > 0) {
      // 2. Nếu khớp, lấy danh sách các quyền (mã chức năng) của người dùng đó
      const [quyen] = await db.query(
        "SELECT MaCN FROM nguoidung_quyen WHERE MaSo = ?",
        [user[0].MaSo]
      );

      // 3. Trả về thông tin người dùng kèm mảng các mã quyền (VD: ['CN1', 'CN2'])
      res.json({
        message: "Đăng nhập thành công!",
        user: {
          ...user[0],
          quyen: quyen.map((q) => q.MaCN),
        },
      });
    } else {
      res
        .status(401)
        .json({ error: "Tên đăng nhập hoặc mật khẩu không chính xác!" });
    }
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi đăng nhập" });
  }
};
