package com.example.studentmanagementapp.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class User implements Serializable {
    @SerializedName("MaSo")
    private String maSo;
    
    @SerializedName("HoTen")
    private String hoTen;
    
    @SerializedName("Email")
    private String email;
    
    @SerializedName("SoDienThoai")
    private String soDienThoai;
    
    @SerializedName("TenDangNhap")
    private String tenDangNhap;

    @SerializedName("MatKhau")
    private String matKhau;
    
    @SerializedName("DanhSachQuyen")
    private List<String> danhSachQuyen;

    @SerializedName("QuyenHeThong")
    private String quyenHeThong;

    public User() {}

    public String getMaSo() { return maSo; }
    public void setMaSo(String maSo) { this.maSo = maSo; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }

    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String matKhau) { this.matKhau = matKhau; }

    public List<String> getDanhSachQuyen() { return danhSachQuyen; }
    public void setDanhSachQuyen(List<String> danhSachQuyen) { this.danhSachQuyen = danhSachQuyen; }

    public String getQuyenHeThong() { return quyenHeThong; }
    public void setQuyenHeThong(String quyenHeThong) { this.quyenHeThong = quyenHeThong; }
}
