const db = require("../config/db");

const generateMaLoaiKT = (ten) => {
  let str = ten
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase();

  str = str.replace(/PHUT/g, "P").replace(/TIET/g, "T");

  const words = str.split(/\s+/);
  const result = words
    .map((word) => {
      const numbers = word.match(/\d+/g);
      if (numbers) return numbers.join("");
      return word.charAt(0);
    })
    .join("");

  return ("KT" + result).slice(0, 10);
};

exports.getAllActiveLoaiKT = async (req, res) => {
  try {
    const [rows] = await db.query(
      "SELECT MaLoaiKiemTra, TenLoaiKiemTra, HeSo FROM loaihinhkiemtra WHERE TrangThai = 1"
    );
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Lỗi khi lấy danh sách loại kiểm tra." });
  }
};

exports.createLoaiKT = async (req, res) => {
  const { TenLoaiKiemTra, HeSo } = req.body;

  if (!TenLoaiKiemTra || !HeSo) {
    return res.status(400).json({ error: "Vui lòng nhập tên và hệ số." });
  }

  try {
    const [existing] = await db.query(
      "SELECT MaLoaiKiemTra, TrangThai FROM loaihinhkiemtra WHERE TenLoaiKiemTra = ?",
      [TenLoaiKiemTra]
    );

    if (existing.length > 0) {
      const item = existing[0];

      if (item.TrangThai === 0) {
        await db.query(
          "UPDATE loaihinhkiemtra SET TrangThai = 1, HeSo = ? WHERE MaLoaiKiemTra = ?",
          [HeSo, item.MaLoaiKiemTra]
        );
        return res.json({
          message: "Thêm loại kiểm tra mới thành công!",
          MaLoaiKiemTra: item.MaLoaiKiemTra,
        });
      } else {
        return res.status(400).json({
          error: "Loại kiểm tra này đã tồn tại và đang hoạt động.",
        });
      }
    }

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
    if (err.code === "ER_DUP_ENTRY") {
      return res.status(400).json({
        error: "Mã viết tắt bị trùng, vui lòng đặt tên khác một chút.",
      });
    }
    res.status(500).json({ error: "Lỗi hệ thống khi xử lý loại kiểm tra." });
  }
};

exports.softDeleteLoaiKT = async (req, res) => {
  const { MaLoaiKiemTra } = req.params;

  try {
    await db.query(
      "UPDATE loaihinhkiemtra SET TrangThai = 0 WHERE MaLoaiKiemTra = ?",
      [MaLoaiKiemTra]
    );
    res.json({ message: "Đã xóa loại hình kiểm tra này." });
  } catch (err) {
    res.status(500).json({ error: "Lỗi khi cập nhật trạng thái xóa." });
  }
};

exports.updateHeSoLoaiKT = async (req, res) => {
  const { MaLoaiKiemTra } = req.params;
  const { HeSo } = req.body;
  if (HeSo === undefined || HeSo === null || isNaN(HeSo)) {
    return res
      .status(400)
      .json({ error: "Dữ liệu không hợp lệ. Vui lòng nhập hệ số." });
  }

  if (HeSo <= 0) {
    return res.status(400).json({ error: "Hệ số phải lớn hơn 0." });
  }

  try {
    const [result] = await db.query(
      "UPDATE loaihinhkiemtra SET HeSo = ? WHERE MaLoaiKiemTra = ? AND TrangThai = 1",
      [HeSo, MaLoaiKiemTra]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({
        error: "Không tìm thấy loại kiểm tra hoặc loại này đã bị xóa.",
      });
    }

    res.json({
      message: "Cập nhật thành công!",
      MaLoaiKiemTra: MaLoaiKiemTra,
      HeSoMoi: HeSo,
    });
  } catch (err) {
    console.error("Lỗi cập nhật loại kiểm tra:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi cập nhật dữ liệu." });
  }
};
