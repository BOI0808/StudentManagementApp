const db = require("../config/db");
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
require("../config/loadEnv");

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
        .json({ error: "Tên đăng nhập hoặc mật khẩu không đúng." });
    }

    const user = users[0];
    const isMatch = await bcrypt.compare(MatKhau, user.MatKhau);
    if (!isMatch) {
      return res
        .status(401)
        .json({ error: "Tên đăng nhập hoặc mật khẩu không đúng." });
    }

    const [qRows] = await db.query(
      "SELECT MaCN FROM nguoidung_quyen WHERE MaSo = ?",
      [user.MaSo]
    );

    const permissions = qRows.map((row) => row.MaCN);

    const accessToken = jwt.sign(
      {
        MaSo: user.MaSo,
        HoTen: user.HoTen,
        PhanQuyen: user.PhanQuyen,
        DanhSachQuyen: permissions,
      },
      process.env.JWT_SECRET,
      { expiresIn: "15m" }
    );

    const refreshToken = jwt.sign(
      { MaSo: user.MaSo },
      process.env.JWT_REFRESH_SECRET,
      { expiresIn: "7d" }
    );

    await db.query("DELETE FROM refresh_tokens WHERE MaSo = ?", [user.MaSo]);
    await db.query("INSERT INTO refresh_tokens (token, MaSo) VALUES (?, ?)", [
      refreshToken,
      user.MaSo,
    ]);

    res.cookie("refreshToken", refreshToken, {
      httpOnly: true,
      secure: true,
      sameSite: "Strict",
    });

    res.json({
      success: true,
      message: "Đăng nhập thành công!",
      data: {
        accessToken,
        refreshToken,
        user: {
          MaSo: user.MaSo,
          HoTen: user.HoTen,
          PhanQuyen: user.PhanQuyen,
          DanhSachQuyen: permissions,
        },
      },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống." });
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

exports.refreshToken = async (req, res) => {
  const refreshToken = req.body.refreshToken;

  if (!refreshToken) {
    return res.status(401).json({ error: "Vui lòng cung cấp refresh token." });
  }

  try {
    const [tokens] = await db.query(
      "SELECT * FROM refresh_tokens WHERE token = ?",
      [refreshToken]
    );
    if (tokens.length === 0) {
      return res.status(403).json({ error: "Refresh token không hợp lệ." });
    }

    jwt.verify(
      refreshToken,
      process.env.JWT_REFRESH_SECRET,
      async (err, user) => {
        if (err) {
          return res.status(403).json({ error: "Refresh token không hợp lệ." });
        }

        const [users] = await db.query(
          "SELECT MaSo, HoTen, PhanQuyen FROM nguoidung WHERE MaSo = ? AND TrangThai = 1",
          [user.MaSo]
        );
        if (users.length === 0) {
          return res.status(403).json({ error: "Tài khoản không hợp lệ." });
        }

        const [qRows] = await db.query(
          "SELECT MaCN FROM nguoidung_quyen WHERE MaSo = ?",
          [user.MaSo]
        );
        const permissions = qRows.map((row) => row.MaCN);

        const accessToken = jwt.sign(
          {
            MaSo: user.MaSo,
            HoTen: user.HoTen,
            PhanQuyen: user.PhanQuyen,
            DanhSachQuyen: permissions,
          },
          process.env.JWT_SECRET,
          { expiresIn: "15m" }
        );

        res.json({ accessToken });
      }
    );
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống." });
  }
};

exports.logout = async (req, res) => {
  const { refreshToken } = req.body;

  if (!refreshToken) {
    return res.status(400).json({ error: "Vui lòng cung cấp refresh token." });
  }

  try {
    const [tokens] = await db.query(
      "SELECT * FROM refresh_tokens WHERE token = ?",
      [refreshToken]
    );
    if (tokens.length === 0) {
      return res.status(403).json({ error: "Refresh token không hợp lệ." });
    }
    await db.query("DELETE FROM refresh_tokens WHERE token = ?", [
      refreshToken,
    ]);
    res.json({ message: "Đăng xuất thành công." });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống." });
  }
};
