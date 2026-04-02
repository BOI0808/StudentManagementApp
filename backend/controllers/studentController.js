const db = require("../config/db");

const generateMaHocSinh = async (connection) => {
  // 1. Tiền tố cho Học sinh
  const prefix = "HS";

  // 2. Lấy 2 số cuối của năm hiện tại (VD: 2026 -> "26")
  const year = new Date().getFullYear().toString().slice(-2);
  const searchPattern = `${prefix}${year}%`; // Tìm dạng "HS26%"

  // 3. Tìm mã lớn nhất hiện có trong năm nay
  const [rows] = await connection.query(
    "SELECT MaHocSinh FROM hocsinh WHERE MaHocSinh LIKE ? ORDER BY MaHocSinh DESC LIMIT 1",
    [searchPattern]
  );

  let nextNumber = 1;
  if (rows.length > 0) {
    // Lấy 4 số cuối (ví dụ '0001'), chuyển thành số và cộng thêm 1
    const lastNumber = parseInt(rows[0].MaHocSinh.slice(-4));
    nextNumber = lastNumber + 1;
  }

  // 4. Kết quả: HS + 26 + 0001 = HS260001 (Đảm bảo 8 ký tự < 10)
  return `${prefix}${year}${nextNumber.toString().padStart(4, "0")}`;
};

// Tiếp nhận học sinh
exports.tiepNhanHocSinh = async (req, res) => {
  const { HoTen, NgaySinh, MaGioiTinh, DiaChi, Email } = req.body;

  const connection = await db.getConnection(); // Lấy connection để dùng chung cho hàm sinh mã
  try {
    await connection.beginTransaction();

    // 1. Kiểm tra dữ liệu rỗng
    if (!HoTen || !NgaySinh || !MaGioiTinh) {
      return res
        .status(400)
        .json({ error: "Vui lòng nhập đầy đủ thông tin bắt buộc." });
    }

    // 2. Lấy quy định tuổi từ bảng ThamSo
    const [config] = await connection.query(
      "SELECT ten_tham_so, gia_tri FROM thamso WHERE ten_tham_so IN ('TuoiToiThieu', 'TuoiToiDa')"
    );
    const minAge =
      config.find((c) => c.ten_tham_so === "TuoiToiThieu")?.gia_tri || 15;
    const maxAge =
      config.find((c) => c.ten_tham_so === "TuoiToiDa")?.gia_tri || 20;

    // 3. Tính và kiểm tra tuổi
    const age = new Date().getFullYear() - new Date(NgaySinh).getFullYear();
    if (age < minAge || age > maxAge) {
      await connection.rollback();
      return res.status(400).json({
        error: `Tuổi học sinh (${age}) không hợp lệ. Phải từ ${minAge} đến ${maxAge} tuổi.`,
      });
    }

    // KIỂM TRA TRÙNG LẶP HỒ SƠ --
    const [existingStudent] = await connection.query(
      "SELECT MaHocSinh FROM hocsinh WHERE HoTen = ? AND NgaySinh = ? AND DiaChi = ?",
      [HoTen.trim(), NgaySinh, DiaChi.trim()]
    );

    if (existingStudent.length > 0) {
      await connection.rollback();
      return res.status(400).json({
        error:
          "Học sinh này đã tồn tại trong hệ thống (Trùng Họ tên, Ngày sinh và Địa chỉ).",
      });
    }

    // 4. Sinh mã học sinh "đẹp" theo ý Khôi (Ví dụ: HS260001)
    const MaHocSinh = await generateMaHocSinh(connection);

    // 5. Lưu vào database
    const query = `INSERT INTO hocsinh (MaHocSinh, HoTen, NgaySinh, MaGioiTinh, DiaChi, Email) VALUES (?, ?, ?, ?, ?, ?)`;
    await connection.query(query, [
      MaHocSinh,
      HoTen.trim(),
      NgaySinh,
      MaGioiTinh,
      DiaChi,
      Email,
    ]);

    await connection.commit();
    res.json({
      message: "Tiếp nhận học sinh thành công!",
      MaHocSinh: MaHocSinh,
    });
  } catch (err) {
    await connection.rollback();
    console.error("Lỗi tiếp nhận:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi tiếp nhận hồ sơ." });
  } finally {
    connection.release();
  }
};

// API Tra cứu học sinh (BM7) - Cập nhật theo bảng ketqua_monhoc
exports.traCuuHocSinh = async (req, res) => {
  const { keyword } = req.query; // Nhận từ khóa tìm kiếm (Tên, Mã HS hoặc Lớp)

  try {
    const query = `
      SELECT 
        hs.MaHocSinh, 
        hs.HoTen, 
        nh.TenNamHoc as NamHoc, 
        l.TenLop,
        -- Tính trung bình Học kỳ 1 từ bảng ketqua_monhoc
        (SELECT ROUND(AVG(kq.DiemTrungBinhMon), 2)
         FROM ketqua_monhoc kq 
         JOIN hocky_namhoc h ON kq.MaHocKyNamHoc = h.MaHocKyNamHoc
         WHERE kq.MaHocSinh = hs.MaHocSinh AND h.TenHocKy = 'Học kỳ 1') as TB_HK1,
        -- Tính trung bình Học kỳ 2 từ bảng ketqua_monhoc
        (SELECT ROUND(AVG(kq.DiemTrungBinhMon), 2)
         FROM ketqua_monhoc kq 
         JOIN hocky_namhoc h ON kq.MaHocKyNamHoc = h.MaHocKyNamHoc
         WHERE kq.MaHocSinh = hs.MaHocSinh AND h.TenHocKy = 'Học kỳ 2') as TB_HK2
      FROM hocsinh hs
      LEFT JOIN chitietlop ctl ON hs.MaHocSinh = ctl.MaHocSinh
      LEFT JOIN lop l ON ctl.MaLop = l.MaLop
      LEFT JOIN hocky_namhoc nh ON l.MaHocKyNamHoc = nh.MaHocKyNamHoc
      WHERE hs.MaHocSinh = ? OR hs.HoTen LIKE ? OR l.TenLop LIKE ?`;

    const searchKeyword = `%${keyword}%`;
    const [rows] = await db.query(query, [
      keyword,
      searchKeyword,
      searchKeyword,
    ]);

    // Tính toán Điểm trung bình cả năm tại tầng ứng dụng
    const result = rows.map((row) => {
      const hk1 = parseFloat(row.TB_HK1) || null;
      const hk2 = parseFloat(row.TB_HK2) || null;
      let tbCaNam = null;

      if (hk1 !== null && hk2 !== null) {
        tbCaNam = ((hk1 + hk2) / 2).toFixed(2);
      } else if (hk1 !== null || hk2 !== null) {
        tbCaNam = (hk1 || hk2).toFixed(2); // Nếu chỉ có 1 học kỳ, lấy điểm học kỳ đó
      }

      return {
        MaHocSinh: row.MaHocSinh,
        HoTen: row.HoTen,
        NamHoc: row.NamHoc,
        Lop: row.TenLop,
        TB_HK1: hk1,
        TB_HK2: hk2,
        TB_CaNam: tbCaNam,
      };
    });

    res.json(result);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi tra cứu học sinh" });
  }
};
