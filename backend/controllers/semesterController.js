const db = require("../config/db");

exports.createHocKyNamHoc = async (req, res) => {
  const { NamHocBatDau, NamHocKetThuc, HocKy } = req.body;

  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    if (!NamHocBatDau || !NamHocKetThuc || !HocKy) {
      return res
        .status(400)
        .json({ error: "Vui lòng nhập đầy đủ Năm học và chọn Học kỳ." });
    }

    if (parseInt(NamHocBatDau) >= parseInt(NamHocKetThuc)) {
      return res
        .status(400)
        .json({ error: "Năm bắt đầu phải nhỏ hơn năm kết thúc." });
    }

    if (parseInt(NamHocKetThuc) - parseInt(NamHocBatDau) >= 2) {
      return res.status(400).json({ error: "Năm học không hợp lệ." });
    }

    let listSemesters = [];
    if (HocKy === 1) listSemesters = [1];
    else if (HocKy === 2) listSemesters = [2];
    else if (HocKy === 3) listSemesters = [1, 2];
    else
      return res.status(400).json({ error: "Lựa chọn học kỳ không hợp lệ." });

    const shortYear = `${NamHocBatDau.toString().slice(
      -2
    )}${NamHocKetThuc.toString().slice(-2)}`;

    for (const sem of listSemesters) {
      const MaHocKyNamHoc = `HK${sem}-${shortYear}`;

      const query = `
        INSERT INTO hocky_namhoc (MaHocKyNamHoc, NamHocBatDau, NamHocKetThuc, TenHocKy) 
        VALUES (?, ?, ?, ?)`;

      await connection.query(query, [
        MaHocKyNamHoc,
        NamHocBatDau,
        NamHocKetThuc,
        `Học kỳ ${sem}`,
      ]);
    }

    await connection.commit();
    res.json({ message: "Lập danh mục học kỳ thành công!" });
  } catch (err) {
    await connection.rollback();
    if (err.code === "ER_DUP_ENTRY") {
      return res
        .status(400)
        .json({ error: "Học kỳ trong niên khóa này đã tồn tại." });
    }
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi tạo học kỳ." });
  } finally {
    connection.release();
  }
};

exports.deleteHocKyNamHoc = async (req, res) => {
  const { MaHocKyNamHoc } = req.params;

  try {
    const [linkedClasses] = await db.query(
      "SELECT MaLop FROM lop WHERE MaHocKyNamHoc = ? LIMIT 1",
      [MaHocKyNamHoc]
    );

    if (linkedClasses.length > 0) {
      return res.status(400).json({
        error: "Không thể xóa học kỳ này vì đã có danh sách lớp học liên quan.",
      });
    }

    const [result] = await db.query(
      "DELETE FROM hocky_namhoc WHERE MaHocKyNamHoc = ?",
      [MaHocKyNamHoc]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ error: "Không tìm thấy học kỳ để xóa." });
    }

    res.json({ message: "Xóa danh mục học kỳ thành công!" });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Lỗi hệ thống khi xóa học kỳ." });
  }
};

exports.getAllHocKyNamHoc = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT MaHocKyNamHoc, TenHocKy, NamHocBatDau, NamHocKetThuc 
       FROM hocky_namhoc 
       ORDER BY NamHocBatDau DESC, TenHocKy ASC`
    );

    const dropdownData = rows.map((item) => ({
      ma: item.MaHocKyNamHoc,
      hocky: `${item.TenHocKy}`,
      namhoc: `${item.NamHocBatDau}-${item.NamHocKetThuc}`,
    }));

    res.json(dropdownData);
  } catch (err) {
    console.error("Lỗi lấy dropdown năm học:", err);
    res.status(500).json({ error: "Lỗi hệ thống khi lấy danh sách năm học." });
  }
};
