const db = require("../config/db");

// API tạo khối lớp
exports.createBlock = async (req, res) => {
  const TenKhoiLop = req.body.TenKhoiLop?.toString().trim();

  try {
    if (!TenKhoiLop)
      return res.status(400).json({ error: "Tên khối không được để trống" });

    // 1. Kiểm tra sự tồn tại (kể cả đã ngưng)
    const [existing] = await db.query(
      "SELECT MaKhoiLop, TrangThai FROM khoilop WHERE TenKhoiLop = ?",
      [TenKhoiLop]
    );

    if (existing.length > 0) {
      if (existing[0].TrangThai === 1) {
        return res
          .status(400)
          .json({ error: "Khối này đã tồn tại và đang hoạt động" });
      } else {
        // Kích hoạt lại khối cũ
        await db.query("UPDATE khoilop SET TrangThai = 1 WHERE MaKhoiLop = ?", [
          existing[0].MaKhoiLop,
        ]);
        return res.json({
          message: "Tạo khối lớp thành công",
          MaKhoiLop: existing[0].MaKhoiLop,
        });
      }
    }

    // 2. Tạo mới nếu chưa từng có
    const MaKhoiLop = "K" + TenKhoiLop;
    await db.query(
      "INSERT INTO khoilop (MaKhoiLop, TenKhoiLop, TrangThai) VALUES (?, ?, 1)",
      [MaKhoiLop, TenKhoiLop]
    );

    res.json({ message: "Tạo khối lớp thành công", MaKhoiLop });
  } catch (err) {
    res.status(500).json({ error: "Lỗi hệ thống khi tạo khối" });
  }
};

// Cập nhật trạng thái khối lớp
exports.toggleBlockStatus = async (req, res) => {
  const { MaKhoiLop } = req.params;
  const { TrangThai } = req.body; // 0 hoặc 1

  try {
    // 1. Nếu muốn ngưng hoạt động (0), phải kiểm tra xem có lớp nào đang thuộc khối này không
    if (TrangThai === 0) {
      const [linkedClasses] = await db.query(
        "SELECT MaLop FROM lop WHERE MaKhoiLop = ? LIMIT 1",
        [MaKhoiLop]
      );
      if (linkedClasses.length > 0) {
        return res.status(400).json({
          error:
            "Không thể ngưng hoạt động khối này vì đang có lớp học thuộc khối.",
        });
      }
    }

    // 2. Cập nhật trạng thái
    const [result] = await db.query(
      "UPDATE khoilop SET TrangThai = ? WHERE MaKhoiLop = ?",
      [TrangThai, MaKhoiLop]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ error: "Không tìm thấy khối lớp." });
    }

    res.json({ message: "Xóa khối lớp thành công!" });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi xóa khối lớp" });
  }
};

// API lấy danh sách khối lớp đang hoạt động
exports.getActiveBlocks = async (req, res) => {
  try {
    const [rows] = await db.query(
      "SELECT * FROM khoilop WHERE TrangThai = 1 ORDER BY TenKhoiLop ASC"
    );
    const dropdownData = rows.map((item) => ({
      makhoilop: item.MaKhoiLop,
      tenkhoilop: item.TenKhoiLop,
    }));

    res.json(dropdownData);
  } catch (err) {
    res.status(500).json({ error: "Lỗi khi lấy danh sách khối lớp." });
  }
};
