const db = require("../config/db");

// API Tạo mới lớp
exports.taoMoiLop = async (req, res) => {
  // Nhận thêm NamHocBatDau, NamHocKetThuc và LoaiHocKy (1, 2 hoặc 3)
  const { TenLop, MaKhoiLop, NamHocBatDau, NamHocKetThuc, LoaiHocKy } =
    req.body;

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    if (!TenLop || !MaKhoiLop || !NamHocBatDau || !LoaiHocKy) {
      return res
        .status(400)
        .json({ error: "Vui lòng nhập đầy đủ thông tin lớp học." });
    }

    // 1. Xác định danh sách các học kỳ cần tạo
    let semesters = [];
    if (LoaiHocKy === 1) semesters = [1];
    else if (LoaiHocKy === 2) semesters = [2];
    else if (LoaiHocKy === 3) semesters = [1, 2]; // Cả năm

    const shortYear = `${NamHocBatDau.toString().slice(
      -2
    )}${NamHocKetThuc.toString().slice(-2)}`;

    for (const hk of semesters) {
      const maHKNH = `HK${hk}-${shortYear}`;

      // 2. Kiểm tra trùng lặp cho từng học kỳ
      const [existing] = await connection.query(
        "SELECT MaLop FROM lop WHERE TenLop = ? AND MaHocKyNamHoc = ?",
        [TenLop, maHKNH]
      );

      if (existing.length > 0) {
        throw new Error(
          `Lớp ${TenLop} đã tồn tại trong Học kỳ ${hk} của niên khóa này.`
        );
      }

      // 3. Sinh mã lớp gợi nhớ (Semantic ID)
      const MaLop = (TenLop.replace(/\s+/g, "") + maHKNH).toUpperCase();

      // 4. Insert vào bảng lop
      await connection.query(
        "INSERT INTO lop (MaLop, TenLop, MaKhoiLop, MaHocKyNamHoc, SiSo) VALUES (?, ?, ?, ?, 0)",
        [MaLop, TenLop, MaKhoiLop, maHKNH]
      );
    }

    await connection.commit();
    res.json({ message: "Tạo lớp học cho cả niên khóa thành công!" });
  } catch (err) {
    await connection.rollback();
    console.error(err);
    res.status(400).json({ error: err.message || "Lỗi hệ thống khi tạo lớp." });
  } finally {
    connection.release();
  }
};
