const db = require("../config/db");

const generateMaHocSinh = async (connection) => {
  // 1. Tiền tố cho Học sinh
  const prefix = "HS";

  // 2. Lấy 2 số cuối của năm hiện tại (VD: 2026 -> "26")
  const year = new Date().getFullYear().toString().slice(-2);
  const searchPattern = `${prefix}${year}%`; // Tìm dạng "HS26%"

  // 3. Tìm mã lớn nhất hiện có trong năm nay
  const [rows] = await connection.query(
    "SELECT MaHocSinh FROM hocsinh WHERE MaHocSinh LIKE ? ORDER BY MaHocSinh DESC LIMIT 1",
    [searchPattern]
  );

  let nextNumber = 1;
  if (rows.length > 0) {
    // Lấy 4 số cuối (ví dụ '0001'), chuyển thành số và cộng thêm 1
    const lastNumber = parseInt(rows[0].MaHocSinh.slice(-4));
    nextNumber = lastNumber + 1;
  }

  // 4. Kết quả: HS + 26 + 0001 = HS260001 (Đảm bảo 8 ký tự < 10)
  return `${prefix}${year}${nextNumber.toString().padStart(4, "0")}`;
};

// Tiếp nhận học sinh
exports.tiepNhanHocSinh = async (req, res) => {
  const { HoTen, NgaySinh, MaGioiTinh, DiaChi, Email } = req.body;

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    // 1. Kiểm tra dữ liệu rỗng (Thêm DiaChi vào đây nhé Khôi)
    if (!HoTen || !NgaySinh || !MaGioiTinh || !DiaChi) {
      return res.status(400).json({
        error: "Vui lòng nhập đầy đủ: Họ tên, Ngày sinh, Giới tính và Địa chỉ.",
      });
    }

    // 2. Chuẩn hóa ngày tháng trước khi tính toán và query
    const dateObj = new Date(NgaySinh);
    if (isNaN(dateObj))
      return res.status(400).json({ error: "Ngày sinh không đúng định dạng." });
    const formattedDate = dateObj.toISOString().split("T")[0];

    // 3. Lấy quy định tuổi từ bảng ThamSo
    const [config] = await connection.query(
      "SELECT ten_tham_so, gia_tri FROM thamso WHERE ten_tham_so IN ('TuoiToiThieu', 'TuoiToiDa')"
    );
    const minAge =
      config.find((c) => c.ten_tham_so === "TuoiToiThieu")?.gia_tri || 15;
    const maxAge =
      config.find((c) => c.ten_tham_so === "TuoiToiDa")?.gia_tri || 20;

    // 4. Kiểm tra điều kiện tuổi
    const age = new Date().getFullYear() - dateObj.getFullYear();
    if (age < minAge || age > maxAge) {
      await connection.rollback();
      return res.status(400).json({
        error: `Tuổi học sinh (${age}) không hợp lệ (QĐ: ${minAge}-${maxAge}).`,
      });
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(Email)) {
      return res.status(400).json({ error: "Định dạng Email không hợp lệ." });
    }

    // 5. KIỂM TRA TRÙNG LẶP HỒ SƠ (Dùng formattedDate để chính xác 100%)
    const [existingStudent] = await connection.query(
      "SELECT MaHocSinh FROM hocsinh WHERE HoTen = ? AND NgaySinh = ? AND DiaChi = ?",
      [HoTen.trim(), formattedDate, DiaChi.trim()]
    );

    if (existingStudent.length > 0) {
      await connection.rollback();
      return res
        .status(400)
        .json({ error: "Học sinh này đã tồn tại trong hệ thống." });
    }

    // 6. Sinh mã học sinh "đẹp" (HS260001)
    const MaHocSinh = await generateMaHocSinh(connection);

    // 7. Lưu vào database
    const query = `INSERT INTO hocsinh (MaHocSinh, HoTen, NgaySinh, MaGioiTinh, DiaChi, Email) VALUES (?, ?, ?, ?, ?, ?)`;
    await connection.query(query, [
      MaHocSinh,
      HoTen.trim(),
      formattedDate,
      MaGioiTinh,
      DiaChi.trim(),
      Email,
    ]);

    await connection.commit();
    res.json({
      message: "Tiếp nhận học sinh thành công!",
      MaHocSinh: MaHocSinh,
    });
  } catch (err) {
    await connection.rollback();
    console.error("Lỗi tiếp nhận:", err);
    // Bắt lỗi nếu MaGioiTinh chưa có trong DB
    if (err.code === "ER_NO_REFERENCED_ROW_2") {
      return res
        .status(400)
        .json({ error: "Mã giới tính không hợp lệ (GT1, GT2 hoặc GT3)." });
    }
    res.status(500).json({ error: "Lỗi hệ thống khi tiếp nhận hồ sơ." });
  } finally {
    connection.release();
  }
};

