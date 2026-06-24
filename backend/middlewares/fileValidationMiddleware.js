const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

const fileValidation = (req, res, next) => {
  if (req.file) {
    if (req.file.size > MAX_FILE_SIZE) {
      return res.status(400).json({
        error: `File quá lớn (max ${MAX_FILE_SIZE / 1024 / 1024}MB)`,
      });
    }

    const allowedMimes = [
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "application/vnd.ms-excel",
    ];
    if (!allowedMimes.includes(req.file.mimetype)) {
      return res.status(400).json({ error: "Chỉ chấp nhận file Excel" });
    }
  }
  next();
};

module.exports = fileValidation;
