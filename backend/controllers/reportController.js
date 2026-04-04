const db = require("../config/db");

// Báo cáo tổng kết môn (BM10)
exports.getBaoCaoTongKetMon = async (req, res) => {
  const { MaHocKyNamHoc, MaMonHoc } = req.query;

  try {
    // 1. Lấy đúng tham số DiemDatMon vừa thêm
    const [ts] = await db.query(
      "SELECT gia_tri FROM thamso WHERE ten_tham_so = 'DiemDatMon'"
    );
    const diemDatMon = ts[0].gia_tri;

    // 2. Query thống kê (giữ nguyên logic cũ nhưng dùng diemDatMon)
    const query = `
      SELECT 
        l.TenLop, 
        l.SiSo,
        COUNT(CASE WHEN kq.DiemTrungBinhMon >= ? THEN 1 END) AS SoLuongDat
      FROM lop l
      LEFT JOIN chitietlop ctl ON l.MaLop = ctl.MaLop
      LEFT JOIN ketqua_monhoc kq ON ctl.MaHocSinh = kq.MaHocSinh 
        AND kq.MaMonHoc = ? 
        AND kq.MaHocKyNamHoc = l.MaHocKyNamHoc
      WHERE l.MaHocKyNamHoc = ?
      GROUP BY l.MaLop, l.TenLop, l.SiSo
    `;

    const [rows] = await db.query(query, [diemDatMon, MaMonHoc, MaHocKyNamHoc]);

    // 3. Tính tỉ lệ và format kết quả cho UI
    const reportData = rows.map((item) => {
      const siSo = item.SiSo || 0;
      const soLuongDat = item.SoLuongDat || 0;
      const tiLe = siSo > 0 ? ((soLuongDat / siSo) * 100).toFixed(2) : 0;

      return {
        lop: item.TenLop,
        siSo: siSo,
        soLuongDat: soLuongDat,
        tiLe: `${tiLe}%`, // Trả về dạng chuỗi kèm % cho Giang dễ hiển thị
      };
    });

    res.json(reportData);
  } catch (err) {
    console.error("Lỗi báo cáo tổng kết môn:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi lập báo cáo." });
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
