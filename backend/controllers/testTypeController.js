const db = require("../config/db");

const generateMaLoaiKT = (ten) => {
  // 1. Loại bỏ dấu tiếng Việt và chuyển sang chữ hoa
  let str = ten
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase();

  // 2. Viết tắt các từ thông dụng (Phút -> P, Tiết -> T)
  str = str.replace(/PHUT/g, "P").replace(/TIET/g, "T");

  // 3. Lấy các chữ số và chữ cái đầu của mỗi từ còn lại
  const words = str.split(/\s+/);
  const result = words
    .map((word) => {
      const numbers = word.match(/\d+/g); // Giữ lại số (như 15, 1)
      if (numbers) return numbers.join("");
      return word.charAt(0); // Lấy chữ cái đầu
    })
    .join("");

  // 4. Ghép tiền tố KT và giới hạn 10 ký tự
  return ("KT" + result).slice(0, 10);
};

// 1. Lấy danh sách đang hoạt động để đổ vào bảng
exports.getAllActiveLoaiKT = async (req, res) => {
  try {
    // Chỉ lấy những loại có TrangThai = 1
    const [rows] = await db.query(
      "SELECT MaLoaiKiemTra, TenLoaiKiemTra, HeSo FROM loaihinhkiemtra WHERE TrangThai = 1"
    );
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi khi lấy danh sách loại kiểm tra." });
  }
};

// 2. Thêm mới loại hình kiểm tra
exports.createLoaiKT = async (req, res) => {
  const { TenLoaiKiemTra, HeSo } = req.body;

  if (!TenLoaiKiemTra || !HeSo) {
    return res.status(400).json({ error: "Vui lòng nhập tên và hệ số." });
  }

  try {
    // 1. Kiểm tra xem tên này đã tồn tại trong DB chưa
    const [existing] = await db.query(
      "SELECT MaLoaiKiemTra, TrangThai FROM loaihinhkiemtra WHERE TenLoaiKiemTra = ?",
      [TenLoaiKiemTra]
    );

    if (existing.length > 0) {
      const item = existing[0];

      // TRƯỜNG HỢP A: Đang ngưng hoạt động (TrangThai = 0) -> Bật lại
      if (item.TrangThai === 0) {
        await db.query(
          "UPDATE loaihinhkiemtra SET TrangThai = 1, HeSo = ? WHERE MaLoaiKiemTra = ?",
          [HeSo, item.MaLoaiKiemTra]
        );
        return res.json({
          message: "Thêm loại kiểm tra mới thành công!",
          MaLoaiKiemTra: item.MaLoaiKiemTra,
        });
      }

      // TRƯỜNG HỢP B: Đang hoạt động rồi (TrangThai = 1) -> Báo lỗi trùng
      else {
        return res.status(400).json({
          error: "Loại kiểm tra này đã tồn tại và đang hoạt động.",
        });
      }
    }

    // 2. TRƯỜNG HỢP C: Chưa từng tồn tại -> Thêm mới hoàn toàn
    const MaLoaiKiemTra = generateMaLoaiKT(TenLoaiKiemTra);

    const query = `
      INSERT INTO loaihinhkiemtra (MaLoaiKiemTra, TenLoaiKiemTra, HeSo, TrangThai)
      VALUES (?, ?, ?, 1)
    `;

    await db.query(query, [MaLoaiKiemTra, TenLoaiKiemTra, HeSo]);

    res.json({
      message: "Thêm loại kiểm tra mới thành công!",
      MaLoaiKiemTra: MaLoaiKiemTra,
    });
  } catch (err) {
    // Xử lý lỗi trùng Mã (nếu hàm generate sinh ra mã trùng với loại khác)
    if (err.code === "ER_DUP_ENTRY") {
      return res.status(400).json({
        error: "Mã viết tắt bị trùng, vui lòng đặt tên khác một chút.",
      });
    }
    res.status(500).json({ error: "Lỗi hệ thống khi xử lý loại kiểm tra." });
  }
};

// 3. Xóa mềm (Soft Delete) khi nhấn icon thùng rác
exports.softDeleteLoaiKT = async (req, res) => {
  const { MaLoaiKiemTra } = req.params; // MaLoaiKiemTra

  try {
    // Không DELETE thật mà chỉ UPDATE TrangThai về 0
    await db.query(
      "UPDATE loaihinhkiemtra SET TrangThai = 0 WHERE MaLoaiKiemTra = ?",
      [MaLoaiKiemTra]
    );
    res.json({ message: "Đã xóa loại hình kiểm tra này." });
  } catch (err) {
    res.status(500).json({ error: "Lỗi khi cập nhật trạng thái xóa." });
  }
};

// API Cập nhật tên và hệ số của một loại kiểm tra
exports.updateHeSoLoaiKT = async (req, res) => {
  const { MaLoaiKiemTra } = req.params;
  const { TenLoaiKiemTra, HeSo } = req.body;

  if (!TenLoaiKiemTra || HeSo === undefined || HeSo === null || isNaN(HeSo)) {
    return res
      .status(400)
      .json({ error: "Dữ liệu không hợp lệ. Vui lòng nhập tên và hệ số." });
  }

  if (HeSo <= 0) {
    return res.status(400).json({ error: "Hệ số phải lớn hơn 0." });
  }

  try {
    // Cập nhật cả TenLoaiKiemTra và HeSo
    const [result] = await db.query(
      "UPDATE loaihinhkiemtra SET TenLoaiKiemTra = ?, HeSo = ? WHERE MaLoaiKiemTra = ? AND TrangThai = 1",
      [TenLoaiKiemTra, HeSo, MaLoaiKiemTra]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({
        error: "Không tìm thấy loại kiểm tra hoặc loại này đã bị xóa.",
      });
    }

    res.json({
      message: "Cập nhật thành công!",
      MaLoaiKiemTra: MaLoaiKiemTra,
      TenMoi: TenLoaiKiemTra,
      HeSoMoi: HeSo,
    });
  } catch (err) {
    console.error("Lỗi cập nhật loại kiểm tra:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi cập nhật dữ liệu." });
  }
};
