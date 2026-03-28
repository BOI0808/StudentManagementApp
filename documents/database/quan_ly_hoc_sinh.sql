-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Mar 28, 2026 at 07:48 AM
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
CREATE DATABASE IF NOT EXISTS `quan_ly_hoc_sinh` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `quan_ly_hoc_sinh`;

-- --------------------------------------------------------

--
-- Table structure for table `bangdiem`
--

DROP TABLE IF EXISTS `bangdiem`;
CREATE TABLE `bangdiem` (
  `MaBangDiem` varchar(10) NOT NULL,
  `MaLop` varchar(10) DEFAULT NULL,
  `MaMonHoc` varchar(10) DEFAULT NULL,
  `MaLoaiKiemTra` varchar(10) DEFAULT NULL,
  `MaHocSinh` varchar(10) DEFAULT NULL,
  `Diem` float DEFAULT NULL
) ;

--
-- RELATIONSHIPS FOR TABLE `bangdiem`:
--   `MaLop`
--       `lop` -> `MaLop`
--   `MaMonHoc`
--       `monhoc` -> `MaMonHoc`
--   `MaLoaiKiemTra`
--       `loaihinhkiemtra` -> `MaLoaiKiemTra`
--   `MaHocSinh`
--       `hocsinh` -> `MaHocSinh`
--

-- --------------------------------------------------------

--
-- Table structure for table `chitietlop`
--

