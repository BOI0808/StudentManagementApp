const db = require("../config/db");

// BM4: Lập danh mục môn học
exports.createSubject = async (req, res) => {
  const { TenMonHoc } = req.body;

  try {
    // 1. Kiểm tra rỗng
    if (!TenMonHoc) {
      return res
        .status(400)
        .json({ error: "Tên môn học không được để trống." });
    }

    // 2. Kiểm tra trùng lặp (Bước 4 thuật toán: So sánh D1 với D3) [cite: 180-181]
    const [existing] = await db.query(
      "SELECT * FROM monhoc WHERE TenMonHoc = ?",
      [TenMonHoc]
    );
    if (existing.length > 0) {
      return res
        .status(400)
        .json({ error: "Môn học này đã tồn tại trong danh mục." });
    }

    // 3. Lưu xuống cơ sở dữ liệu (Bước 6 thuật toán: Lưu D4) [cite: 183]
    const MaMonHoc = "MH" + Date.now().toString().slice(-4); // Tạo mã tự động
    await db.query("INSERT INTO monhoc (MaMonHoc, TenMonHoc) VALUES (?, ?)", [
      MaMonHoc,
      TenMonHoc,
    ]);

    res.json({ message: "Lập danh mục môn học mới thành công!", MaMonHoc }); // [cite: 184]
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi lập danh mục môn học" });
  }
};

// BM4: Lập danh mục môn học
exports.getMonHoc = async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM monhoc");
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi lấy danh sách môn học" });
  }
};
