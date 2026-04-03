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
        if (semesters.length === 2) {
          throw new Error(
            `Lớp ${TenLop} đã tồn tại trong cả 2 học kỳ của niên khóa này.`
          );
        } else {
          throw new Error(
            `Lớp ${TenLop} đã tồn tại trong Học kỳ ${hk} của niên khóa này.`
          );
        }
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
    let successMessage = "";
    if (LoaiHocKy === 3) {
      successMessage = `Tạo lớp ${TenLop} cho cả niên khóa ${NamHocBatDau}-${NamHocKetThuc} thành công!`;
    } else {
      successMessage = `Tạo lớp ${TenLop} cho Học kỳ ${LoaiHocKy} (${NamHocBatDau}-${NamHocKetThuc}) thành công!`;
    }

    res.json({ message: successMessage });
  } catch (err) {
    await connection.rollback();
    console.error(err);
    res.status(400).json({ error: err.message || "Học kỳ chưa tồn tại!" });
  } finally {
    connection.release();
  }
};

// API lấy danh sách lớp học để đổ vào thanh tìm kiếm/dropdown
exports.getLopHoc = async (req, res) => {
  try {
    // JOIN với bảng hocky_namhoc để lấy tên học kỳ và năm học, đồng thời sắp xếp theo năm học mới nhất và tên lớp
    const query = `
      SELECT l.MaLop, l.TenLop, l.MaHocKyNamHoc, l.SiSo, hn.TenHocKy, hn.NamHocBatDau, hn.NamHocKetThuc
      FROM lop l
      JOIN hocky_namhoc hn ON l.MaHocKyNamHoc = hn.MaHocKyNamHoc
      ORDER BY hn.NamHocBatDau DESC, l.TenLop ASC
    `;

    const [rows] = await db.query(query);

    // Format lại dữ liệu để Giang dễ hiển thị trên Android
    const dropdownData = rows.map((item) => ({
      maLop: item.MaLop,
      tenLop: item.TenLop,
      siSoHienTai: item.SiSo,
      hienThi: `${item.TenLop} - ${item.TenHocKy} (${item.NamHocBatDau}-${item.NamHocKetThuc})`,
    }));

    res.json(dropdownData);
  } catch (err) {
    console.error("Lỗi lấy danh sách lớp:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi lấy danh sách lớp học." });
  }
};
