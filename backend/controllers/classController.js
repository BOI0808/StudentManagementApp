const db = require("../config/db");

// API Tạo mới lớp
exports.taoMoiLop = async (req, res) => {
  const { TenLop, MaKhoiLop, MaHocKyNamHoc, LoaiHocKy } = req.body;
  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();
    if (!TenLop || !MaKhoiLop || !MaHocKyNamHoc) {
      return res.status(400).json({ error: "Vui lòng nhập đầy đủ thông tin." });
    }
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

exports.getHocSinhTheoLop = async (req, res) => {
  const { MaLop } = req.params;
  try {
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
      return res.status(400).json({
        success: false,
        error: `Danh sách (${DanhSachMaHS.length}) vượt quá sĩ số tối đa (${config.gia_tri}).`,
      });
    }

    // KIỂM TRA CHI TIẾT TỪNG HỌC SINH
    let validationErrors = [];
    for (const MaHocSinh of DanhSachMaHS) {
      const [otherClass] = await connection.query(
        `SELECT ctl.MaLop FROM chitietlop ctl 
         JOIN lop l ON ctl.MaLop = l.MaLop 
         WHERE ctl.MaHocSinh = ? AND l.MaHocKyNamHoc = ? AND ctl.MaLop != ? LIMIT 1`,
        [MaHocSinh, lopInfo.MaHocKyNamHoc, MaLop]
      );

      if (otherClass.length > 0) {
        validationErrors.push({
          maHocSinh: MaHocSinh,
          message: "Học sinh này đã có lớp khác",
        });
      }
    }

    if (validationErrors.length > 0) {
      await connection.rollback();
      return res.status(400).json({ success: false, errors: validationErrors });
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
    res.json({ success: true, message: "Lưu thành công!" });
  } catch (err) {
    await connection.rollback();
    res
      .status(500)
      .json({ success: false, error: err.message || "Lỗi hệ thống." });
  } finally {
    connection.release();
  }
};

exports.importStudentToClassExcel = async (req, res) => {
  const { MaLop } = req.body;

  if (!MaLop) {
    return res.status(400).json({
      success: false,
      error: "Thiếu MaLop trong body.",
    });
  }

  if (!req.file || !req.file.buffer) {
    return res.status(400).json({
      success: false,
      error: "Vui lòng đính kèm file Excel.",
    });
  }

  // Parse workbook
  let rows;
  try {
    const workbook = xlsx.read(req.file.buffer, { type: "buffer" });
    const sheetName = workbook.SheetNames[0];
    if (!sheetName) {
      return res.status(400).json({
        success: false,
        error: "File Excel không có sheet nào.",
      });
    }
    const worksheet = workbook.Sheets[sheetName];
    rows = xlsx.utils.sheet_to_json(worksheet, { defval: "" });
    if (!Array.isArray(rows) || rows.length === 0) {
      return res.status(400).json({
        success: false,
        error: "File Excel rỗng hoặc không đúng định dạng.",
      });
    }

    // Normalize header keys (trim)
    rows = rows.map((r) => {
      const normalized = {};
      Object.keys(r).forEach((k) => {
        normalized[String(k).trim()] = r[k];
      });
      return normalized;
    });
  } catch (err) {
    console.error("Lỗi đọc file Excel:", err);
    return res.status(400).json({
      success: false,
      error: "Không thể đọc file Excel. Đảm bảo là .xlsx hợp lệ.",
    });
  }

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    // 1) Lấy thông tin lớp
    const [lopRows] = await connection.query(
      "SELECT MaHocKyNamHoc, SiSo FROM lop WHERE MaLop = ?",
      [MaLop]
    );
    if (lopRows.length === 0) {
      throw new Error("Lớp học không tồn tại.");
    }
    const lopInfo = lopRows[0];

    // 2) Lấy SiSoToiDa
    const [ssRows] = await connection.query(
      "SELECT gia_tri FROM thamso WHERE ten_tham_so = 'SiSoToiDa' LIMIT 1"
    );
    const siSoToiDa = Number(ssRows[0]?.gia_tri || 0);

    // 3) Kiểm tra sĩ số hiện tại + số dòng Excel không vượt quá SiSoToiDa
    const totalToAdd = rows.length;
    if (Number(lopInfo.SiSo) + totalToAdd > siSoToiDa) {
      throw new Error(
        `Sĩ số vượt quá giới hạn. Hiện tại: ${lopInfo.SiSo}, muốn thêm: ${totalToAdd}, Tối đa: ${siSoToiDa}`
      );
    }

    // Required header columns
    const requiredCols = ["MaHocSinh", "HoTen", "NgaySinh", "GioiTinh"];
    const firstRowKeys = Object.keys(rows[0] || {});
    for (const col of requiredCols) {
      if (!firstRowKeys.includes(col)) {
        throw new Error(`Thiếu cột '${col}' trong file Excel`);
      }
    }

    // Helpers
    const excelDateToJSDate = (v) => {
      if (v == null || v === "") return null;
      if (typeof v === "number") {
        // Excel serial date to JS date
        // Excel's epoch starts at 1900 and has a bug; common conversion:
        const jsDate = new Date(Math.round((v - (25567 + 2)) * 86400 * 1000));
        return jsDate;
      }
      // Try parse string
      const d = new Date(v);
      return isNaN(d) ? null : d;
    };
    const formatDate = (d) => {
      if (!(d instanceof Date)) return null;
      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, "0");
      const dd = String(d.getDate()).padStart(2, "0");
      return `${yyyy}-${mm}-${dd}`;
    };
    const mapGioiTinh = (s) => {
      if (!s && s !== 0) return null;
      const t = String(s).trim().toLowerCase();
      if (t === "nam") return "GT1";
      if (t === "nữ" || t === "nu" || t === "nữ".toLowerCase()) return "GT2";
      return "GT3"; // Khác
    };

    // 4) Check duplicates inside file
    const seen = new Set();
    for (let i = 0; i < rows.length; i++) {
      const excelRowNumber = i + 2; // header row 1
      const MaHocSinh = String(rows[i].MaHocSinh || "").trim();
      if (!MaHocSinh) {
        throw new Error(`Dòng ${excelRowNumber}: Thiếu MaHocSinh.`);
      }
      if (seen.has(MaHocSinh)) {
        throw new Error(
          `Dòng ${excelRowNumber}: Mã học sinh bị trùng trong file (${MaHocSinh}).`
        );
      }
      seen.add(MaHocSinh);
    }

    // 5) Validate each row and prepare batch insert values
    const values = []; // array of [MaLop, MaHocSinh]
    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];
      const excelRowNumber = i + 2;

      const MaHocSinh = String(row.MaHocSinh || "").trim();
      const HoTen = String(row.HoTen || "").trim();
      const NgaySinhRaw = row.NgaySinh;
      const GioiTinhRaw = row.GioiTinh;

      if (!MaHocSinh) {
        throw new Error(`Dòng ${excelRowNumber}: Thiếu MaHocSinh.`);
      }
      if (!HoTen) {
        throw new Error(`Dòng ${excelRowNumber}: Thiếu HoTen.`);
      }
      if (!NgaySinhRaw && NgaySinhRaw !== 0) {
        throw new Error(`Dòng ${excelRowNumber}: Thiếu NgaySinh.`);
      }
      if (!GioiTinhRaw && GioiTinhRaw !== 0) {
        throw new Error(`Dòng ${excelRowNumber}: Thiếu GioiTinh.`);
      }

      // Parse date
      const parsedDate = excelDateToJSDate(NgaySinhRaw);
      if (!parsedDate) {
        throw new Error(`Dòng ${excelRowNumber}: Ngày sinh không hợp lệ.`);
      }
      const formattedDate = formatDate(parsedDate);

      // Map gender to MaGioiTinh
      const mappedGT = mapGioiTinh(GioiTinhRaw);

      // 6) Kiểm tra học sinh tồn tại
      const [hsRows] = await connection.query(
        "SELECT MaHocSinh, HoTen, DATE_FORMAT(NgaySinh, '%Y-%m-%d') AS NgaySinh, MaGioiTinh FROM hocsinh WHERE MaHocSinh = ? LIMIT 1",
        [MaHocSinh]
      );
      if (hsRows.length === 0) {
        throw new Error(
          `Dòng ${excelRowNumber}: Không tìm thấy học sinh với mã ${MaHocSinh}.`
        );
      }
      const hs = hsRows[0];

      // 7) So khớp dữ liệu
      const dbHoTen = String(hs.HoTen || "").trim();
      const dbNgaySinh = String(hs.NgaySinh || "").trim();
      const dbMaGioiTinh = String(hs.MaGioiTinh || "").trim();

      if (
        dbHoTen.toLowerCase() !== HoTen.toLowerCase() ||
        dbNgaySinh !== formattedDate ||
        dbMaGioiTinh !== mappedGT
      ) {
        throw new Error(
          `Dòng ${excelRowNumber}: Thông tin học sinh không khớp với mã ${MaHocSinh}.`
        );
      }

      // 8) Kiểm tra đã có trong lớp target
      const [inClass] = await connection.query(
        "SELECT 1 FROM chitietlop WHERE MaLop = ? AND MaHocSinh = ? LIMIT 1",
        [MaLop, MaHocSinh]
      );
      if (inClass.length > 0) {
        throw new Error(
          `Dòng ${excelRowNumber}: Học sinh đã có trong lớp ${MaLop}.`
        );
      }

      // 9) Kiểm tra ràng buộc học kỳ (đã có lớp khác trong same MaHocKyNamHoc)
      const [otherClass] = await connection.query(
        `SELECT c.MaLop
         FROM chitietlop c
         JOIN lop l ON c.MaLop = l.MaLop
         WHERE c.MaHocSinh = ? AND l.MaHocKyNamHoc = ? LIMIT 1`,
        [MaHocSinh, lopInfo.MaHocKyNamHoc]
      );
      if (otherClass.length > 0) {
        throw new Error(
          `Dòng ${excelRowNumber}: Học sinh đã có lớp khác (${otherClass[0].MaLop}) trong học kỳ ${lopInfo.MaHocKyNamHoc}.`
        );
      }

      // Save to batch
      values.push([MaLop, MaHocSinh]);
    }

    // 10) Thực hiện insert hàng loạt nếu có
    if (values.length > 0) {
      await connection.query(
        "INSERT INTO chitietlop (MaLop, MaHocSinh) VALUES ?",
        [values]
      );

      // Update SiSo (tăng)
      await connection.query("UPDATE lop SET SiSo = SiSo + ? WHERE MaLop = ?", [
        values.length,
        MaLop,
      ]);
    }

    await connection.commit();
    return res.json({
      success: true,
      message: `Đã import thành công ${values.length} học sinh vào lớp ${MaLop}.`,
    });
  } catch (err) {
    try {
      await connection.rollback();
    } catch (rbErr) {
      console.error("Rollback failed:", rbErr);
    }
    console.error("importStudentToClassExcel error:", err);
    const msg = err.message || "Lỗi hệ thống khi import file Excel.";
    return res.status(400).json({
      success: false,
      error: `${msg} Toàn bộ quá trình đã bị hủy.`,
    });
  } finally {
    connection.release();
  }
};
