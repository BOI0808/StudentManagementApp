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

  // 1. Kiểm tra không được để trống (Trim để tránh chỉ nhập dấu cách)
  if (
    !HoTen?.trim() ||
    !TenDangNhap?.trim() ||
    !MatKhau ||
    !Email?.trim() ||
    !SoDienThoai?.trim()
  ) {
    return res
      .status(400)
      .json({ error: "Vui lòng nhập đầy đủ thông tin bắt buộc." });
  }

  // 2. Kiểm tra định dạng Email (Regex chuẩn)
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(Email)) {
    return res.status(400).json({ error: "Định dạng Email không hợp lệ." });
  }

  // 3. Kiểm tra Số điện thoại (10 số, đầu số Việt Nam)
  const phoneRegex = /^(03|05|07|08|09|01[2|6|8|9])+([0-9]{8})\b/;
  if (!phoneRegex.test(SoDienThoai)) {
    return res
      .status(400)
      .json({ error: "Số điện thoại không đúng định dạng Việt Nam." });
  }

  // 4. Kiểm tra Tên đăng nhập (Từ 5-20 ký tự, không dấu cách, không ký tự đặc biệt)
  const userRegex = /^[a-zA-Z0-9_]{5,20}$/;
  if (!userRegex.test(TenDangNhap)) {
    return res.status(400).json({
      error:
        "Tên đăng nhập phải từ 5-20 ký tự, không chứa khoảng trắng hoặc ký tự đặc biệt.",
    });
  }

  // 5. Độ mạnh mật khẩu (Tối thiểu 6 ký tự)
  if (MatKhau.length < 6) {
    return res.status(400).json({ error: "Mật khẩu phải có ít nhất 6 ký tự." });
  }

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    // Kiểm tra trùng lặp
    const [existing] = await connection.query(
      "SELECT MaSo FROM nguoidung WHERE TenDangNhap = ? OR Email = ?",
      [TenDangNhap.trim(), Email.trim()]
    );
    if (existing.length > 0) {
      return res
        .status(400)
        .json({ error: "Tên đăng nhập hoặc Email đã được sử dụng." });
    }

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

exports.getAllAccounts = async (req, res) => {
  try {
    // 1. Thêm nd.Email vào danh sách các cột cần lấy
    const query = `
      SELECT 
        nd.MaSo, 
        nd.HoTen, 
        nd.Email, 
        nd.SoDienThoai, 
        nd.TenDangNhap, 
        GROUP_CONCAT(ndq.MaCN) AS DS_Quyen
      FROM nguoidung nd
      LEFT JOIN nguoidung_quyen ndq ON nd.MaSo = ndq.MaSo
      WHERE nd.TrangThai = 1 
      GROUP BY nd.MaSo
    `;

    const [rows] = await db.query(query);

    // 2. Trình bày dữ liệu sạch sẽ cho Giang (Android)
    const result = rows.map((user) => ({
      MaSo: user.MaSo,
      HoTen: user.HoTen,
      Email: user.Email, // Đã thêm Email vào đây
      SoDienThoai: user.SoDienThoai,
      TenDangNhap: user.TenDangNhap,
      QuyenHeThong: user.DS_Quyen ? `{${user.DS_Quyen}}` : "{}",
    }));

    res.json(result);
  } catch (err) {
    console.error("Lỗi lấy danh sách tài khoản:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi tải dữ liệu." });
  }
};

exports.updateAccount = async (req, res) => {
  const { id } = req.params;
  // 1. SỬA LỖI: Lấy thêm Email và TenDangNhap từ req.body
  const { HoTen, SoDienThoai, TenDangNhap, Email, MatKhau, DanhSachQuyen } =
    req.body;

  // 2. SỬA LOGIC: Kiểm tra các trường bắt buộc (bỏ qua MatKhau ở bước này)
  if (
    !HoTen?.trim() ||
    !TenDangNhap?.trim() ||
    !Email?.trim() ||
    !SoDienThoai?.trim()
  ) {
    return res
      .status(400)
      .json({ error: "Vui lòng nhập đầy đủ thông tin bắt buộc." });
  }

  // Validation Regex giữ nguyên như của Khôi (Rất tốt)
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(Email))
    return res.status(400).json({ error: "Email không hợp lệ." });

  const phoneRegex = /^(03|05|07|08|09|01[2|6|8|9])+([0-9]{8})\b/;
  if (!phoneRegex.test(SoDienThoai))
    return res.status(400).json({ error: "SĐT không hợp lệ." });

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    // 3. SỬA SQL: Cập nhật thêm TenDangNhap và Email
    let updateQuery =
      "UPDATE nguoidung SET HoTen = ?, SoDienThoai = ?, TenDangNhap = ?, Email = ? ";
    let queryParams = [HoTen, SoDienThoai, TenDangNhap, Email];

    // 4. SỬA LOGIC: Chỉ validate và update MatKhau nếu nó thực sự thay đổi
    if (MatKhau && MatKhau !== "********") {
      if (MatKhau.length < 6) {
        throw new Error("Mật khẩu mới phải có ít nhất 6 ký tự.");
      }
      updateQuery += ", MatKhau = ? ";
      queryParams.push(MatKhau);
    }

    updateQuery += " WHERE MaSo = ?";
    queryParams.push(id);

    await connection.query(updateQuery, queryParams);

    // Logic xử lý quyền cũ/mới của Khôi rất chuẩn, giữ nguyên
    if (DanhSachQuyen && Array.isArray(DanhSachQuyen)) {
      await connection.query("DELETE FROM nguoidung_quyen WHERE MaSo = ?", [
        id,
      ]);
      if (DanhSachQuyen.length > 0) {
        const quyenValues = DanhSachQuyen.map((maCN) => [id, maCN]);
        await connection.query(
          "INSERT INTO nguoidung_quyen (MaSo, MaCN) VALUES ?",
          [quyenValues]
        );
      }
    }

    await connection.commit();
    res.json({ message: "Cập nhật tài khoản và quyền hạn thành công!" });
  } catch (err) {
    await connection.rollback();
    res
      .status(400)
      .json({ error: err.message || "Lỗi hệ thống khi cập nhật." });
  } finally {
    connection.release();
  }
};

exports.softDeleteAccount = async (req, res) => {
  const { id } = req.params;
  try {
    const [result] = await db.query(
      "UPDATE nguoidung SET TrangThai = 0 WHERE MaSo = ?",
      [id]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ error: "Không tìm thấy tài khoản." });
    }

    res.json({ message: "Đã ngưng hoạt động tài khoản này thành công!" });
  } catch (err) {
    res.status(500).json({ error: "Lỗi hệ thống khi xóa tài khoản." });
  }
};
