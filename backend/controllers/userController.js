const db = require("../config/db");
const xlsx = require("xlsx");

const generateMaSo = async (connection) => {
  const prefix = "GV";
  const year = new Date().getFullYear().toString().slice(-2);
  const searchPattern = `${prefix}${year}%`;

  const [rows] = await connection.query(
    "SELECT MaSo FROM nguoidung WHERE MaSo LIKE ? ORDER BY MaSo DESC LIMIT 1",
    [searchPattern]
  );

  let nextNumber = 1;
  if (rows.length > 0) {
    const lastNumber = parseInt(rows[0].MaSo.slice(-3));
    nextNumber = lastNumber + 1;
  }

  return `${prefix}${year}${nextNumber.toString().padStart(3, "0")}`;
};

const validateEmail = (email) => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
};

const validatePhone = (phone) => {
  return /^(03|05|07|08|09|01[2|6|8|9])+([0-9]{8})\b/.test(phone);
};

const validateUsername = (username) => {
  return /^[a-zA-Z0-9_]{5,20}$/.test(username);
};

exports.importUsersExcel = async (req, res) => {
  if (!req.file) {
    return res
      .status(400)
      .json({ success: false, error: "Vui lòng đính kèm file Excel." });
  }

  const connection = await db.getConnection();
  try {
    const workbook = xlsx.read(req.file.buffer, { type: "buffer" });
    const sheetName = workbook.SheetNames[0];
    const worksheet = workbook.Sheets[sheetName];

    // 1. Chuyển đổi sang JSON và chuẩn hóa Key (xóa khoảng trắng thừa ở tiêu đề)
    const rawData = xlsx.utils.sheet_to_json(worksheet, { defval: "" });
    const data = rawData.map((row) => {
      const newRow = {};
      Object.keys(row).forEach((key) => {
        newRow[key.trim()] = row[key];
      });
      return newRow;
    });

    let errors = [];
    let usersToInsert = [];
    let usernamesInFile = new Set();
    let emailsInFile = new Set();

    // GIAI ĐOẠN 1: Validate toàn bộ dữ liệu
    for (let i = 0; i < data.length; i++) {
      const row = data[i];
      const rowIndex = i + 2;

      // ÁNH XẠ THÔNG MINH: Hỗ trợ cả tiếng Việt (có dấu/có cách) và tiếng Anh
      const HoTen = String(
        row["Họ và tên"] || row["Họ tên"] || row.HoTen || ""
      ).trim();
      const TenDangNhap = String(
        row["Tên đăng nhập"] || row.TenDangNhap || ""
      ).trim();
      const MatKhau = String(row["Mật khẩu"] || row.MatKhau || "").trim();
      const Email = String(row["Email"] || row.Email || "").trim();
      const SoDienThoai = String(
        row["Số điện thoại"] || row.SoDienThoai || row.SĐT || ""
      ).trim();
      const DanhSachQuyen = String(
        row["Quyền hạn"] || row.DanhSachQuyen || row.Quyen || ""
      ).trim();

      // 1. Kiểm tra thiếu thông tin bắt buộc
      if (!HoTen || !TenDangNhap || !MatKhau || !Email || !SoDienThoai) {
        errors.push({
          row: rowIndex,
          message: "Thiếu thông tin bắt buộc (Họ tên, TK, MK, Email, SĐT).",
        });
        continue;
      }

      // 2. Kiểm tra định dạng
      if (!validateUsername(TenDangNhap)) {
        errors.push({
          row: rowIndex,
          message: "Tên đăng nhập không hợp lệ (5-20 ký tự, không dấu cách).",
        });
      }
      if (!validateEmail(Email)) {
        errors.push({
          row: rowIndex,
          message: "Định dạng Email không hợp lệ.",
        });
      }
      if (!validatePhone(SoDienThoai)) {
        errors.push({
          row: rowIndex,
          message: "Số điện thoại không đúng định dạng VN.",
        });
      }
      if (MatKhau.length < 6) {
        errors.push({
          row: rowIndex,
          message: "Mật khẩu phải từ 6 ký tự trở lên.",
        });
      }

      // 3. Kiểm tra trùng lặp trong file
      if (usernamesInFile.has(TenDangNhap)) {
        errors.push({
          row: rowIndex,
          message: `Tên đăng nhập '${TenDangNhap}' bị lặp trong file.`,
        });
      }
      if (emailsInFile.has(Email)) {
        errors.push({
          row: rowIndex,
          message: `Email '${Email}' bị lặp trong file.`,
        });
      }
      usernamesInFile.add(TenDangNhap);
      emailsInFile.add(Email);

      // 4. Kiểm tra trùng lặp trong DB
      const [existing] = await connection.query(
        "SELECT TenDangNhap, Email FROM nguoidung WHERE TenDangNhap = ? OR Email = ? LIMIT 1",
        [TenDangNhap, Email]
      );

      if (existing.length > 0) {
        if (existing[0].TenDangNhap === TenDangNhap) {
          errors.push({
            row: rowIndex,
            message: `Tên đăng nhập '${TenDangNhap}' đã tồn tại trên hệ thống.`,
          });
        } else {
          errors.push({
            row: rowIndex,
            message: `Email '${Email}' đã tồn tại trên hệ thống.`,
          });
        }
      }

      usersToInsert.push({
        HoTen,
        TenDangNhap,
        MatKhau,
        Email,
        SoDienThoai,
        DanhSachQuyen,
      });
    }

    if (errors.length > 0) {
      return res.status(400).json({ success: false, errors: errors });
    }

    // GIAI ĐOẠN 2: Insert (Transaction)
    await connection.beginTransaction();

    for (const user of usersToInsert) {
      const MaSo = await generateMaSo(connection);
      await connection.query(
        "INSERT INTO nguoidung (MaSo, HoTen, TenDangNhap, MatKhau, Email, SoDienThoai, PhanQuyen) VALUES (?, ?, ?, ?, ?, ?, ?)",
        [
          MaSo,
          user.HoTen,
          user.TenDangNhap,
          user.MatKhau,
          user.Email,
          user.SoDienThoai,
          "Giáo Viên",
        ]
      );

      if (user.DanhSachQuyen) {
        const quyenCodes = user.DanhSachQuyen.split(",")
          .map((q) => q.trim().toUpperCase())
          .filter((q) => q !== "");

        if (quyenCodes.length > 0) {
          const quyenValues = quyenCodes.map((maCN) => [MaSo, maCN]);
          await connection.query(
            "INSERT INTO nguoidung_quyen (MaSo, MaCN) VALUES ?",
            [quyenValues]
          );
        }
      }
    }

    await connection.commit();
    res.json({
      success: true,
      message: `Đã nhập thành công ${usersToInsert.length} tài khoản!`,
    });
  } catch (err) {
    await connection.rollback();
    console.error("Lỗi Import:", err);
    res
      .status(500)
      .json({ success: false, error: "Lỗi hệ thống khi xử lý file Excel." });
  } finally {
    connection.release();
  }
};

