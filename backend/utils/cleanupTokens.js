const db = require("../config/db");
const cron = require("node-cron");

const cleanupExpiredTokens = async () => {
  try {
    const now = Math.floor(Date.now() / 1000); // Thời gian hiện tại (Unix timestamp)
    await db.query(
      "DELETE FROM refresh_tokens WHERE UNIX_TIMESTAMP(created_at) + 604800 < ?",
      [now]
    );
    console.log("Đã xóa các Refresh Token hết hạn.");
  } catch (err) {
    console.error("Lỗi khi xóa Refresh Token hết hạn:", err);
  }
};

// Chạy cron job mỗi ngày lúc 00:00
cron.schedule("0 0 * * *", cleanupExpiredTokens);
