const db = require("../config/db");

// Tiếp nhận học sinh
exports.tiepNhanHocSinh = async (req, res) => {
  const { HoTen, NgaySinh, MaGioiTinh, DiaChi, Email } = req.body;
  try {
    // 1. Lấy quy định về tuổi từ bảng ThamSo
    const [config] = await db.query(
      "SELECT ten_tham_so, gia_tri FROM thamso WHERE ten_tham_so IN ('TuoiToiThieu', 'TuoiToiDa')"
    );
    const minAge = config.find((c) => c.ten_tham_so === "TuoiToiThieu").gia_tri;
    const maxAge = config.find((c) => c.ten_tham_so === "TuoiToiDa").gia_tri;

    // 2. Tính tuổi học sinh (B6 trong thuật toán báo cáo)
    const birthYear = new Date(NgaySinh).getFullYear();
    const currentYear = new Date().getFullYear();
    const age = currentYear - birthYear;

    // 3. Kiểm tra điều kiện tuổi (QĐ4)
    if (age < minAge || age > maxAge) {
      return res.status(400).json({
        error: `Tuổi học sinh phải từ ${minAge} đến ${maxAge}. Hiện tại là ${age} tuổi.`,
      });
    }

    // 4. Tạo mã học sinh tự động (Ví dụ: HS + timestamp)
    const MaHocSinh = "HS" + Date.now().toString().slice(-8);

    // 5. Lưu vào bảng hocsinh (B9 trong thuật toán báo cáo)
    const query =
      "INSERT INTO hocsinh (MaHocSinh, HoTen, NgaySinh, MaGioiTinh, DiaChi, Email) VALUES (?, ?, ?, ?, ?, ?)";
    await db.query(query, [
      MaHocSinh,
      HoTen,
      NgaySinh,
      MaGioiTinh,
      DiaChi,
      Email,
    ]);

    res.json({ message: "Tiếp nhận học sinh thành công!", MaHocSinh });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi tiếp nhận hồ sơ" });
  }
};

// API Tra cứu học sinh (BM7)
exports.traCuuHocSinh = async (req, res) => {
  const { ten } = req.query; // Lấy tên từ chuỗi query (VD: ?ten=Khoi)

  try {
    const query = `
      SELECT 
        hs.MaHocSinh, 
        hs.HoTen, 
        l.TenLop, 
        hs.Email,
        hs.NgaySinh,
        gt.TenGioiTinh
      FROM hocsinh hs
      LEFT JOIN chitietlop ctl ON hs.MaHocSinh = ctl.MaHocSinh
      LEFT JOIN lop l ON ctl.MaLop = l.MaLop
      LEFT JOIN gioitinh gt ON hs.MaGioiTinh = gt.MaGioiTinh
      WHERE hs.HoTen LIKE ?`; // Tìm kiếm theo từ khóa (gần đúng)

    const [rows] = await db.query(query, [`%${ten}%`]);
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi tra cứu học sinh" });
  }
};
