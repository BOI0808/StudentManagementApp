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
  const TenMonHoc = req.body.TenMonHoc?.toString().trim();

  try {
    if (!TenMonHoc) {
      return res
        .status(400)
        .json({ error: "Tên môn học không được để trống." });
    }

    // 1. Kiểm tra xem môn học đã từng tồn tại trong DB chưa (kể cả đang ngưng dạy)
    const [existing] = await db.query(
      "SELECT MaMonHoc, TrangThai FROM monhoc WHERE TenMonHoc = ?",
      [TenMonHoc]
    );

    if (existing.length > 0) {
      const subject = existing[0];

      // Trường hợp A: Môn học đang hoạt động bình thường
      if (subject.TrangThai === 1) {
        return res
          .status(400)
          .json({ error: "Môn học này đã tồn tại và đang hoạt động." });
      }

      // Trường hợp B: Môn học tồn tại nhưng đang ngưng dạy -> Bật lại
      else {
        await db.query("UPDATE monhoc SET TrangThai = 1 WHERE MaMonHoc = ?", [
          subject.MaMonHoc,
        ]);
        return res.json({
          message: "Lập danh mục môn học mới thành công!",
          MaMonHoc: subject.MaMonHoc,
        });
      }
    }

    // 2. Nếu môn học hoàn toàn mới -> Tiến hành thêm mới như cũ
    const MaMonHoc = generateSubjectCode(TenMonHoc);

    await db.query(
      "INSERT INTO monhoc (MaMonHoc, TenMonHoc, TrangThai) VALUES (?, ?, 1)",
      [MaMonHoc, TenMonHoc]
    );

    res.json({ message: "Lập danh mục môn học mới thành công!", MaMonHoc });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi lập danh mục môn học" });
  }
};

// Cập nhật trạng thái môn học
exports.toggleSubjectStatus = async (req, res) => {
  const { MaMonHoc } = req.params;
  const { TrangThai } = req.body; // Gửi lên 0 để ngưng dạy, 1 để mở lại

  try {
    // Cập nhật trạng thái thay vì xóa
    const [result] = await db.query(
      "UPDATE monhoc SET TrangThai = ? WHERE MaMonHoc = ?",
      [TrangThai, MaMonHoc]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ error: "Không tìm thấy môn học." });
    }

    if (TrangThai !== 0 && TrangThai !== 1) {
      return res.status(400).json({ error: "Trạng thái không hợp lệ." });
    }

    const message =
      TrangThai === 0 ? "Đã xóa môn học." : "Đã thêm môn học thành công.";
    res.json({ message });
  } catch (err) {
    res.status(500).json({ error: "Lỗi hệ thống khi xóa môn học." });
  }
};

// API lấy danh sách môn đang dạy
exports.getActiveSubjects = async (req, res) => {
  try {
    const [rows] = await db.query("SELECT * FROM monhoc WHERE TrangThai = 1");
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi khi lấy danh sách môn học." });
  }
};