DROP TABLE IF EXISTS `chitietlop`;
CREATE TABLE `chitietlop` (
  `MaLop` varchar(10) NOT NULL,
  `MaHocSinh` varchar(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- RELATIONSHIPS FOR TABLE `chitietlop`:
--   `MaLop`
--       `lop` -> `MaLop`
--   `MaHocSinh`
--       `hocsinh` -> `MaHocSinh`
--

-- --------------------------------------------------------

--
-- Table structure for table `gioitinh`
--

DROP TABLE IF EXISTS `gioitinh`;
CREATE TABLE `gioitinh` (
  `MaGioiTinh` varchar(10) NOT NULL,
  `TenGioiTinh` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- RELATIONSHIPS FOR TABLE `gioitinh`:
--

-- --------------------------------------------------------

--
-- Table structure for table `hocky_namhoc`
--

DROP TABLE IF EXISTS `hocky_namhoc`;
CREATE TABLE `hocky_namhoc` (
  `MaHocKyNamHoc` varchar(10) NOT NULL,
  `TenNamHoc` varchar(100) DEFAULT NULL,
  `TenHocKy` varchar(100) DEFAULT NULL,
  `NgayBatDau` date DEFAULT NULL,
  `NgayKetThuc` date DEFAULT NULL,
  `TrangThai` varchar(100) DEFAULT NULL
) ;

--
-- RELATIONSHIPS FOR TABLE `hocky_namhoc`:
--

-- --------------------------------------------------------

--
-- Table structure for table `hocsinh`
--

DROP TABLE IF EXISTS `hocsinh`;
CREATE TABLE `hocsinh` (
  `MaHocSinh` varchar(10) NOT NULL,
  `HoTen` varchar(100) DEFAULT NULL,
  `NgaySinh` date DEFAULT NULL,
  `MaGioiTinh` varchar(10) DEFAULT NULL,
  `DiaChi` varchar(100) DEFAULT NULL,
  `Email` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- RELATIONSHIPS FOR TABLE `hocsinh`:
--   `MaGioiTinh`
--       `gioitinh` -> `MaGioiTinh`
--

-- --------------------------------------------------------

--
-- Table structure for table `khoilop`
--

DROP TABLE IF EXISTS `khoilop`;
CREATE TABLE `khoilop` (
  `MaKhoiLop` varchar(10) NOT NULL,
  `TenKhoiLop` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- RELATIONSHIPS FOR TABLE `khoilop`:
--

-- --------------------------------------------------------

--
-- Table structure for table `loaihinhkiemtra`
--

DROP TABLE IF EXISTS `loaihinhkiemtra`;
CREATE TABLE `loaihinhkiemtra` (
  `MaLoaiKiemTra` varchar(10) NOT NULL,
  `TenLoaiKiemTra` varchar(100) DEFAULT NULL,
  `HeSo` int(11) DEFAULT NULL
) ;

--
-- RELATIONSHIPS FOR TABLE `loaihinhkiemtra`:
--

-- --------------------------------------------------------

--
-- Table structure for table `lop`
--

DROP TABLE IF EXISTS `lop`;
CREATE TABLE `lop` (
  `MaLop` varchar(10) NOT NULL,
  `TenLop` varchar(100) DEFAULT NULL,
  `MaKhoiLop` varchar(10) DEFAULT NULL,
  `MaHocKyNamHoc` varchar(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- RELATIONSHIPS FOR TABLE `lop`:
--   `MaKhoiLop`
--       `khoilop` -> `MaKhoiLop`
--   `MaHocKyNamHoc`
--       `hocky_namhoc` -> `MaHocKyNamHoc`
--

-- --------------------------------------------------------

--
-- Table structure for table `monhoc`
--

DROP TABLE IF EXISTS `monhoc`;
CREATE TABLE `monhoc` (
  `MaMonHoc` varchar(10) NOT NULL,
  `TenMonHoc` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- RELATIONSHIPS FOR TABLE `monhoc`:
--

-- --------------------------------------------------------

--
-- Table structure for table `nguoidung`
--

DROP TABLE IF EXISTS `nguoidung`;
CREATE TABLE `nguoidung` (
  `MaSo` varchar(10) NOT NULL,
  `HoTen` varchar(100) DEFAULT NULL,
  `TenDangNhap` varchar(100) DEFAULT NULL,
  `MatKhau` varchar(100) DEFAULT NULL,
  `Email` varchar(100) DEFAULT NULL,
  `SoDienThoai` varchar(15) DEFAULT NULL,
  `PhanQuyen` varchar(100) DEFAULT NULL
) ;

--
-- RELATIONSHIPS FOR TABLE `nguoidung`:
--

-- --------------------------------------------------------

--
-- Table structure for table `thamso`
--

DROP TABLE IF EXISTS `thamso`;
CREATE TABLE `thamso` (
  `ma_tham_so` int(11) NOT NULL,
  `ten_tham_so` varchar(50) NOT NULL,
  `gia_tri` float NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- RELATIONSHIPS FOR TABLE `thamso`:
--

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
  MODIFY `ma_tham_so` int(11) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `bangdiem`
--
ALTER TABLE `bangdiem`
  ADD CONSTRAINT `bangdiem_ibfk_1` FOREIGN KEY (`MaLop`) REFERENCES `lop` (`MaLop`),
  ADD CONSTRAINT `bangdiem_ibfk_2` FOREIGN KEY (`MaMonHoc`) REFERENCES `monhoc` (`MaMonHoc`),
  ADD CONSTRAINT `bangdiem_ibfk_3` FOREIGN KEY (`MaLoaiKiemTra`) REFERENCES `loaihinhkiemtra` (`MaLoaiKiemTra`),
  ADD CONSTRAINT `bangdiem_ibfk_4` FOREIGN KEY (`MaHocSinh`) REFERENCES `hocsinh` (`MaHocSinh`);

--
-- Constraints for table `chitietlop`
--
ALTER TABLE `chitietlop`
  ADD CONSTRAINT `chitietlop_ibfk_1` FOREIGN KEY (`MaLop`) REFERENCES `lop` (`MaLop`),
  ADD CONSTRAINT `chitietlop_ibfk_2` FOREIGN KEY (`MaHocSinh`) REFERENCES `hocsinh` (`MaHocSinh`);

--
-- Constraints for table `hocsinh`
--
ALTER TABLE `hocsinh`
  ADD CONSTRAINT `hocsinh_ibfk_1` FOREIGN KEY (`MaGioiTinh`) REFERENCES `gioitinh` (`MaGioiTinh`);

--
-- Constraints for table `lop`
--
ALTER TABLE `lop`
  ADD CONSTRAINT `lop_ibfk_1` FOREIGN KEY (`MaKhoiLop`) REFERENCES `khoilop` (`MaKhoiLop`),
  ADD CONSTRAINT `lop_ibfk_2` FOREIGN KEY (`MaHocKyNamHoc`) REFERENCES `hocky_namhoc` (`MaHocKyNamHoc`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