// API Tìm kiếm học sinh (Bản tinh chỉnh định dạng ngày)
exports.searchHocSinh = async (req, res) => {
  const { key } = req.query;

  try {
    if (!key || key.trim() === "") {
      return res.json([]);
    }

    const searchKey = `%${key.trim()}%`;

    // Dùng DATE_FORMAT để ép kiểu ngày về chuỗi YYYY-MM-DD
    const query = `
      SELECT 
        MaHocSinh, 
        HoTen, 
        DATE_FORMAT(NgaySinh, '%Y-%m-%d') AS NgaySinh, 
        MaGioiTinh 
      FROM hocsinh 
      WHERE (HoTen LIKE ? OR MaHocSinh LIKE ?)
      LIMIT 15
    `;

    const [rows] = await db.query(query, [searchKey, searchKey]);

    res.json(rows);
  } catch (err) {
    console.error("Lỗi tìm kiếm học sinh:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi tìm kiếm." });
  }
};

exports.updateHocSinh = async (req, res) => {
  const { MaHocSinh, HoTen, NgaySinh, MaGioiTinh, DiaChi, Email } = req.body;
  const connection = await db.getConnection();

  try {
    await connection.beginTransaction();

    // 1. Kiểm tra dữ liệu đầu vào
    if (!MaHocSinh || !HoTen || !NgaySinh || !MaGioiTinh || !DiaChi) {
      return res
        .status(400)
        .json({ error: "Vui lòng nhập đầy đủ thông tin học sinh." });
    }

    // 2. Kiểm tra định dạng ngày và tính tuổi
    const dateObj = new Date(NgaySinh);
    if (isNaN(dateObj))
      return res.status(400).json({ error: "Ngày sinh không hợp lệ." });
    const formattedDate = dateObj.toISOString().split("T")[0];

    // Lấy quy định tuổi từ database
    const [config] = await connection.query(
      "SELECT ten_tham_so, gia_tri FROM thamso WHERE ten_tham_so IN ('TuoiToiThieu', 'TuoiToiDa')"
    );
    const minAge =
      config.find((c) => c.ten_tham_so === "TuoiToiThieu")?.gia_tri || 15;
    const maxAge =
      config.find((c) => c.ten_tham_so === "TuoiToiDa")?.gia_tri || 20;

    const age = new Date().getFullYear() - dateObj.getFullYear();
    if (age < minAge || age > maxAge) {
      throw new Error(
        `Tuổi học sinh (${age}) không phù hợp quy định (${minAge}-${maxAge}).`
      );
    }

    // 3. Cập nhật vào bảng hocsinh
    const query = `
      UPDATE hocsinh 
      SET HoTen = ?, NgaySinh = ?, MaGioiTinh = ?, DiaChi = ?, Email = ? 
      WHERE MaHocSinh = ?
    `;
    const [result] = await connection.query(query, [
      HoTen.trim(),
      formattedDate,
      MaGioiTinh,
      DiaChi.trim(),
      Email,
      MaHocSinh,
    ]);

    if (result.affectedRows === 0) {
      throw new Error("Không tìm thấy học sinh để cập nhật.");
    }

    await connection.commit();
    res.json({ message: "Cập nhật hồ sơ học sinh thành công!" });
  } catch (err) {
    await connection.rollback();
    console.error("Lỗi cập nhật hồ sơ:", err);
    res
      .status(400)
      .json({ error: err.message || "Lỗi hệ thống khi cập nhật." });
  } finally {
    connection.release();
  }
};
// API Xóa học sinh khỏi lớp (Cập nhật lại sĩ số)
exports.xoaHocSinhKhoiLop = async (req, res) => {
  const { MaLop, MaHocSinh } = req.body;
  const connection = await db.getConnection();

  try {
    await connection.beginTransaction();

    // 1. Kiểm tra xem học sinh có thực sự ở trong lớp đó không
    const [check] = await connection.query(
      "SELECT * FROM chitietlop WHERE MaLop = ? AND MaHocSinh = ?",
      [MaLop, MaHocSinh]
    );

    if (check.length === 0) {
      await connection.rollback();
      return res
        .status(404)
        .json({ error: "Không tìm thấy học sinh này trong lớp." });
    }

    // 2. THỰC HIỆN SONG SONG: Xóa ở bảng chi tiết và Trừ sĩ số ở bảng lop
    await connection.query(
      "DELETE FROM chitietlop WHERE MaLop = ? AND MaHocSinh = ?",
      [MaLop, MaHocSinh]
    );

    await connection.query(
      "UPDATE lop SET SiSo = CASE WHEN SiSo > 0 THEN SiSo - 1 ELSE 0 END WHERE MaLop = ?",
      [MaLop]
    );

    await connection.commit();
    res.json({ message: "Đã xóa học sinh khỏi danh sách lớp thành công!" });
  } catch (err) {
    await connection.rollback();
    console.error("Lỗi xóa học sinh khỏi lớp:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi thực hiện xóa." });
  } finally {
    connection.release();
  }
};

