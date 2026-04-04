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
  // Nhận dữ liệu từ body do Giang gửi lên
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

  const connection = await db.getConnection(); // Lấy connection để dùng Transaction

  try {
    await connection.beginTransaction(); // Bắt đầu giao dịch

    for (const record of DanhSachDiem) {
      const { maHocSinh, diem, ghiChu } = record;

      // 1. Kiểm tra điểm hợp lệ trong đoạn $0$ đến $10$
      if (diem < 0 || diem > 10) {
        throw new Error(`Học sinh ${maHocSinh} có điểm ${diem} không hợp lệ!`);
      }

      // 2. Kiểm tra xem bản ghi này đã tồn tại chưa
      const [existing] = await connection.query(
        "SELECT MaBangDiem FROM bangdiem WHERE MaLop = ? AND MaMonHoc = ? AND MaLoaiKiemTra = ? AND MaHocSinh = ?",
        [MaLop, MaMonHoc, MaLoaiKiemTra, maHocSinh]
      );

      if (existing.length > 0) {
        // TRƯỜNG HỢP A: Đã có điểm -> Cập nhật (Update)
        await connection.query(
          "UPDATE bangdiem SET Diem = ?, GhiChu = ? WHERE MaBangDiem = ?",
          [diem, ghiChu, existing[0].MaBangDiem]
        );
      } else {
        // TRƯỜNG HỢP B: Chưa có điểm -> Thêm mới (Insert)
        // Tạo mã bản ghi (giới hạn 10 ký tự theo SQL)
        const MaBangDiem = "BD" + Date.now().toString().slice(-8);

        await connection.query(
          "INSERT INTO bangdiem (MaBangDiem, MaLop, MaMonHoc, MaLoaiKiemTra, MaHocSinh, Diem, GhiChu) VALUES (?, ?, ?, ?, ?, ?, ?)",
          [MaBangDiem, MaLop, MaMonHoc, MaLoaiKiemTra, maHocSinh, diem, ghiChu]
        );
      }
    }

    await connection.commit(); // Thành công hết thì xác nhận lưu vào DB
    res.json({ message: "Đã lưu bảng điểm thành công!" });
  } catch (err) {
    await connection.rollback(); // Có bất kỳ lỗi nào thì hoàn tác toàn bộ
    console.error("Lỗi lưu bảng điểm:", err);
    res
      .status(400)
      .json({ error: err.message || "Lỗi hệ thống khi lưu điểm." });
  } finally {
    connection.release(); // Giải phóng connection
  }
};
