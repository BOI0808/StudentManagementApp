const db = require("../config/db");

// API Lập danh sách lớp (BM6)
exports.lapDanhSachLop = async (req, res) => {
  const { MaLop, MaHocSinh } = req.body;

  try {
    // 1. Lấy sĩ số tối đa từ bảng thamso (QĐ6)
    const [config] = await db.query(
      "SELECT gia_tri FROM thamso WHERE ten_tham_so = 'SiSoToiDa'"
    );
    const maxSiSo = config[0].gia_tri;

    // 2. Tính sĩ số hiện tại của lớp
    const [currentSiSo] = await db.query(
      "SELECT COUNT(*) as count FROM chitietlop WHERE MaLop = ?",
      [MaLop]
    );

    // 3. Kiểm tra nếu lớp đã đầy
    if (currentSiSo[0].count >= maxSiSo) {
      return res.status(400).json({
        error: `Lớp đã đầy! Sĩ số tối đa là ${maxSiSo} học sinh.`,
      });
    }

    // 4. Lưu học sinh vào lớp (Bảng chitietlop)
    await db.query("INSERT INTO chitietlop (MaLop, MaHocSinh) VALUES (?, ?)", [
      MaLop,
      MaHocSinh,
    ]);

    res.json({ message: "Xếp lớp cho học sinh thành công!" });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi lập danh sách lớp" });
  }
};
