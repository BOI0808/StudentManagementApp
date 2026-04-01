const db = require("../config/db");

// API tạo khối lớp
exports.createBlock = async (req, res) => {
  // 1. Dùng trim() để loại bỏ khoảng trắng thừa ở hai đầu
  const TenKhoiLop = req.body.TenKhoiLop?.toString().trim();

  try {
    if (!TenKhoiLop)
      return res.status(400).json({ error: "Tên khối không được để trống" });

    // 2. Kiểm tra độ dài (Tên khối thường ngắn như 10, 11, 12)
    if (TenKhoiLop.length > 5) {
      return res
        .status(400)
        .json({ error: "Tên khối quá dài (Ví dụ: 10, 11, 12)" });
    }

    const [existing] = await db.query(
      "SELECT * FROM khoilop WHERE TenKhoiLop = ?",
      [TenKhoiLop]
    );

    if (existing.length > 0)
      return res.status(400).json({ error: "Khối lớp này đã tồn tại" });

    const MaKhoiLop = "K" + TenKhoiLop;
    await db.query(
      "INSERT INTO khoilop (MaKhoiLop, TenKhoiLop) VALUES (?, ?)",
      [MaKhoiLop, TenKhoiLop]
    );

    res.json({ message: "Tạo khối lớp thành công", MaKhoiLop });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi tạo khối" });
  }
};

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
