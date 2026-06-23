const dotenv = require("dotenv");
const path = require("path");

// Load .env file from backend directory
const envPath = path.join(__dirname, "../.env");
dotenv.config({ path: envPath });

// Verify critical environment variables
if (!process.env.JWT_SECRET) {
  console.warn("⚠️ Cảnh báo: Thiếu biến môi trường JWT_SECRET");
}

if (!process.env.DB_HOST) {
  console.warn("⚠️ Cảnh báo: Thiếu biến môi trường DB_HOST");
}
