const db = require("../config/db");
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
require("../config/loadEnv"); // Load environment variables

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
    let isMatch = false;

    const isBcrypt =
      user.MatKhau &&
      (user.MatKhau.startsWith("$2a$") ||
        user.MatKhau.startsWith("$2b$") ||
        user.MatKhau.startsWith("$2y$"));

    if (isBcrypt) {
      isMatch = await bcrypt.compare(MatKhau, user.MatKhau);
    } else {
      isMatch = user.MatKhau === MatKhau;
      if (isMatch) {
        try {
          const hashedPassword = await bcrypt.hash(MatKhau, 10);
          await db.query("UPDATE nguoidung SET MatKhau = ? WHERE MaSo = ?", [
            hashedPassword,
            user.MaSo,
          ]);
        } catch (updateErr) {
          console.error("Lỗi tự động nâng cấp mật khẩu sang hash:", updateErr);
        }
      }
    }

    if (!isMatch) {
      return res.status(401).json({ error: "Mật khẩu không chính xác!" });
    }

    const [quyen] = await db.query(
      "SELECT MaCN FROM nguoidung_quyen WHERE MaSo = ?",
      [user.MaSo]
    );

    const payload = {
      MaSo: user.MaSo,
      HoTen: user.HoTen,
      PhanQuyen: user.PhanQuyen,
      quyen: quyen.map((q) => q.MaCN),
    };

    const accessToken = jwt.sign(payload, process.env.JWT_SECRET, {
      expiresIn: "24h",
    });

    res.json({
      message: "Đăng nhập thành công!",
      accessToken: accessToken,
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

    const isBcrypt =
      user[0].MatKhau &&
      (user[0].MatKhau.startsWith("$2a$") ||
        user[0].MatKhau.startsWith("$2b$") ||
        user[0].MatKhau.startsWith("$2y$"));
    let isMatch = false;

    if (isBcrypt) {
      isMatch = await bcrypt.compare(MatKhauCu, user[0].MatKhau);
    } else {
      isMatch = user[0].MatKhau === MatKhauCu;
    }

    if (!isMatch) {
      return res.status(401).json({ error: "Mật khẩu cũ không chính xác!" });
    }

    const hashedNewPassword = await bcrypt.hash(MatKhauMoi, 10);
    await db.query("UPDATE nguoidung SET MatKhau = ? WHERE MaSo = ?", [
      hashedNewPassword,
      user[0].MaSo,
    ]);

    res.json({ message: "Đổi mật khẩu thành công!" });
  } catch (err) {
    console.error("Lỗi đổi mật khẩu:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi đổi mật khẩu." });
  }
};
