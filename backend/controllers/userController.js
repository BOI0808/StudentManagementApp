const db = require("../config/db");

const generateMaSo = async (connection) => {
  // 1. Mặc định tiền tố là Giáo Viên
  const prefix = "GV";

  // 2. Lấy 2 số cuối của năm hiện tại (2026 -> "26")
  const year = new Date().getFullYear().toString().slice(-2);
  const searchPattern = `${prefix}${year}%`; // Tìm dạng "GV26%"

  // 3. Tìm mã lớn nhất hiện có trong DB
  const [rows] = await connection.query(
    "SELECT MaSo FROM nguoidung WHERE MaSo LIKE ? ORDER BY MaSo DESC LIMIT 1",
    [searchPattern]
  );

  let nextNumber = 1;
  if (rows.length > 0) {
    // Lấy 3 số cuối (ví dụ '005'), chuyển thành số (5) và cộng thêm 1
    const lastNumber = parseInt(rows[0].MaSo.slice(-3));
    nextNumber = lastNumber + 1;
  }

  // 4. Kết quả: GV + 26 + 001 = GV26001
  return `${prefix}${year}${nextNumber.toString().padStart(3, "0")}`;
};

exports.createUser = async (req, res) => {
  const { HoTen, TenDangNhap, MatKhau, Email, SoDienThoai, DanhSachQuyen } =
    req.body;

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    // 1. Tự động sinh MaSo "đẹp"
    const MaSo = await generateMaSo(connection);

    // 2. Thêm vào bảng nguoidung
    const userQuery = `INSERT INTO nguoidung (MaSo, HoTen, TenDangNhap, MatKhau, Email, SoDienThoai, PhanQuyen) 
                       VALUES (?, ?, ?, ?, ?, ?, ?)`;
    await connection.query(userQuery, [
      MaSo,
      HoTen,
      TenDangNhap,
      MatKhau,
      Email,
      SoDienThoai,
      "Giáo Viên",
    ]);

    // 3. Cấp quyền (Sử dụng các mã viết tắt gợi nhớ của Khôi: CNTNHS, CNNBD...)
    if (DanhSachQuyen && DanhSachQuyen.length > 0) {
      const quyenQuery = "INSERT INTO nguoidung_quyen (MaSo, MaCN) VALUES ?";
      const values = DanhSachQuyen.map((maCN) => [MaSo, maCN]);
      await connection.query(quyenQuery, [values]);
    }

    await connection.commit();
    res.json({
      message: "Tạo tài khoản thành công!",
      MaSo: MaSo, // Trả về mã vừa sinh để Admin biết
    });
  } catch (err) {
    await connection.rollback();
    console.error("Lỗi tạo user:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi tạo tài khoản" });
  } finally {
    connection.release(); // Giải phóng kết nối (Quan trọng)
  }
};
