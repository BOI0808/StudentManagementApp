const db = require("../config/db");

// Báo cáo tổng kết môn (BM10)
exports.getBaoCaoMon = async (req, res) => {
  const { MaMonHoc, MaHocKyNamHoc } = req.query;

  // 1. Kiểm tra tham số (Rất quan trọng để tránh lỗi NULL như BM11)
  if (!MaMonHoc || !MaHocKyNamHoc) {
    return res
      .status(400)
      .json({ error: "Vui lòng cung cấp đầy đủ MaMonHoc và MaHocKyNamHoc." });
  }

  try {
    const query = `
      SELECT 
        l.TenLop, 
        COUNT(DISTINCT ctl.MaHocSinh) as SiSo,
        -- Đếm số học sinh đạt (DiemTB >= 5) trong bảng ketqua_monhoc
        COUNT(DISTINCT CASE WHEN kq.DiemTrungBinhMon >= 5 THEN kq.MaHocSinh END) as SoLuongDat
      FROM lop l
      -- JOIN để lấy danh sách học sinh thực tế của lớp
      LEFT JOIN chitietlop ctl ON l.MaLop = ctl.MaLop
      -- LEFT JOIN với bảng điểm để lấy kết quả môn học tương ứng
      LEFT JOIN ketqua_monhoc kq ON ctl.MaHocSinh = kq.MaHocSinh 
           AND kq.MaMonHoc = ? 
           AND kq.MaHocKyNamHoc = ?
      WHERE l.MaHocKyNamHoc = ?
      GROUP BY l.MaLop, l.TenLop`;

    const [rows] = await db.query(query, [
      MaMonHoc,
      MaHocKyNamHoc,
      MaHocKyNamHoc,
    ]);

    const reportData = rows.map((row) => ({
      TenLop: row.TenLop,
      SiSo: row.SiSo,
      SoLuongDat: row.SoLuongDat,
      TiLe:
        row.SiSo > 0
          ? ((row.SoLuongDat / row.SiSo) * 100).toFixed(2) + "%"
          : "0%",
    }));

    res.json(reportData);
  } catch (err) {
    console.error("Lỗi BM10 chi tiết:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi lập báo cáo môn học" });
  }
};

// Báo cáo tổng kết học kỳ (BM11) (done)
exports.getBaoCaoHocKy = async (req, res) => {
  const { MaHocKyNamHoc } = req.query;

  // Kiểm tra nếu tham số bị trống (Tránh lỗi NULL trong SQL của Khôi)
  if (!MaHocKyNamHoc) {
    return res.status(400).json({ error: "Vui lòng chọn Mã học kỳ năm học." });
  }

  try {
    const query = `
      SELECT 
        l.TenLop, 
        COUNT(DISTINCT ctl.MaHocSinh) as SiSo,
        COUNT(DISTINCT hs_dat.MaHocSinh) as SoLuongDat
      FROM lop l
      LEFT JOIN chitietlop ctl ON l.MaLop = ctl.MaLop
      LEFT JOIN (
        -- Tìm danh sách học sinh đạt tất cả các môn (Min >= 5)
        SELECT kq.MaHocSinh
        FROM ketqua_monhoc kq
        WHERE kq.MaHocKyNamHoc = ?
        GROUP BY kq.MaHocSinh
        HAVING MIN(kq.DiemTrungBinhMon) >= 5
      ) hs_dat ON ctl.MaHocSinh = hs_dat.MaHocSinh
      WHERE l.MaHocKyNamHoc = ?
      GROUP BY l.MaLop, l.TenLop`;

    const [rows] = await db.query(query, [MaHocKyNamHoc, MaHocKyNamHoc]);

    const reportData = rows.map((row) => ({
      TenLop: row.TenLop,
      SiSo: row.SiSo,
      SoLuongDat: row.SoLuongDat,
      TiLe:
        row.SiSo > 0
          ? ((row.SoLuongDat / row.SiSo) * 100).toFixed(2) + "%"
          : "0%",
    }));

    res.json(reportData);
  } catch (err) {
    console.error("Lỗi MySQL:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi lập báo cáo học kỳ" });
  }
};
