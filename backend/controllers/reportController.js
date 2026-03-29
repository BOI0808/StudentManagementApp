const db = require("../config/db");

// Báo cáo tổng kết môn (BM10)
exports.getBaoCaoMon = async (req, res) => {
  const { MaMon, MaHocKyNamHoc } = req.query;
  try {
    const query = `
      SELECT l.TenLop, COUNT(ctl.MaHocSinh) as SiSo, 
      SUM(CASE WHEN kq.DiemTrungBinhMon >= 5 THEN 1 ELSE 0 END) as SoLuongDat
      FROM lop l
      JOIN chitietlop ctl ON l.MaLop = ctl.MaLop
      LEFT JOIN ketqua_monhoc kq ON ctl.MaHocSinh = kq.MaHocSinh 
        AND kq.MaLop = l.MaLop AND kq.MaMonHoc = ? AND kq.MaHocKyNamHoc = ?
      GROUP BY l.MaLop, l.TenLop`;

    const [rows] = await db.query(query, [MaMon, MaHocKyNamHoc]);
    const reportData = rows.map((row) => ({
      ...row,
      TiLe:
        row.SiSo > 0
          ? ((row.SoLuongDat / row.SiSo) * 100).toFixed(2) + "%"
          : "0%",
    }));
    res.json(reportData);
  } catch (err) {
    res.status(500).json({ error: "Lỗi khi lập báo cáo môn" });
  }
};

// Báo cáo tổng kết học kỳ (BM11)
exports.getBaoCaoHocKy = async (req, res) => {
  const { MaHocKyNamHoc } = req.query;
  try {
    const query = `
      SELECT l.TenLop, COUNT(ctl.MaHocSinh) as SiSo,
      (
        SELECT COUNT(*) FROM (
          SELECT MaHocSinh FROM ketqua_monhoc 
          WHERE MaHocKyNamHoc = ? AND MaLop = l.MaLop
          GROUP BY MaHocSinh HAVING MIN(DiemTrungBinhMon) >= 5
        ) as HocSinhDat
      ) as SoLuongDat
      FROM lop l
      JOIN chitietlop ctl ON l.MaLop = ctl.MaLop
      WHERE l.MaHocKyNamHoc = ?
      GROUP BY l.MaLop, l.TenLop`;

    const [rows] = await db.query(query, [MaHocKyNamHoc, MaHocKyNamHoc]);
    const reportData = rows.map((row) => ({
      ...row,
      TiLe:
        row.SiSo > 0
          ? ((row.SoLuongDat / row.SiSo) * 100).toFixed(2) + "%"
          : "0%",
    }));
    res.json(reportData);
  } catch (err) {
    res.status(500).json({ error: "Lỗi khi lập báo cáo học kỳ" });
  }
};
