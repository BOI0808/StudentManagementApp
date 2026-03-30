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
