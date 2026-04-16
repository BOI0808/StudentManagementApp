package com.example.studentmanagementapp.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class ClassModel implements Serializable {
    // Dùng cho việc nhận dữ liệu từ GET API
    @SerializedName("maLop")
    private String maLop;
    
    @SerializedName("tenLop")
    private String tenLop;
    
    @SerializedName("hienThi")
    private String hienThi;

    // Dùng cho việc gửi dữ liệu lên POST API (Khớp với Backend destructuring)
    // Và nhận dữ liệu từ GET API (sử dụng alternate để khớp với maHocKyNamHoc từ backend)
    @SerializedName(value = "MaLop", alternate = {"maLop_post"})
    private String maLopPost;

    @SerializedName("TenLop")
    private String tenLopPost;

    @SerializedName("MaKhoiLop")
    private String maKhoiLopPost;

    @SerializedName(value = "MaHocKyNamHoc", alternate = {"maHocKyNamHoc"})
    private String maHocKyNamHocPost;

    @SerializedName("LoaiHocKy")
    private Integer loaiHocKyPost;

    @SerializedName("DanhSachMaHS")
    private List<String> danhSachMaHS;

    public String getMaLop() { return maLop != null ? maLop : maLopPost; }
    public void setMaLop(String maLop) { 
        this.maLopPost = maLop; 
        this.maLop = maLop; 
    }
    
    public String getTenLop() { return tenLop != null ? tenLop : tenLopPost; }
    public void setTenLop(String tenLop) { 
        this.tenLopPost = tenLop; 
        this.tenLop = tenLop; 
    }

    public String getMaKhoiLop() { return maKhoiLopPost; }
    public void setMaKhoiLop(String maKhoiLop) { this.maKhoiLopPost = maKhoiLop; }

    public String getMaHocKyNamHoc() { return maHocKyNamHocPost; }
    public void setMaHocKyNamHoc(String maHocKyNamHoc) { this.maHocKyNamHocPost = maHocKyNamHoc; }

    public Integer getLoaiHocKy() { return loaiHocKyPost; }
    public void setLoaiHocKy(Integer loaiHocKy) { this.loaiHocKyPost = loaiHocKy; }
    
    public List<String> getDanhSachMaHS() { return danhSachMaHS; }
    public void setDanhSachMaHS(List<String> danhSachMaHS) { this.danhSachMaHS = danhSachMaHS; }

    @Override
    public String toString() {
        return (tenLop != null && !tenLop.isEmpty()) ? tenLop : (hienThi != null ? hienThi : maLop);
    }
}
