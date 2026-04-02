const db = require("../config/db");

// API Lập danh mục học kỳ năm học (BM2)
exports.createHocKyNamHoc = async (req, res) => {
  // Chỉ nhận Năm học và Học kỳ từ Body
  const { NamHocBatDau, NamHocKetThuc, HocKy } = req.body;

  try {
    // 1. Kiểm tra dữ liệu rỗng
    if (!NamHocBatDau || !NamHocKetThuc || !HocKy) {
      return res
        .status(400)
        .json({ error: "Vui lòng nhập đầy đủ Năm học và chọn Học kỳ." });
    }

    // 2. Kiểm tra logic Học kỳ (1 hoặc 2)
    const hkValue = parseInt(HocKy);
    if (hkValue !== 1 && hkValue !== 2) {
      return res
        .status(400)
        .json({ error: "Học kỳ không hợp lệ (chỉ chấp nhận 1 hoặc 2)." });
    }

    // 3. Kiểm tra logic Năm học (Thay thế cho QĐ2 cũ)
    if (parseInt(NamHocBatDau) >= parseInt(NamHocKetThuc)) {
      return res
        .status(400)
        .json({ error: "Năm học bắt đầu phải nhỏ hơn năm học kết thúc." });
    }

    // 4. Sinh MaHocKyNamHoc an toàn (Dưới 10 ký tự)
    // Ví dụ: HK1-2526
    const shortYear = `${NamHocBatDau.toString().slice(
      -2
    )}${NamHocKetThuc.toString().slice(-2)}`;
    const MaHocKyNamHoc = `HK${hkValue}-${shortYear}`;

    // 5. Lưu xuống DB (Bỏ NgayBatDau, NgayKetThuc)
    const query = `
      INSERT INTO hocky_namhoc (MaHocKyNamHoc, NamHocBatDau, NamHocKetThuc, TenHocKy) 
      VALUES (?, ?, ?, ?)`;

    await db.query(query, [
      MaHocKyNamHoc,
      NamHocBatDau,
      NamHocKetThuc,
      `Học kỳ ${hkValue}`,
    ]);

    res.json({
      message: "Lập danh mục học kỳ thành công!",
      MaHocKy: MaHocKyNamHoc,
    });
  } catch (err) {
    if (err.code === "ER_DUP_ENTRY") {
      return res
        .status(400)
        .json({ error: "Học kỳ này đã tồn tại trong hệ thống." });
    }
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi tạo học kỳ." });
  }
};
