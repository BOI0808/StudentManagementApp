const db = require("../config/db");

// BM9: Nhập bảng điểm môn học [cite: 336-338]
exports.nhapDiem = async (req, res) => {
  const { MaHocSinh, MaLop, MaMonHoc, MaLoaiKiemTra, MaHocKyNamHoc, Diem } =
    req.body;

  try {
    // 1. Kiểm tra quy định điểm (0 <= Diem <= 10) [cite: 338]
    if (Diem < 0 || Diem > 10) {
      return res.status(400).json({ error: "Điểm phải từ 0 đến 10." });
    }

    // 2. Kiểm tra xem học sinh đã có đầu điểm này chưa
    const [existing] = await db.query(
      "SELECT MaBangDiem FROM bangdiem WHERE MaHocSinh = ? AND MaLop = ? AND MaMonHoc = ? AND MaLoaiKiemTra = ?",
      [MaHocSinh, MaLop, MaMonHoc, MaLoaiKiemTra]
    );

    if (existing.length > 0) {
      // Nếu đã có: CẬP NHẬT điểm cũ
      await db.query("UPDATE bangdiem SET Diem = ? WHERE MaBangDiem = ?", [
        Diem,
        existing[0].MaBangDiem,
      ]);
    } else {
      // Nếu chưa có: THÊM MỚI (Tự sinh MaBangDiem trong code)
      const MaBangDiem = "BD" + Date.now().toString().slice(-8);
      await db.query(
        "INSERT INTO bangdiem (MaBangDiem, MaLop, MaMonHoc, MaLoaiKiemTra, MaHocSinh, Diem) VALUES (?, ?, ?, ?, ?, ?)",
        [MaBangDiem, MaLop, MaMonHoc, MaLoaiKiemTra, MaHocSinh, Diem]
      );
    }

    // 3. Tính toán lại Điểm trung bình môn (DiemTrungBinhMon) [cite: 364]
    // Truy vấn tất cả điểm của HS này trong môn/lớp/học kỳ đó kèm theo Hệ số
    const [allGrades] = await db.query(
      `SELECT bd.Diem, lkt.HeSo 
       FROM bangdiem bd 
       JOIN loaihinhkiemtra lkt ON bd.MaLoaiKiemTra = lkt.MaLoaiKiemTra 
       WHERE bd.MaHocSinh = ? AND bd.MaMonHoc = ? AND bd.MaLop = ?`,
      [MaHocSinh, MaMonHoc, MaLop]
    );

    let tongDiemHeSo = 0;
    let tongHeSo = 0;
    allGrades.forEach((g) => {
      tongDiemHeSo += g.Diem * g.HeSo;
      tongHeSo += g.HeSo;
    });

    const diemTB = tongHeSo > 0 ? (tongDiemHeSo / tongHeSo).toFixed(2) : 0;

    // 4. Lưu kết quả vào bảng ketqua_monhoc để làm báo cáo sau này
    await db.query(
      `INSERT INTO ketqua_monhoc (MaHocSinh, MaMonHoc, MaHocKyNamHoc, DiemTrungBinhMon) 
       VALUES (?, ?, ?, ?) 
       ON DUPLICATE KEY UPDATE DiemTrungBinhMon = VALUES(DiemTrungBinhMon)`,
      [MaHocSinh, MaMonHoc, MaHocKyNamHoc, diemTB]
    );

    res.json({ message: "Lưu điểm thành công!", DiemTrungBinhMon: diemTB });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi lưu bảng điểm" });
  }
};
