const db = require("../config/db");

exports.login = async (req, res) => {
  const { TenDangNhap, MatKhau } = req.body;

  try {
    const [users] = await db.query(
      "SELECT MaSo, HoTen, MatKhau, PhanQuyen FROM nguoidung WHERE TenDangNhap = ? AND TrangThai = 1",
      [TenDangNhap]
    );

    if (users.length === 0) {
      return res
        .status(401)
        .json({ error: "Tài khoản không tồn tại hoặc đã bị khóa!" });
    }

    const user = users[0];
    if (user.MatKhau !== MatKhau) {
      return res.status(401).json({ error: "Mật khẩu không chính xác!" });
    }

    const [quyen] = await db.query(
      "SELECT MaCN FROM nguoidung_quyen WHERE MaSo = ?",
      [user.MaSo]
    );

    res.json({
      message: "Đăng nhập thành công!",
      user: {
        MaSo: user.MaSo,
        HoTen: user.HoTen,
        PhanQuyen: user.PhanQuyen,
        quyen: quyen.map((q) => q.MaCN),
      },
    });
  } catch (err) {
    console.error("Lỗi đăng nhập:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi đăng nhập" });
  }
};

exports.changePassword = async (req, res) => {
  const { TenDangNhap, MatKhauCu, MatKhauMoi, XacNhanMatKhau } = req.body;

  if (!TenDangNhap || !MatKhauCu || !MatKhauMoi || !XacNhanMatKhau) {
    return res.status(400).json({ error: "Vui lòng nhập đầy đủ các trường." });
  }

  if (MatKhauMoi !== XacNhanMatKhau) {
    return res
      .status(400)
      .json({ error: "Mật khẩu mới và xác nhận không khớp!" });
  }

  try {
    const [user] = await db.query(
      "SELECT MaSo, MatKhau FROM nguoidung WHERE TenDangNhap = ? AND TrangThai = 1",
      [TenDangNhap]
    );

    if (user.length === 0) {
      return res
        .status(404)
        .json({ error: "Tài khoản không tồn tại hoặc đã bị khóa." });
    }

    if (user[0].MatKhau !== MatKhauCu) {
      return res.status(401).json({ error: "Mật khẩu cũ không chính xác!" });
    }

    await db.query("UPDATE nguoidung SET MatKhau = ? WHERE MaSo = ?", [
      MatKhauMoi,
      user[0].MaSo,
    ]);

    res.json({ message: "Đổi mật khẩu thành công!" });
  } catch (err) {
    console.error("Lỗi đổi mật khẩu:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi đổi mật khẩu." });
  }
};
