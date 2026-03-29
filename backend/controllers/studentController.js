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
