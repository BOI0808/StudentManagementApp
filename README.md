# StudentManagementApp

**StudentManagementApp** là một ứng dụng quản lý học sinh, bao gồm **Frontend** (ứng dụng Android) và **Backend** (Node.js/Express) kết nối cơ sở dữ liệu MySQL. Ứng dụng hỗ trợ các chức năng như tiếp nhận học sinh, lập danh sách lớp, nhập điểm, tra cứu thông tin và tạo báo cáo tổng kết.

---

## 🗂️ Cấu trúc dự án

```
StudentManagementApp/
├── .gitignore
├── README.md
├── documents/
│   ├── database/
│   │   └── quan_ly_hoc_sinh.sql  # File SQL chứa schema và dữ liệu mẫu
│   ├── icon/
├── backend/
│   ├── server.js                # File khởi chạy server
│   ├── package.json             # Thông tin dependencies của backend
│   ├── .env                     # Biến môi trường (được .gitignore)
│   ├── config/                  # Cấu hình (DB, môi trường)
│   ├── controllers/             # Xử lý logic nghiệp vụ
│   ├── routes/                  # Định nghĩa các endpoint API
│   ├── middlewares/             # Middleware (xác thực, xử lý lỗi)
│   ├── utils/                   # Các tiện ích (cleanup, mã hóa)
├── android/
│   ├── app/                     # Mã nguồn ứng dụng Android
│   ├── build.gradle.kts         # Cấu hình Gradle
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   ├── local.properties         # Cấu hình local (được .gitignore)
│   ├── .idea/                   # Cấu hình IDE (được .gitignore)
```

---

## 🚀 Chức năng chính

### 1. **Backend (Node.js/Express)**

- **Xác thực người dùng (JWT):**
  - Đăng nhập, đổi mật khẩu, phân quyền theo chức năng.
- **Quản lý học sinh:**
  - Tiếp nhận học sinh mới, tra cứu thông tin, cập nhật, xóa.
  - Import danh sách học sinh từ file Excel.
- **Quản lý lớp học:**
  - Lập danh sách lớp, xếp học sinh vào lớp.
  - Tra cứu danh sách học sinh theo lớp.
- **Quản lý điểm số:**
  - Nhập điểm, tính điểm trung bình, tổng kết học kỳ/năm học.
- **Báo cáo:**
  - Báo cáo tổng kết môn học và học kỳ.
- **Cài đặt hệ thống:**
  - Quản lý tham số như tuổi tối thiểu/tối đa, sĩ số lớp, điểm đạt.

### 2. **Frontend (Android)**

- **Giao diện người dùng:**
  - Tiếp nhận học sinh, lập danh sách lớp, xếp học sinh cho lớp, nhập điểm, tra cứu, tạo báo cáo, tạo năm học, tạo khối lớp, tạo môn học, tạo loại kiểm tra, cài đặt tham số hệ thống.
- **Tích hợp API:**
  - Gọi API từ backend qua Retrofit.
- **Xử lý file Excel:**
  - Xem trước và gửi file Excel lên server.

---

## 🛠️ Công nghệ sử dụng

### **Backend:**

- **Node.js**: Xây dựng server.
- **Express.js**: Framework cho RESTful API.
- **MySQL**: Cơ sở dữ liệu.
- **jsonwebtoken**: Xác thực người dùng.
- **bcrypt**: Mã hóa mật khẩu.
- **multer**: Xử lý upload file.
- **xlsx**: Đọc và ghi file Excel.

### **Frontend:**

- **Android (Java)**: Ứng dụng Android Native.
- **Retrofit**: Gọi API.
- **Material Design**: Giao diện người dùng.

---

## 🗃️ Cơ sở dữ liệu

### **Schema chính:**

- **`hocsinh`**: Thông tin học sinh.
- **`lop`**: Thông tin lớp học.
- **`bangdiem`**: Điểm số học sinh.
- **`nguoidung`**: Tài khoản người dùng.
- **`thamso`**: Tham số hệ thống.

### **File SQL:**

- File [`quan_ly_hoc_sinh.sql`](documents/database/quan_ly_hoc_sinh.sql) chứa toàn bộ schema và dữ liệu mẫu.

---

## 🔧 Cài đặt và chạy dự án

### 1. **Cài đặt Backend**

```bash
# Di chuyển vào thư mục backend
cd backend

# Cài đặt dependencies
npm install

# Tạo file .env và cấu hình biến môi trường
cp .env.example .env

# Chạy server
npm start
```

### 2. **Cài đặt Frontend**

```bash
# Mở thư mục android trong Android Studio
cd android

# Đồng bộ Gradle và chạy ứng dụng trên thiết bị Android
```

---

## 📄 Biến môi trường (Backend)

Tạo file `.env` trong thư mục `backend` với nội dung:

```
DB_HOST=localhost
DB_USER=root
DB_PASS=
DB_NAME=quan_ly_hoc_sinh
DB_CONN_LIMIT=10
PORT=3000
JWT_SECRET=your_jwt_secret
JWT_REFRESH_SECRET=your_refresh_secret
```

---

## 📂 Các endpoint API chính

### **Xác thực:**

- `POST /api/auths/dang-nhap`: Đăng nhập.
- `POST /api/auths/doi-mat-khau`: Đổi mật khẩu.

### **Học sinh:**

- `POST /api/students/tiep-nhan-hoc-sinh`: Tiếp nhận học sinh.
- `GET /api/students/search`: Tra cứu học sinh.

### **Lớp học:**

- `POST /api/classes/lap-danh-sach-lop`: Lập danh sách lớp.
- `GET /api/classes/danh-sach-lop`: Lấy danh sách lớp.

### **Điểm số:**

- `POST /api/grades/nhap-diem`: Nhập điểm.
- `GET /api/grades/nhap-diem/danh-sach`: Lấy danh sách học sinh nhập điểm.

### **Báo cáo:**

- `GET /api/reports/bao-cao-mon`: Báo cáo tổng kết môn.
- `GET /api/reports/bao-cao-hoc-ky`: Báo cáo tổng kết học kỳ.

---

## 📋 Ghi chú

- **File `.env` và các file nhạy cảm khác đã được thêm vào `.gitignore`.**
- **Ứng dụng hỗ trợ import file Excel để nhập danh sách học sinh và điểm số.**