exports.createUser = async (req, res) => {
  const { HoTen, TenDangNhap, MatKhau, Email, SoDienThoai, DanhSachQuyen } =
    req.body;

  if (
    !HoTen?.trim() ||
    !TenDangNhap?.trim() ||
    !MatKhau ||
    !Email?.trim() ||
    !SoDienThoai?.trim()
  ) {
    return res
      .status(400)
      .json({ error: "Vui lòng nhập đầy đủ thông tin bắt buộc." });
  }

  if (!validateEmail(Email))
    return res.status(400).json({ error: "Định dạng Email không hợp lệ." });
  if (!validatePhone(SoDienThoai))
    return res
      .status(400)
      .json({ error: "Số điện thoại không đúng định dạng Việt Nam." });
  if (!validateUsername(TenDangNhap))
    return res.status(400).json({ error: "Tên đăng nhập phải từ 5-20 ký tự." });
  if (MatKhau.length < 6)
    return res.status(400).json({ error: "Mật khẩu phải có ít nhất 6 ký tự." });

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    const [existing] = await connection.query(
      "SELECT MaSo FROM nguoidung WHERE TenDangNhap = ? OR Email = ?",
      [TenDangNhap.trim(), Email.trim()]
    );
    if (existing.length > 0) {
      return res
        .status(400)
        .json({ error: "Tên đăng nhập hoặc Email đã được sử dụng." });
    }

    const MaSo = await generateMaSo(connection);

    await connection.query(
      "INSERT INTO nguoidung (MaSo, HoTen, TenDangNhap, MatKhau, Email, SoDienThoai, PhanQuyen) VALUES (?, ?, ?, ?, ?, ?, ?)",
      [
        MaSo,
        HoTen.trim(),
        TenDangNhap.trim(),
        MatKhau,
        Email.trim(),
        SoDienThoai.trim(),
        "Giáo Viên",
      ]
    );

    if (DanhSachQuyen && DanhSachQuyen.length > 0) {
      const values = DanhSachQuyen.map((maCN) => [MaSo, maCN]);
      await connection.query(
        "INSERT INTO nguoidung_quyen (MaSo, MaCN) VALUES ?",
        [values]
      );
    }

    await connection.commit();
    res.json({ message: "Tạo tài khoản thành công!", MaSo: MaSo });
  } catch (err) {
    await connection.rollback();
    res.status(500).json({ error: "Lỗi hệ thống khi tạo tài khoản" });
  } finally {
    connection.release();
  }
};

