const rateLimit = require("express-rate-limit");

const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5,
  message: { error: "Quá nhiều lần đăng nhập thất bại. Thử lại sau 15 phút." },
  legacyHeaders: false,
  skipSuccessfulRequests: true,
});

const apiLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 100,
  message: { error: "Quá nhiều request. Vui lòng thử lại sau." },
});

module.exports = { loginLimiter, apiLimiter };
