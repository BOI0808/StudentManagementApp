const db = require("../config/db");

// 1. Lấy toàn bộ tham số hiện tại để Giang đổ vào các ô nhập
exports.getThamSo = async (req, res) => {
  try {
    const [rows] = await db.query("SELECT ten_tham_so, gia_tri FROM thamso");

    // Biến mảng thành một Object duy nhất để Giang (Android) dễ bóc tách dữ liệu
    const settings = rows.reduce((obj, item) => {
      obj[item.ten_tham_so] = item.gia_tri;
      return obj;
    }, {});

    res.json(settings);
  } catch (err) {
    res.status(500).json({ error: "Lỗi khi tải quy định hệ thống." });
  }
};

// 2. Cập nhật đồng thời tất cả các quy định
exports.updateThamSo = async (req, res) => {
  const {
    TuoiToiThieu,
    TuoiToiDa,
    SiSoToiThieu,
    SiSoToiDa,
    DiemDatMon,
    DiemDat,
    DiemToiThieu,
    DiemToiDa,
  } = req.body;

  // Kiểm tra logic cơ bản trước khi lưu
  if (
    TuoiToiThieu >= TuoiToiDa ||
    SiSoToiThieu >= SiSoToiDa ||
    DiemToiThieu >= DiemToiDa
  ) {
    return res
      .status(400)
      .json({ error: "Giá trị tối thiểu phải nhỏ hơn giá trị tối đa!" });
  }

  try {
    // Sử dụng CASE WHEN để cập nhật nhiều dòng chỉ với 1 câu query (tối ưu hiệu năng)
    const query = `
      UPDATE thamso
      SET gia_tri = CASE ten_tham_so
        WHEN 'TuoiToiThieu' THEN ?
        WHEN 'TuoiToiDa' THEN ?
        WHEN 'SiSoToiThieu' THEN ?
        WHEN 'SiSoToiDa' THEN ?
        WHEN 'DiemDatMon' THEN ?
        WHEN 'DiemDat' THEN ?
        WHEN 'DiemToiThieu' THEN ?
        WHEN 'DiemToiDa' THEN ?
        ELSE gia_tri
      END
      WHERE ten_tham_so IN ('TuoiToiThieu', 'TuoiToiDa', 'SiSoToiThieu', 'SiSoToiDa', 'DiemDatMon', 'DiemDat', 'DiemToiThieu', 'DiemToiDa')
    `;

    await db.query(query, [
      TuoiToiThieu,
      TuoiToiDa,
      SiSoToiThieu,
      SiSoToiDa,
      DiemDatMon,
      DiemDat,
      DiemToiThieu,
      DiemToiDa,
    ]);

    res.json({ message: "Đã cập nhật quy định hệ thống thành công!" });
  } catch (err) {
    console.error("Lỗi cập nhật tham số:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi lưu quy định." });
  }
};