exports.getAllAccounts = async (req, res) => {
  try {
    const rightsMap = {
      CNTNHS: "1",
      CNLDSL: "2",
      CNLDSHSCL: "3",
      CNLDSNH: "4",
      CNLDSKL: "5",
      CNLDSMH: "6",
      CNTCHS: "7",
      CNNBD: "8",
      CNNDSCLKT: "9",
      CNLBCTKM: "10",
      CNLBCTKHK: "11",
      CNCDTSHT: "12",
    };

    const query = `
      SELECT nd.MaSo, nd.HoTen, nd.Email, nd.SoDienThoai, nd.TenDangNhap, nd.MatKhau,
             GROUP_CONCAT(ndq.MaCN) AS DS_Quyen
      FROM nguoidung nd
      LEFT JOIN nguoidung_quyen ndq ON nd.MaSo = ndq.MaSo
      WHERE nd.TrangThai = 1 
      GROUP BY nd.MaSo
    `;

    const [rows] = await db.query(query);

    const result = rows.map((user) => {
      let mappedRights = user.DS_Quyen
        ? `{${user.DS_Quyen.split(",")
            .map((code) => parseInt(rightsMap[code.trim().toUpperCase()]))
            .filter((n) => !isNaN(n))
            .sort((a, b) => a - b)
            .join(",")}}`
        : "{}";

      if (user.MaSo && user.MaSo.startsWith("ADMIN"))
        mappedRights = "{1,2,3,4,5,6,7,8,9,10,11,12}";

      return {
        MaSo: user.MaSo,
        HoTen: user.HoTen,
        Email: user.Email,
        SoDienThoai: user.SoDienThoai,
        TenDangNhap: user.TenDangNhap,
        MatKhau: user.MatKhau,
        QuyenHeThong: mappedRights,
      };
    });

    res.json(result);
  } catch (err) {
    res.status(500).json({ error: "Lỗi hệ thống khi tải dữ liệu." });
  }
};

exports.updateAccount = async (req, res) => {
  const { id } = req.params;
  const { HoTen, SoDienThoai, TenDangNhap, Email, MatKhau, DanhSachQuyen } =
    req.body;

  if (
    !HoTen?.trim() ||
    !TenDangNhap?.trim() ||
    !Email?.trim() ||
    !SoDienThoai?.trim()
  ) {
    return res
      .status(400)
      .json({ error: "Vui lòng nhập đầy đủ thông tin bắt buộc." });
  }

  if (!validateEmail(Email))
    return res.status(400).json({ error: "Email không hợp lệ." });
  if (!validatePhone(SoDienThoai))
    return res.status(400).json({ error: "SĐT không hợp lệ." });

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    let updateFields = [
      "HoTen = ?",
      "SoDienThoai = ?",
      "TenDangNhap = ?",
      "Email = ?",
    ];
    let queryParams = [
      HoTen.trim(),
      SoDienThoai.trim(),
      TenDangNhap.trim(),
      Email.trim(),
    ];

    if (MatKhau && MatKhau !== "********") {
      if (MatKhau.length < 6)
        throw new Error("Mật khẩu mới phải có ít nhất 6 ký tự.");
      updateFields.push("MatKhau = ?");
      queryParams.push(MatKhau);
    }

    queryParams.push(id);
    await connection.query(
      `UPDATE nguoidung SET ${updateFields.join(", ")} WHERE MaSo = ?`,
      queryParams
    );

    if (DanhSachQuyen && Array.isArray(DanhSachQuyen)) {
      await connection.query("DELETE FROM nguoidung_quyen WHERE MaSo = ?", [
        id,
      ]);
      if (DanhSachQuyen.length > 0) {
        const quyenValues = DanhSachQuyen.map((maCN) => [id, maCN]);
        await connection.query(
          "INSERT INTO nguoidung_quyen (MaSo, MaCN) VALUES ?",
          [quyenValues]
        );
      }
    }

    await connection.commit();
    res.json({ message: "Cập nhật tài khoản thành công!" });
  } catch (err) {
    await connection.rollback();
    res
      .status(400)
      .json({ error: err.message || "Lỗi hệ thống khi cập nhật." });
  } finally {
    connection.release();
  }
};

exports.softDeleteAccount = async (req, res) => {
  const { id } = req.params;
  try {
    const [result] = await db.query(
      "UPDATE nguoidung SET TrangThai = 0 WHERE MaSo = ?",
      [id]
    );
    if (result.affectedRows === 0)
      return res.status(404).json({ error: "Không tìm thấy tài khoản." });
    res.json({ message: "Đã ngưng hoạt động tài khoản này thành công!" });
  } catch (err) {
    res.status(500).json({ error: "Lỗi hệ thống khi xóa tài khoản." });
  }
};
