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
      ORDER BY l.TenLop ASC
    `;

    const [rows] = await db.query(query, [diemDatMon, MaMonHoc, MaHocKyNamHoc]);

    // 3. Tính tỉ lệ và format kết quả cho UI
    const reportData = rows.map((item) => {
      const siSo = item.SiSo || 0;
      const soLuongDat = item.SoLuongDat || 0;
      const tiLe = siSo > 0 ? ((soLuongDat / siSo) * 100).toFixed(1) : 0;

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

// Báo cáo tổng kết học kỳ
exports.getBaoCaoTongKetHocKy = async (req, res) => {
  const { MaHocKyNamHoc } = req.query; // Giang gửi mã học kỳ từ Dropdown

  if (!MaHocKyNamHoc) {
    return res
      .status(400)
      .json({ error: "Vui lòng chọn học kỳ để xem báo cáo." });
  }

  try {
    // 1. Lấy điểm đạt hệ thống
    const [ts] = await db.query(
      "SELECT gia_tri FROM thamso WHERE ten_tham_so = 'DiemDat'"
    );
    const diemDat = ts[0].gia_tri;

    // 2. Query tổng hợp: Tính GPA học kỳ và đếm số lượng đạt theo lớp
    const query = `
      SELECT 
        l.TenLop, 
        l.SiSo,
        COUNT(CASE WHEN student_gpa.GPA_HocKy >= ? THEN 1 END) AS SoLuongDat
      FROM lop l
      LEFT JOIN chitietlop ctl ON l.MaLop = ctl.MaLop
      LEFT JOIN (
        -- Tính GPA trung bình các môn của mỗi học sinh trong học kỳ đó
        SELECT MaHocSinh, MaHocKyNamHoc, AVG(DiemTrungBinhMon) AS GPA_HocKy
        FROM ketqua_monhoc
        GROUP BY MaHocSinh, MaHocKyNamHoc
      ) AS student_gpa ON ctl.MaHocSinh = student_gpa.MaHocSinh 
        AND student_gpa.MaHocKyNamHoc = l.MaHocKyNamHoc
      WHERE l.MaHocKyNamHoc = ?
      GROUP BY l.MaLop, l.TenLop, l.SiSo
    `;

    const [rows] = await db.query(query, [diemDat, MaHocKyNamHoc]);

    // 3. Format dữ liệu trả về cho Giang đổ vào bảng
    const finalReport = rows.map((item) => {
      const siSo = item.SiSo || 0;
      const soLuongDat = item.SoLuongDat || 0;
      const tiLe = siSo > 0 ? ((soLuongDat / siSo) * 100).toFixed(2) : 0;

      return {
        lop: item.TenLop,
        siSo: siSo,
        soLuongDat: soLuongDat,
        tiLe: `${tiLe}%`,
      };
    });

    res.json(finalReport);
  } catch (err) {
    console.error("Lỗi báo cáo học kỳ:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi lập báo cáo học kỳ." });
  }
};
