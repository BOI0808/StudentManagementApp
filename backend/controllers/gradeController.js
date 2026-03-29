const db = require("../config/db");

// API Nhập điểm
exports.nhapDiem = async (req, res) => {
  const { MaHocSinh, MaLop, MaMonHoc, MaLoaiKiemTra, MaHocKyNamHoc, Diem } =
    req.body;

  try {
    // 1. Lưu điểm vào bảng bangdiem (BM9)
    const maBangDiem = "BD" + Date.now();
    await db.query(
      "INSERT INTO bangdiem (MaBangDiem, MaLop, MaMonHoc, MaLoaiKiemTra, MaHocSinh, Diem) VALUES (?, ?, ?, ?, ?, ?)",
      [maBangDiem, MaLop, MaMonHoc, MaLoaiKiemTra, MaHocSinh, Diem]
    );

    // 2. Lấy tất cả điểm của học sinh này trong môn này/học kỳ này để tính trung bình
    const [allGrades] = await db.query(
      `SELECT bd.Diem, lkt.HeSo 
       FROM bangdiem bd 
       JOIN loaihinhkiemtra lkt ON bd.MaLoaiKiemTra = lkt.MaLoaiKiemTra 
       WHERE bd.MaHocSinh = ? AND bd.MaMonHoc = ? AND bd.MaLop = ?`,
      [MaHocSinh, MaMonHoc, MaLop]
    );

    // 3. Công thức tính điểm trung bình:
    // DiemTB = (Tổng (Diem * HeSo)) / (Tổng HeSo)
    let tongDiemHeSo = 0;
    let tongHeSo = 0;
    allGrades.forEach((g) => {
      tongDiemHeSo += g.Diem * g.HeSo;
      tongHeSo += g.HeSo;
    });
    const diemTB = tongHeSo > 0 ? (tongDiemHeSo / tongHeSo).toFixed(2) : 0;

    // 4. Cập nhật hoặc Thêm mới vào bảng ketqua_monhoc (Để phục vụ báo cáo)
    await db.query(
      `INSERT INTO ketqua_monhoc (MaHocSinh, MaLop, MaMonHoc, MaHocKyNamHoc, DiemTrungBinhMon) 
       VALUES (?, ?, ?, ?, ?) 
       ON DUPLICATE KEY UPDATE DiemTrungBinhMon = VALUES(DiemTrungBinhMon)`,
      [MaHocSinh, MaLop, MaMonHoc, MaHocKyNamHoc, diemTB]
    );

    res.json({
      message: "Nhập điểm và cập nhật kết quả môn thành công!",
      DiemTrungBinh: diemTB,
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi nhập điểm" });
  }
};
