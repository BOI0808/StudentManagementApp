const db = require("../config/db");

const generateSubjectCode = (tenMon) => {
  return tenMon
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "") // Loại bỏ dấu tiếng Việt
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .replace(/\s+/g, "") // Loại bỏ khoảng trắng
    .toUpperCase()
    .slice(0, 10); // Đảm bảo không quá 10 ký tự
};
// BM4: Lập danh mục môn học
exports.createSubject = async (req, res) => {
  // 1. Chuẩn hóa dữ liệu đầu vào
  const TenMonHoc = req.body.TenMonHoc?.toString().trim();

  try {
    if (!TenMonHoc) {
      return res
        .status(400)
        .json({ error: "Tên môn học không được để trống." });
    }

    // 2. Kiểm tra trùng lặp (Giữ nguyên logic tốt của Khôi)
    const [existing] = await db.query(
      "SELECT * FROM monhoc WHERE TenMonHoc = ?",
      [TenMonHoc]
    );
    if (existing.length > 0) {
      return res
        .status(400)
        .json({ error: "Môn học này đã tồn tại trong danh mục." });
    }

    // 3. Sinh MaMonHoc: MH + 6 số cuối timestamp (Đảm bảo < 10 ký tự)
    const MaMonHoc = generateSubjectCode(TenMonHoc);

    await db.query("INSERT INTO monhoc (MaMonHoc, TenMonHoc) VALUES (?, ?)", [
      MaMonHoc,
      TenMonHoc,
    ]);

    res.json({ message: "Lập danh mục môn học thành công!", MaMonHoc });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi lập danh mục môn học" });
  }
};
