const db = require("../config/db");

// API Lập danh mục học kỳ năm học (BM2)
exports.createHocKyNamHoc = async (req, res) => {
  const { NamHoc, HocKy, NgayBatDau, NgayKetThuc } = req.body;

  try {
    // 1. Kiểm tra dữ liệu rỗng (Bước 2 thuật toán) [cite: 112]
    if (!NamHoc || !HocKy || !NgayBatDau || !NgayKetThuc) {
      return res
        .status(400)
        .json({ error: "Vui lòng nhập đầy đủ thông tin bắt buộc." });
    }

    // 2. Kiểm tra logic ngày tháng (QĐ2 - Bước 3)
    if (new Date(NgayBatDau) >= new Date(NgayKetThuc)) {
      return res
        .status(400)
        .json({ error: "Ngày bắt đầu phải nhỏ hơn ngày kết thúc học kỳ." });
    }

    // 3. Lưu xuống cơ sở dữ liệu (Bước 4) [cite: 116]
    const query =
      "INSERT INTO hocky_namhoc (NamHoc, HocKy, NgayBatDau, NgayKetThuc) VALUES (?, ?, ?, ?)";
    await db.query(query, [NamHoc, HocKy, NgayBatDau, NgayKetThuc]);

    res.json({ message: "Lập danh mục học kỳ năm học thành công!" });
  } catch (err) {
    // Kiểm tra trùng lặp mã (Bước 3)
    if (err.code === "ER_DUP_ENTRY") {
      return res
        .status(400)
        .json({ error: "Học kỳ và năm học này đã tồn tại." });
    }
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi lập danh mục học kỳ" });
  }
};
