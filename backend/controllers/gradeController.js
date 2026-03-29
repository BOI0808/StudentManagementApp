const db = require("../config/db");

// API Lưu điểm (Biểu mẫu 9)
exports.nhapDiem = async (req, res) => {
  const { MaHocSinh, MaLop, MaMonHoc, MaLoaiKiemTra, Diem } = req.body;
  try {
    const query =
      "INSERT INTO bangdiem (MaBangDiem, MaLop, MaMonHoc, MaLoaiKiemTra, MaHocSinh, Diem) VALUES (?, ?, ?, ?, ?, ?)";
    await db.query(query, [
      "BD" + Date.now(),
      MaLop,
      MaMonHoc,
      MaLoaiKiemTra,
      MaHocSinh,
      Diem,
    ]);
    res.json({ message: "Lưu điểm thành công" });
  } catch (err) {
    res.status(500).json({ error: "Lỗi khi lưu điểm" });
  }
};
