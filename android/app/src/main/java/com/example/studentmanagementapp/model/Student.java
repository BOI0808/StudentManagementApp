package com.example.studentmanagementapp.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Student implements Serializable {
    @SerializedName(value = "MaHocSinh", alternate = {"maHocSinh", "ma_hoc_sinh", "mahocsinh", "MAHOCSINH", "Ma_Hoc_Sinh"})
    private String maHocSinh;
    
    @SerializedName(value = "HoTen", alternate = {"hoTen", "ho_ten", "hoten", "HOTEN", "Ho_Ten"})
    private String hoTen;
    
    @SerializedName(value = "NgaySinh", alternate = {"ngaySinh", "ngay_sinh", "ngaysinh", "NGAYSINH", "Ngay_Sinh"})
    private String ngaySinh;
    
    @SerializedName(value = "MaGioiTinh", alternate = {"maGioiTinh", "ma_gioi_tinh", "magioitinh", "MAGIOITINH", "Ma_Gioi_Tinh"})
    private String maGioiTinh;
    
    @SerializedName(value = "DiaChi", alternate = {"diaChi", "dia_chi", "diachi", "DIACHI", "Dia_Chi"})
    private String diaChi;
    
    @SerializedName(value = "Email", alternate = {"email", "EMAIL", "e_mail", "E_Mail"})
    private String email;

    // Getters and Setters
    public String getMaHocSinh() { return maHocSinh; }
    public void setMaHocSinh(String maHocSinh) { this.maHocSinh = maHocSinh; }
    
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    
    public String getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(String ngaySinh) { this.ngaySinh = ngaySinh; }
    
    public String getMaGioiTinh() { return maGioiTinh; }
    public void setMaGioiTinh(String maGioiTinh) { this.maGioiTinh = maGioiTinh; }
    
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return (hoTen != null ? hoTen : "") + " (" + (maHocSinh != null ? maHocSinh : "") + ")";
    }
}