// API Tra cứu học sinh (BM7) - Cập nhật theo bảng ketqua_monhoc
exports.traCuuHocSinh = async (req, res) => {
  const { maLop, maHocSinh, hoTen } = req.query;

  try {
    let query = `
      SELECT 
        hs.MaHocSinh, 
        hs.HoTen, 
        l.TenLop, 
        CONCAT(hn.NamHocBatDau, '-', hn.NamHocKetThuc) AS NamHoc,
        -- SỬA LỖI: Phải JOIN qua bảng 'lop' để lấy MaHocKyNamHoc
        (SELECT ROUND(AVG(bd.Diem), 1) FROM bangdiem bd 
         JOIN lop l_in ON bd.MaLop = l_in.MaLop
         JOIN hocky_namhoc hnk ON l_in.MaHocKyNamHoc = hnk.MaHocKyNamHoc 
         WHERE bd.MaHocSinh = hs.MaHocSinh AND hnk.TenHocKy LIKE '%1%') AS HK1,
        
        (SELECT ROUND(AVG(bd.Diem), 1) FROM bangdiem bd 
         JOIN lop l_in ON bd.MaLop = l_in.MaLop
         JOIN hocky_namhoc hnk ON l_in.MaHocKyNamHoc = hnk.MaHocKyNamHoc 
         WHERE bd.MaHocSinh = hs.MaHocSinh AND hnk.TenHocKy LIKE '%2%') AS HK2
      FROM hocsinh hs
      JOIN chitietlop ctl ON hs.MaHocSinh = ctl.MaHocSinh
      JOIN lop l ON ctl.MaLop = l.MaLop
      JOIN hocky_namhoc hn ON l.MaHocKyNamHoc = hn.MaHocKyNamHoc
      WHERE 1=1
    `;

    let params = [];
    if (maLop?.trim()) {
      query += " AND l.MaLop = ?";
      params.push(maLop.trim());
    }
    if (maHocSinh?.trim()) {
      query += " AND hs.MaHocSinh = ?";
      params.push(maHocSinh.trim());
    }
    if (hoTen?.trim()) {
      query += " AND hs.HoTen LIKE ?";
      params.push(`%${hoTen.trim()}%`);
    }

    const [rows] = await db.query(query, params);

    const finalResult = rows.map((item) => {
      const hk1 = item.HK1 || 0;
      const hk2 = item.HK2 || 0;
      // Công thức: $GPA = (HK1 + HK2 \times 2) / 3$
      const caNam = (hk1 + hk2 * 2) / 3;

      return {
        maHocSinh: item.MaHocSinh,
        hoTen: item.HoTen,
        lop: item.TenLop,
        namHoc: item.NamHoc,
        diemHK1: hk1 > 0 ? hk1 : "N/A",
        diemHK2: hk2 > 0 ? hk2 : "N/A",
        diemCaNam: hk1 > 0 && hk2 > 0 ? caNam.toFixed(1) : "N/A",
      };
    });

    res.json(finalResult);
  } catch (err) {
    console.error("Lỗi SQL:", err.sqlMessage); // Hiện lỗi chi tiết ra console để debug
    res
      .status(500)
      .json({
        error: "Lỗi hệ thống khi tra cứu dữ liệu.",
        details: err.sqlMessage,
      });
  }
};
