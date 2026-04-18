const db = require("../config/db");

// API Tạo mới lớp
exports.taoMoiLop = async (req, res) => {
  const { TenLop, MaKhoiLop, MaHocKyNamHoc, LoaiHocKy } = req.body;

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    // 1. Kiểm tra đầu vào
    if (!TenLop || !MaKhoiLop || !MaHocKyNamHoc) {
      return res.status(400).json({ error: "Vui lòng nhập đầy đủ thông tin." });
    }

    // 2. Logic xử lý "Cả năm" (LoaiHocKy = 3)
    let listMaHK = [];
    if (LoaiHocKy === 3) {
      const yearSuffix = MaHocKyNamHoc.split("-")[1];
      listMaHK = [`HK1-${yearSuffix}`, `HK2-${yearSuffix}`];
    } else {
      listMaHK = [MaHocKyNamHoc];
    }

    for (const maHK of listMaHK) {
      const [existing] = await connection.query(
        "SELECT MaLop FROM lop WHERE TenLop = ? AND MaHocKyNamHoc = ?",
        [TenLop, maHK]
      );

      if (existing.length > 0) {
        throw new Error(`Lớp ${TenLop} đã tồn tại trong ${maHK}.`);
      }

      const MaLop = (TenLop.replace(/\s+/g, "") + maHK).toUpperCase();

      await connection.query(
        "INSERT INTO lop (MaLop, TenLop, MaKhoiLop, MaHocKyNamHoc, SiSo) VALUES (?, ?, ?, ?, 0)",
        [MaLop, TenLop, MaKhoiLop, maHK]
      );
    }

    await connection.commit();
    res.json({ message: "Tạo lớp học thành công!" });
  } catch (err) {
    await connection.rollback();
    res.status(400).json({ error: err.message });
  } finally {
    connection.release();
  }
};

exports.getLopHoc = async (req, res) => {
  try {
    const query = `
      SELECT l.MaLop, l.TenLop, l.MaHocKyNamHoc, l.SiSo, hn.TenHocKy, hn.NamHocBatDau, hn.NamHocKetThuc
      FROM lop l
      JOIN hocky_namhoc hn ON l.MaHocKyNamHoc = hn.MaHocKyNamHoc
      ORDER BY hn.NamHocBatDau DESC, l.TenLop ASC
    `;
    const [rows] = await db.query(query);
    const dropdownData = rows.map((item) => ({
      maLop: item.MaLop,
      tenLop: item.TenLop,
      siSoHienTai: item.SiSo,
      hienThi: item.MaLop,
      maHocKyNamHoc: item.MaHocKyNamHoc,
    }));
    res.json(dropdownData);
  } catch (err) {
    res.status(500).json({ error: "Lỗi hệ thống khi lấy danh sách lớp học." });
  }
};

exports.searchMaLop = async (req, res) => {
  const { key } = req.query;
  try {
    if (!key || key.trim() === "") return res.json([]);
    const searchKey = `%${key.trim()}%`;
    const query = `
      SELECT l.MaLop, l.TenLop, hn.NamHocBatDau, hn.NamHocKetThuc 
      FROM lop l 
      JOIN hocky_namhoc hn ON l.MaHocKyNamHoc = hn.MaHocKyNamHoc
      WHERE l.MaLop LIKE ? OR l.TenLop LIKE ?
      ORDER BY hn.NamHocBatDau DESC 
      LIMIT 10
    `;
    const [rows] = await db.query(query, [searchKey, searchKey]);
    const result = rows.map((item) => ({
      maLop: item.MaLop,
      hienThi: `${item.MaLop} (${item.TenLop} - ${item.NamHocBatDau}-${item.NamHocKetThuc})`,
    }));
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: "Lỗi hệ thống khi tìm kiếm lớp." });
  }
};

// API lấy danh sách học sinh của một lớp cụ thể
exports.getHocSinhTheoLop = async (req, res) => {
  const { MaLop } = req.params;

  try {
    // SỬA LỖI: Lấy thêm MaGioiTinh để App hiển thị đúng Nam/Nữ
    const query = `
      SELECT 
        hs.MaHocSinh, 
        hs.HoTen, 
        hs.MaGioiTinh,
        DATE_FORMAT(hs.NgaySinh, '%d/%m/%Y') AS NgaySinh
      FROM chitietlop ctl
      JOIN hocsinh hs ON ctl.MaHocSinh = hs.MaHocSinh
      WHERE ctl.MaLop = ?
      ORDER BY hs.HoTen ASC
    `;

    const [rows] = await db.query(query, [MaLop]);
    res.json(rows);
  } catch (err) {
    console.error("Lỗi lấy danh sách học sinh theo lớp:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi tải danh sách học sinh." });
  }
};

exports.luuDanhSachLop = async (req, res) => {
  const { MaLop, DanhSachMaHS } = req.body;
  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();
    const [[config]] = await connection.query(
      "SELECT gia_tri FROM thamso WHERE ten_tham_so = 'SiSoToiDa'"
    );
    const [[lopInfo]] = await connection.query(
      "SELECT MaHocKyNamHoc, TenLop FROM lop WHERE MaLop = ?",
      [MaLop]
    );
    if (!lopInfo) throw new Error("Lớp học không tồn tại.");
    if (DanhSachMaHS.length > config.gia_tri) {
      throw new Error(
        `Danh sách vượt quá sĩ số tối đa cho phép (${config.gia_tri}).`
      );
    }
    for (const MaHocSinh of DanhSachMaHS) {
      const [otherClass] = await connection.query(
        `SELECT ctl.MaLop FROM chitietlop ctl 
         JOIN lop l ON ctl.MaLop = l.MaLop 
         WHERE ctl.MaHocSinh = ? AND l.MaHocKyNamHoc = ? AND ctl.MaLop != ?`,
        [MaHocSinh, lopInfo.MaHocKyNamHoc, MaLop]
      );
      if (otherClass.length > 0) {
        throw new Error(
          `Học sinh mã ${MaHocSinh} đã thuộc lớp khác (${otherClass[0].MaLop}) trong học kỳ này.`
        );
      }
    }
    await connection.query("DELETE FROM chitietlop WHERE MaLop = ?", [MaLop]);
    if (DanhSachMaHS.length > 0) {
      const values = DanhSachMaHS.map((ma) => [MaLop, ma]);
      await connection.query(
        "INSERT INTO chitietlop (MaLop, MaHocSinh) VALUES ?",
        [values]
      );
    }
    await connection.query("UPDATE lop SET SiSo = ? WHERE MaLop = ?", [
      DanhSachMaHS.length,
      MaLop,
    ]);
    await connection.commit();
    res.json({ message: `Đã lưu danh sách lớp ${lopInfo.TenLop} thành công!` });
  } catch (err) {
    await connection.rollback();
    res
      .status(400)
      .json({ error: err.message || "Lỗi hệ thống khi lưu danh sách." });
  } finally {
    connection.release();
  }
};
