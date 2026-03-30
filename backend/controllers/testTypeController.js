const db = require("../config/db");

// BM8: Nhập danh sách các loại hình kiểm tra [cite: 301]
exports.createTestType = async (req, res) => {
  const { TenLoaiKiemTra, HeSo } = req.body; // D1 [cite: 319, 326]

  try {
    // 1. Kiểm tra rỗng và tính hợp lệ của hệ số (QĐ8) [cite: 304, 329]
    if (!TenLoaiKiemTra || HeSo === undefined) {
      return res
        .status(400)
        .json({ error: "Vui lòng nhập đầy đủ tên và hệ số." });
    }
    if (parseFloat(HeSo) <= 0) {
      return res.status(400).json({ error: "Hệ số phải là số dương." });
    }

    // 2. Kiểm tra trùng lặp tên (QĐ7) [cite: 303, 329]
    const [existing] = await db.query(
      "SELECT * FROM loaihinhkiemtra WHERE TenLoaiKiemTra = ?",
      [TenLoaiKiemTra]
    );
    if (existing.length > 0) {
      return res
        .status(400)
        .json({ error: "Tên loại hình kiểm tra này đã tồn tại." });
    }

    // 3. Lưu xuống cơ sở dữ liệu (B6 thuật toán)
    const MaLoaiKiemTra = "KT" + Date.now().toString().slice(-4);
    await db.query(
      "INSERT INTO loaihinhkiemtra (MaLoaiKiemTra, TenLoaiKiemTra, HeSo) VALUES (?, ?, ?)",
      [MaLoaiKiemTra, TenLoaiKiemTra, HeSo]
    );

    res.json({ message: "Thêm loại hình kiểm tra thành công!", MaLoaiKiemTra });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi thêm loại hình kiểm tra" });
  }
};

// Hàm lấy danh sách để hiển thị cho Vinh/Giang chọn khi nhập điểm [cite: 484-485]
exports.getAllTestTypes = async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM loaihinhkiemtra");
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi lấy danh sách loại hình kiểm tra" });
  }
};
