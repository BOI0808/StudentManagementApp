const db = require("../config/db");

//Lấy danh sách học sinh nhập điểm
exports.getHocSinhNhapDiem = async (req, res) => {
  // Giang gửi về 4 mã này từ các Dropdown
  const { MaLop, MaMonHoc, MaLoaiKiemTra, MaHocKyNamHoc } = req.query;

  if (!MaLop || !MaMonHoc || !MaLoaiKiemTra || !MaHocKyNamHoc) {
    return res
      .status(400)
      .json({ error: "Thiếu thông tin bộ lọc để lấy danh sách." });
  }

  try {
    // Dùng LEFT JOIN để lấy tên học sinh TRƯỚC, sau đó "ướm" điểm vào (nếu có)
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

    const [rows] = await db.query(query, [
      MaLop,
      MaMonHoc,
      MaLoaiKiemTra,
      MaLop,
    ]);

    // Format lại dữ liệu cho Giang dễ đổ vào Table
    const result = rows.map((item) => ({
      maHocSinh: item.MaHocSinh,
      hoTen: item.HoTen,
      diem: item.Diem !== null ? item.Diem : "", // Để chuỗi rỗng nếu chưa có điểm
      ghiChu: item.GhiChu || "",
    }));

    res.json(result);
  } catch (err) {
    console.error(err);
    res
      .status(500)
      .json({ error: "Lỗi hệ thống khi lấy danh sách nhập điểm." });
  }
};

exports.luuBangDiem = async (req, res) => {
  const { MaLop, MaMonHoc, MaLoaiKiemTra, DanhSachDiem } = req.body;

  if (
    !MaLop ||
    !MaMonHoc ||
    !MaLoaiKiemTra ||
    !DanhSachDiem ||
    !Array.isArray(DanhSachDiem)
  ) {
    return res.status(400).json({ error: "Dữ liệu gửi lên không hợp lệ." });
  }

  const connection = await db.getConnection();

  try {
    await connection.beginTransaction();

    // 0. Lấy MaHocKyNamHoc từ bảng lớp để đồng bộ sang ketqua_monhoc
    const [classInfo] = await connection.query(
      "SELECT MaHocKyNamHoc FROM lop WHERE MaLop = ?",
      [MaLop]
    );
    const maHKNH = classInfo[0].MaHocKyNamHoc;

    for (const record of DanhSachDiem) {
      const { maHocSinh, diem, ghiChu } = record;

      if (diem < 0 || diem > 10) {
        throw new Error(`Học sinh ${maHocSinh} có điểm ${diem} không hợp lệ!`);
      }

      // 1. Lưu/Cập nhật điểm vào bảng bangdiem
      const [existing] = await connection.query(
        "SELECT MaBangDiem FROM bangdiem WHERE MaLop = ? AND MaMonHoc = ? AND MaLoaiKiemTra = ? AND MaHocSinh = ?",
        [MaLop, MaMonHoc, MaLoaiKiemTra, maHocSinh]
      );

      if (existing.length > 0) {
        await connection.query(
          "UPDATE bangdiem SET Diem = ?, GhiChu = ? WHERE MaBangDiem = ?",
          [diem, ghiChu, existing[0].MaBangDiem]
        );
      } else {
        const MaBangDiem = "BD" + Date.now().toString().slice(-8);
        await connection.query(
          "INSERT INTO bangdiem (MaBangDiem, MaLop, MaMonHoc, MaLoaiKiemTra, MaHocSinh, Diem, GhiChu) VALUES (?, ?, ?, ?, ?, ?, ?)",
          [MaBangDiem, MaLop, MaMonHoc, MaLoaiKiemTra, maHocSinh, diem, ghiChu]
        );
      }

      // 2. TÍNH TOÁN ĐIỂM TRUNG BÌNH MÔN NGAY LẬP TỨC
      const [stats] = await connection.query(
        `SELECT SUM(bd.Diem * lkt.HeSo) / SUM(lkt.HeSo) AS DiemTB 
         FROM bangdiem bd 
         JOIN loaihinhkiemtra lkt ON bd.MaLoaiKiemTra = lkt.MaLoaiKiemTra 
         WHERE bd.MaHocSinh = ? AND bd.MaLop = ? AND bd.MaMonHoc = ?`,
        [maHocSinh, MaLop, MaMonHoc]
      );

      const diemTB = stats[0].DiemTB
        ? parseFloat(stats[0].DiemTB).toFixed(1)
        : 0;

      // 3. Cập nhật vào bảng ketqua_monhoc
      await connection.query(
        `INSERT INTO ketqua_monhoc (MaHocSinh, MaMonHoc, MaHocKyNamHoc, DiemTrungBinhMon) 
         VALUES (?, ?, ?, ?) 
         ON DUPLICATE KEY UPDATE DiemTrungBinhMon = VALUES(DiemTrungBinhMon)`,
        [maHocSinh, MaMonHoc, maHKNH, diemTB]
      );
    }

    await connection.commit();
    res.json({
      message: "Đã lưu bảng điểm và cập nhật điểm trung bình môn thành công!",
    });
  } catch (err) {
    await connection.rollback();
    console.error("Lỗi lưu điểm:", err);
    res
      .status(400)
      .json({ error: err.message || "Lỗi hệ thống khi lưu điểm." });
  } finally {
    connection.release();
  }
};
