-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Apr 07, 2026 at 06:45 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `quan_ly_hoc_sinh`
--

-- --------------------------------------------------------

--
-- Table structure for table `bangdiem`
--

CREATE TABLE `bangdiem` (
  `MaBangDiem` varchar(10) NOT NULL,
  `MaLop` varchar(15) NOT NULL,
  `MaMonHoc` varchar(10) DEFAULT NULL,
  `MaLoaiKiemTra` varchar(10) DEFAULT NULL,
  `MaHocSinh` varchar(10) DEFAULT NULL,
  `Diem` float DEFAULT NULL,
  `GhiChu` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `bangdiem`
--

INSERT INTO `bangdiem` (`MaBangDiem`, `MaLop`, `MaMonHoc`, `MaLoaiKiemTra`, `MaHocSinh`, `Diem`, `GhiChu`) VALUES
('BD74637600', '10A1HK1-2627', 'TOANHOC', 'KT15P', 'HS260001', 9.5, 'Giỏi'),
('BD76199500', '10A1HK2-2627', 'TOANHOC', 'KT15P', 'HS260001', 9, 'Giỏi'),
('BD76200101', '10A1HK2-2627', 'TOANHOC', 'KT15P', 'HS260002', 3, ''),
('BD76413101', '10A1HK1-2627', 'TOANHOC', 'KT15P', 'HS260002', 4, '');

-- --------------------------------------------------------

--
-- Table structure for table `chitietlop`
--

CREATE TABLE `chitietlop` (
  `MaLop` varchar(15) NOT NULL,
  `MaHocSinh` varchar(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `chitietlop`
--

INSERT INTO `chitietlop` (`MaLop`, `MaHocSinh`) VALUES
('10A1HK1-2627', 'HS260001'),
('10A1HK1-2627', 'HS260002');

-- --------------------------------------------------------

--
-- Table structure for table `chucnang`
--

CREATE TABLE `chucnang` (
  `MaCN` varchar(10) NOT NULL,
  `TenCN` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `chucnang`
--

INSERT INTO `chucnang` (`MaCN`, `TenCN`) VALUES
('CNCDTSHT', 'Cài đặt tham số hệ thống'),
('CNLBCTKHK', 'Lập báo cáo tổng kết học kỳ'),
('CNLBCTKM', 'Lập báo cáo tổng kết môn'),
('CNLDSHSCL', 'Lập danh sách học sinh cho lớp'),
('CNLDSKL', 'Lập danh sách khối lớp'),
('CNLDSL', 'Lập danh sách lớp'),
('CNLDSMH', 'Lập danh sách môn học'),
('CNLDSNH', 'Lập danh sách năm học'),
('CNNBD', 'Nhập bảng điểm'),
('CNNDSCLKT', 'Nhập danh sách các loại kiểm tra'),
('CNQLND', 'Quản lý người dùng'),
('CNTCHS', 'Tra cứu học sinh'),
('CNTNHS', 'Tiếp nhận học sinh');

-- --------------------------------------------------------

--
-- Table structure for table `gioitinh`
--

CREATE TABLE `gioitinh` (
  `MaGioiTinh` varchar(10) NOT NULL,
  `TenGioiTinh` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `gioitinh`
--

INSERT INTO `gioitinh` (`MaGioiTinh`, `TenGioiTinh`) VALUES
('GT1', 'Nam'),
('GT2', 'Nữ'),
('GT3', 'Khác');

-- --------------------------------------------------------

--
-- Table structure for table `hocky_namhoc`
--

CREATE TABLE `hocky_namhoc` (
  `MaHocKyNamHoc` varchar(10) NOT NULL,
  `NamHocBatDau` int(11) DEFAULT NULL,
  `NamHocKetThuc` int(11) DEFAULT NULL,
  `TenHocKy` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `hocky_namhoc`
--

INSERT INTO `hocky_namhoc` (`MaHocKyNamHoc`, `NamHocBatDau`, `NamHocKetThuc`, `TenHocKy`) VALUES
('HK1-2526', 2025, 2026, 'Học kỳ 1'),
('HK1-2627', 2026, 2027, 'Học kỳ 1'),
('HK2-2526', 2025, 2026, 'Học kỳ 2'),
('HK2-2627', 2026, 2027, 'Học kỳ 2');

-- --------------------------------------------------------

--
-- Table structure for table `hocsinh`
--

CREATE TABLE `hocsinh` (
  `MaHocSinh` varchar(10) NOT NULL,
  `HoTen` varchar(100) DEFAULT NULL,
  `NgaySinh` date DEFAULT NULL,
  `MaGioiTinh` varchar(10) DEFAULT NULL,
  `DiaChi` varchar(100) DEFAULT NULL,
  `Email` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `hocsinh`
--

INSERT INTO `hocsinh` (`MaHocSinh`, `HoTen`, `NgaySinh`, `MaGioiTinh`, `DiaChi`, `Email`) VALUES
('HS260001', 'Đinh Nhật Khôi', '2009-08-15', 'GT1', 'Thành phố Thủ Đức, TP.HCM', 'nhatkhoi.pro@gmail.com'),
('HS260002', 'Nguyễn Văn A', '2010-05-20', 'GT1', 'Quận Thủ Đức, TP.HCM', 'nguyenvana@gmail.com');

-- --------------------------------------------------------

--
-- Table structure for table `ketqua_monhoc`
--

CREATE TABLE `ketqua_monhoc` (
  `MaHocSinh` varchar(10) NOT NULL,
  `MaMonHoc` varchar(10) NOT NULL,
  `MaHocKyNamHoc` varchar(10) NOT NULL,
  `DiemTrungBinhMon` float DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `ketqua_monhoc`
--

INSERT INTO `ketqua_monhoc` (`MaHocSinh`, `MaMonHoc`, `MaHocKyNamHoc`, `DiemTrungBinhMon`) VALUES
('HS260001', 'TOANHOC', 'HK1-2627', 9.5),
('HS260001', 'TOANHOC', 'HK2-2627', 9),
('HS260002', 'TOANHOC', 'HK1-2627', 4),
('HS260002', 'TOANHOC', 'HK2-2627', 3);

-- --------------------------------------------------------

--
-- Table structure for table `khoilop`
--

CREATE TABLE `khoilop` (
  `MaKhoiLop` varchar(10) NOT NULL,
  `TenKhoiLop` varchar(100) DEFAULT NULL,
  `TrangThai` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `khoilop`
--

INSERT INTO `khoilop` (`MaKhoiLop`, `TenKhoiLop`, `TrangThai`) VALUES
('K10', '10', 1),
('K11', '11', 1);

-- --------------------------------------------------------

--
-- Table structure for table `loaihinhkiemtra`
--

CREATE TABLE `loaihinhkiemtra` (
  `MaLoaiKiemTra` varchar(10) NOT NULL,
  `TenLoaiKiemTra` varchar(100) DEFAULT NULL,
  `HeSo` int(11) DEFAULT NULL,
  `TrangThai` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `loaihinhkiemtra`
--

INSERT INTO `loaihinhkiemtra` (`MaLoaiKiemTra`, `TenLoaiKiemTra`, `HeSo`, `TrangThai`) VALUES
('KT15P', '15 phút', 1, 1),
('KT1T', '1 tiết', 2, 1),
('KTCK', 'Cuối kỳ', 3, 1);

-- --------------------------------------------------------

--
-- Table structure for table `lop`
--

CREATE TABLE `lop` (
  `MaLop` varchar(15) NOT NULL,
  `TenLop` varchar(100) DEFAULT NULL,
  `MaKhoiLop` varchar(10) DEFAULT NULL,
  `MaHocKyNamHoc` varchar(10) DEFAULT NULL,
  `SiSo` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `lop`
--

INSERT INTO `lop` (`MaLop`, `TenLop`, `MaKhoiLop`, `MaHocKyNamHoc`, `SiSo`) VALUES
('10A1HK1-2526', '10A1', 'K10', 'HK1-2526', 0),
('10A1HK1-2627', '10A1', 'K10', 'HK1-2627', 2),
('10A1HK2-2526', '10A1', 'K10', 'HK2-2526', 0),
('10A1HK2-2627', '10A1', 'K10', 'HK2-2627', 0);

-- --------------------------------------------------------

--
-- Table structure for table `monhoc`
--

CREATE TABLE `monhoc` (
  `MaMonHoc` varchar(10) NOT NULL,
  `TenMonHoc` varchar(100) DEFAULT NULL,
  `TrangThai` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `monhoc`
--

INSERT INTO `monhoc` (`MaMonHoc`, `TenMonHoc`, `TrangThai`) VALUES
('NGUVAN', 'Ngữ văn', 1),
('TOANHOC', 'Toán học', 1);

-- --------------------------------------------------------

--
-- Table structure for table `nguoidung`
--

CREATE TABLE `nguoidung` (
  `MaSo` varchar(10) NOT NULL,
  `HoTen` varchar(100) DEFAULT NULL,
  `TenDangNhap` varchar(100) DEFAULT NULL,
  `MatKhau` varchar(100) DEFAULT NULL,
  `Email` varchar(100) DEFAULT NULL,
  `SoDienThoai` varchar(15) DEFAULT NULL,
  `PhanQuyen` varchar(100) DEFAULT NULL,
  `TrangThai` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `nguoidung`
--

INSERT INTO `nguoidung` (`MaSo`, `HoTen`, `TenDangNhap`, `MatKhau`, `Email`, `SoDienThoai`, `PhanQuyen`, `TrangThai`) VALUES
('ADMIN01', 'Quản trị viên hệ thống', 'admin', 'admin123', 'admin@school.edu.vn', '0123456789', 'Quản trị viên', 1);

-- --------------------------------------------------------

--
-- Table structure for table `nguoidung_quyen`
--

CREATE TABLE `nguoidung_quyen` (
  `MaSo` varchar(10) NOT NULL,
  `MaCN` varchar(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `nguoidung_quyen`
--

INSERT INTO `nguoidung_quyen` (`MaSo`, `MaCN`) VALUES
('ADMIN01', 'CNCDTSHT'),
('ADMIN01', 'CNLBCTKHK'),
('ADMIN01', 'CNLBCTKM'),
('ADMIN01', 'CNLDSHSCL'),
('ADMIN01', 'CNLDSKL'),
('ADMIN01', 'CNLDSL'),
('ADMIN01', 'CNLDSMH'),
('ADMIN01', 'CNLDSNH'),
('ADMIN01', 'CNNBD'),
('ADMIN01', 'CNNDSCLKT'),
('ADMIN01', 'CNQLND'),
('ADMIN01', 'CNTCHS'),
('ADMIN01', 'CNTNHS');

-- --------------------------------------------------------

--
-- Table structure for table `thamso`
--

CREATE TABLE `thamso` (
  `ma_tham_so` int(11) NOT NULL,
  `ten_tham_so` varchar(50) NOT NULL,
  `gia_tri` float NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `thamso`
--

INSERT INTO `thamso` (`ma_tham_so`, `ten_tham_so`, `gia_tri`) VALUES
(1, 'TuoiToiThieu', 15),
(2, 'TuoiToiDa', 20),
(3, 'SiSoToiDa', 40),
(4, 'DiemDat', 5),
(5, 'DiemDatMon', 5),
(6, 'SiSoToiThieu', 15),
(7, 'DiemToiThieu', 0),
(8, 'DiemToiDa', 10);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `bangdiem`
--
ALTER TABLE `bangdiem`
  ADD PRIMARY KEY (`MaBangDiem`),
  ADD KEY `MaLop` (`MaLop`),
  ADD KEY `MaMonHoc` (`MaMonHoc`),
  ADD KEY `MaLoaiKiemTra` (`MaLoaiKiemTra`),
  ADD KEY `MaHocSinh` (`MaHocSinh`);

--
-- Indexes for table `chitietlop`
--
ALTER TABLE `chitietlop`
  ADD PRIMARY KEY (`MaLop`,`MaHocSinh`),
  ADD KEY `MaHocSinh` (`MaHocSinh`);

--
-- Indexes for table `chucnang`
--
ALTER TABLE `chucnang`
  ADD PRIMARY KEY (`MaCN`);

--
-- Indexes for table `gioitinh`
--
ALTER TABLE `gioitinh`
  ADD PRIMARY KEY (`MaGioiTinh`);

--
-- Indexes for table `hocky_namhoc`
--
ALTER TABLE `hocky_namhoc`
  ADD PRIMARY KEY (`MaHocKyNamHoc`);

--
-- Indexes for table `hocsinh`
--
ALTER TABLE `hocsinh`
  ADD PRIMARY KEY (`MaHocSinh`),
  ADD KEY `MaGioiTinh` (`MaGioiTinh`);

--
-- Indexes for table `ketqua_monhoc`
--
ALTER TABLE `ketqua_monhoc`
  ADD PRIMARY KEY (`MaHocSinh`,`MaMonHoc`,`MaHocKyNamHoc`),
  ADD KEY `MaMonHoc` (`MaMonHoc`);

--
-- Indexes for table `khoilop`
--
ALTER TABLE `khoilop`
  ADD PRIMARY KEY (`MaKhoiLop`);

--
-- Indexes for table `loaihinhkiemtra`
--
ALTER TABLE `loaihinhkiemtra`
  ADD PRIMARY KEY (`MaLoaiKiemTra`),
  ADD UNIQUE KEY `TenLoaiKiemTra` (`TenLoaiKiemTra`);

--
-- Indexes for table `lop`
--
ALTER TABLE `lop`
  ADD PRIMARY KEY (`MaLop`),
  ADD KEY `MaKhoiLop` (`MaKhoiLop`),
  ADD KEY `MaHocKyNamHoc` (`MaHocKyNamHoc`);

--
-- Indexes for table `monhoc`
--
ALTER TABLE `monhoc`
  ADD PRIMARY KEY (`MaMonHoc`);

--
-- Indexes for table `nguoidung`
--
ALTER TABLE `nguoidung`
  ADD PRIMARY KEY (`MaSo`),
  ADD UNIQUE KEY `TenDangNhap` (`TenDangNhap`);

--
-- Indexes for table `nguoidung_quyen`
--
ALTER TABLE `nguoidung_quyen`
  ADD PRIMARY KEY (`MaSo`,`MaCN`),
  ADD KEY `MaCN` (`MaCN`);

--
-- Indexes for table `thamso`
--
ALTER TABLE `thamso`
  ADD PRIMARY KEY (`ma_tham_so`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `thamso`
--
ALTER TABLE `thamso`
  MODIFY `ma_tham_so` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `bangdiem`
--
ALTER TABLE `bangdiem`
  ADD CONSTRAINT `bangdiem_ibfk_2` FOREIGN KEY (`MaMonHoc`) REFERENCES `monhoc` (`MaMonHoc`),
  ADD CONSTRAINT `bangdiem_ibfk_3` FOREIGN KEY (`MaLoaiKiemTra`) REFERENCES `loaihinhkiemtra` (`MaLoaiKiemTra`),
  ADD CONSTRAINT `bangdiem_ibfk_4` FOREIGN KEY (`MaHocSinh`) REFERENCES `hocsinh` (`MaHocSinh`),
  ADD CONSTRAINT `fk_bangdiem_lop` FOREIGN KEY (`MaLop`) REFERENCES `lop` (`MaLop`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `chitietlop`
--
ALTER TABLE `chitietlop`
  ADD CONSTRAINT `chitietlop_ibfk_2` FOREIGN KEY (`MaHocSinh`) REFERENCES `hocsinh` (`MaHocSinh`),
  ADD CONSTRAINT `fk_chitietlop_lop` FOREIGN KEY (`MaLop`) REFERENCES `lop` (`MaLop`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `hocsinh`
--
ALTER TABLE `hocsinh`
  ADD CONSTRAINT `hocsinh_ibfk_1` FOREIGN KEY (`MaGioiTinh`) REFERENCES `gioitinh` (`MaGioiTinh`);

--
-- Constraints for table `ketqua_monhoc`
--
ALTER TABLE `ketqua_monhoc`
  ADD CONSTRAINT `ketqua_monhoc_ibfk_1` FOREIGN KEY (`MaHocSinh`) REFERENCES `hocsinh` (`MaHocSinh`),
  ADD CONSTRAINT `ketqua_monhoc_ibfk_2` FOREIGN KEY (`MaMonHoc`) REFERENCES `monhoc` (`MaMonHoc`);

--
-- Constraints for table `lop`
--
ALTER TABLE `lop`
  ADD CONSTRAINT `lop_ibfk_1` FOREIGN KEY (`MaKhoiLop`) REFERENCES `khoilop` (`MaKhoiLop`),
  ADD CONSTRAINT `lop_ibfk_2` FOREIGN KEY (`MaHocKyNamHoc`) REFERENCES `hocky_namhoc` (`MaHocKyNamHoc`);

--
-- Constraints for table `nguoidung_quyen`
--
ALTER TABLE `nguoidung_quyen`
  ADD CONSTRAINT `nguoidung_quyen_ibfk_1` FOREIGN KEY (`MaSo`) REFERENCES `nguoidung` (`MaSo`),
  ADD CONSTRAINT `nguoidung_quyen_ibfk_2` FOREIGN KEY (`MaCN`) REFERENCES `chucnang` (`MaCN`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
