const db = require("../config/db");

//Lấy danh sách học sinh nhập điểm
exports.getHocSinhNhapDiem = async (req, res) => {
  const { MaLop, MaMonHoc, MaLoaiKiemTra, MaHocKyNamHoc } = req.query;

  if (!MaLop || !MaMonHoc || !MaLoaiKiemTra || !MaHocKyNamHoc) {
    return res.status(400).json({ error: "Thiếu thông tin bộ lọc để lấy danh sách." });
  }

  try {
    const query = `
      SELECT 
        hs.MaHocSinh, 
        hs.HoTen, 
        bd.Diem, 
        bd.GhiChu
      FROM hocsinh hs
      JOIN chitietlop ctl ON hs.MaHocSinh = ctl.MaHocSinh
      LEFT JOIN bangdiem bd ON hs.MaHocSinh = bd.MaHocSinh 
        AND bd.MaLop = ? 
        AND bd.MaMonHoc = ? 
        AND bd.MaLoaiKiemTra = ?
      WHERE ctl.MaLop = ?
    `;

    const [rows] = await db.query(query, [MaLop, MaMonHoc, MaLoaiKiemTra, MaLop]);

    const result = rows.map((item) => ({
      maHocSinh: item.MaHocSinh,
      hoTen: item.HoTen,
      diem: item.Diem !== null ? item.Diem : "",
      ghiChu: item.GhiChu || "",
    }));

    res.json(result);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi lấy danh sách nhập điểm." });
  }
};

exports.luuBangDiem = async (req, res) => {
  const { MaLop, MaMonHoc, MaLoaiKiemTra, DanhSachDiem } = req.body;

  if (!MaLop || !MaMonHoc || !MaLoaiKiemTra || !Array.isArray(DanhSachDiem)) {
    return res.status(400).json({ error: "Dữ liệu gửi lên không đầy đủ hoặc sai định dạng." });
  }

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    const [[lop]] = await connection.query("SELECT MaHocKyNamHoc FROM lop WHERE MaLop = ?", [MaLop]);
    if (!lop) throw new Error("Lớp học không tồn tại.");
    const maHKNH = lop.MaHocKyNamHoc;

    const [range] = await connection.query("SELECT ten_tham_so, gia_tri FROM thamso WHERE ten_tham_so IN ('DiemToiThieu', 'DiemToiDa')");
    const minDiem = range.find(r => r.ten_tham_so === 'DiemToiThieu')?.gia_tri ?? 0;
    const maxDiem = range.find(r => r.ten_tham_so === 'DiemToiDa')?.gia_tri ?? 10;

    for (let i = 0; i < DanhSachDiem.length; i++) {
      const record = DanhSachDiem[i];
      const maHS = record.maHocSinh;
      const diemStr = record.diem;
      const ghiChu = record.ghiChu || "";

      if (!maHS) continue;

      if (diemStr === "" || diemStr === null || diemStr === undefined) {
        await connection.query(
          "DELETE FROM bangdiem WHERE MaLop = ? AND MaMonHoc = ? AND MaLoaiKiemTra = ? AND MaHocSinh = ?",
          [MaLop, MaMonHoc, MaLoaiKiemTra, maHS]
        );
      } else {
        const numDiem = parseFloat(diemStr);
        if (isNaN(numDiem)) {
          throw new Error(`Điểm của học sinh ${maHS} phải là một con số.`);
        }
        if (numDiem < minDiem || numDiem > maxDiem) {
          throw new Error(`Học sinh ${maHS} có điểm ${numDiem} nằm ngoài phạm vi (${minDiem}-${maxDiem}).`);
        }

        const [existing] = await connection.query(
          "SELECT MaBangDiem FROM bangdiem WHERE MaLop = ? AND MaMonHoc = ? AND MaLoaiKiemTra = ? AND MaHocSinh = ?",
          [MaLop, MaMonHoc, MaLoaiKiemTra, maHS]
        );

        if (existing.length > 0) {
          await connection.query(
            "UPDATE bangdiem SET Diem = ?, GhiChu = ? WHERE MaBangDiem = ?",
            [numDiem, ghiChu, existing[0].MaBangDiem]
          );
        } else {
          const MaBangDiem = `BD${Date.now().toString().slice(-5)}${i.toString().padStart(3, "0")}`;
          await connection.query(
            "INSERT INTO bangdiem (MaBangDiem, MaLop, MaMonHoc, MaLoaiKiemTra, MaHocSinh, Diem, GhiChu) VALUES (?, ?, ?, ?, ?, ?, ?)",
            [MaBangDiem, MaLop, MaMonHoc, MaLoaiKiemTra, maHS, numDiem, ghiChu]
          );
        }
      }

      const [stats] = await connection.query(
        `SELECT SUM(bd.Diem * lkt.HeSo) / SUM(lkt.HeSo) AS DiemTB 
         FROM bangdiem bd 
         JOIN loaihinhkiemtra lkt ON bd.MaLoaiKiemTra = lkt.MaLoaiKiemTra 
         WHERE bd.MaHocSinh = ? AND bd.MaLop = ? AND bd.MaMonHoc = ?`,
        [maHS, MaLop, MaMonHoc]
      );

      const diemTB = stats[0].DiemTB !== null ? parseFloat(stats[0].DiemTB).toFixed(2) : 0;

      await connection.query(
        `INSERT INTO ketqua_monhoc (MaHocSinh, MaMonHoc, MaHocKyNamHoc, DiemTrungBinhMon) 
         VALUES (?, ?, ?, ?) 
         ON DUPLICATE KEY UPDATE DiemTrungBinhMon = VALUES(DiemTrungBinhMon)`,
        [maHS, MaMonHoc, maHKNH, diemTB]
      );
    }

    await connection.commit();
    res.json({ message: "Lưu bảng điểm thành công!" });
  } catch (err) {
    await connection.rollback();
    console.error("Lỗi lưu điểm:", err.message);
    res.status(400).json({ error: err.message });
  } finally {
    connection.release();
  }
};
