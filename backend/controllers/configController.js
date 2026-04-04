const db = require("../config/db");

// Lấy quy định (Tuổi, sĩ số...)
exports.getQuyDinh = async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM thamso");
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi lấy quy định" });
  }
};

// Cập nhật quy định (PUT)
exports.updateQuyDinh = async (req, res) => {
  const { ten_tham_so, gia_tri } = req.body;

  try {
    // 1. Kiểm tra tham số đầu vào
    if (!ten_tham_so || gia_tri === undefined) {
      return res
        .status(400)
        .json({ error: "Vui lòng cung cấp tên tham số và giá trị mới." });
    }

    // 2. Cập nhật giá trị vào bảng thamso
    const [result] = await db.query(
      "UPDATE thamso SET gia_tri = ? WHERE ten_tham_so = ?",
      [gia_tri, ten_tham_so]
    );

    if (result.affectedRows === 0) {
      return res
        .status(404)
        .json({ error: "Không tìm thấy tham số này trong hệ thống." });
    }

    res.json({ message: `Cập nhật quy định '${ten_tham_so}' thành công!` });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi cập nhật quy định" });
  }
};
