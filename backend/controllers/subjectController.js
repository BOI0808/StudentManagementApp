const db = require("../config/db");

// BM4: Lập danh mục môn học
exports.getMonHoc = async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM monhoc");
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi lấy danh sách môn học" });
  }
};
