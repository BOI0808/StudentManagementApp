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
    await db.query("UPDATE thamso SET gia_tri = ? WHERE ten_tham_so = ?", [
      gia_tri,
      ten_tham_so,
    ]);
    res.json({ message: `Đã cập nhật ${ten_tham_so} thành ${gia_tri}` });
  } catch (err) {
    res.status(500).json({ error: "Lỗi cập nhật quy định" });
  }
};

// Lấy danh sách môn học
exports.getMonHoc = async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM monhoc");
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi lấy danh sách môn học" });
  }
};
