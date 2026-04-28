package com.example.studentmanagementapp.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class ClassModel implements Serializable {
    // Dữ liệu sạch từ API tìm kiếm mới
    @SerializedName("maLop")
    private String maLop;
    
    @SerializedName("tenLop")
    private String tenLop;
    
    @SerializedName("namHoc")
    private String namHoc;
    
    @SerializedName("hienThi")
    private String hienThi;

    @SerializedName(value = "tenHocKy", alternate = {"tenhocky", "TenHocKy", "hocky", "HocKy"})
    private String tenHocKy;

    // Dùng cho việc gửi dữ liệu lên POST API hoặc các API cũ
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

    public String getMaLop() { 
        if (maLop != null) return maLop;
        return (maLopPost != null) ? maLopPost : "";
    }
    
    public void setMaLop(String maLop) { 
        this.maLopPost = maLop; 
        this.maLop = maLop; 
    }
    
    public String getTenLop() { 
        // Ưu tiên trả về tenLop sạch
        if (tenLop != null && !tenLop.isEmpty()) return tenLop;
        if (tenLopPost != null && !tenLopPost.isEmpty()) return tenLopPost;
        // Fallback về hienThi nếu không có tên riêng
        return (hienThi != null) ? hienThi : "";
    }
    
    public void setTenLop(String tenLop) { 
        this.tenLopPost = tenLop; 
        this.tenLop = tenLop; 
    }

    public String getNamHoc() { 
        // Ưu tiên trả về namHoc sạch từ API
        if (namHoc != null && !namHoc.isEmpty()) return namHoc;
        
        // Fallback parse từ hienThi nếu trường hợp API cũ không có namHoc riêng
        if (hienThi != null && hienThi.contains(" - ")) {
            try {
                String[] parts = hienThi.split(" - ");
                if (parts.length > 1) {
                    return parts[parts.length - 1].replace(")", "").trim();
                }
            } catch (Exception ignored) {}
        }
        return "Chưa có"; 
    }
    
    public void setNamHoc(String namHoc) { this.namHoc = namHoc; }

    public String getTenHocKy() { 
        if (tenHocKy != null) return tenHocKy;
        if (hienThi != null) {
            if (hienThi.contains("HK1")) return "Học kỳ 1";
            if (hienThi.contains("HK2")) return "Học kỳ 2";
        }
        return "Chưa có"; 
    }
    
    public void setTenHocKy(String tenHocKy) { this.tenHocKy = tenHocKy; }

    public String getHienThi() { return hienThi; }
    public void setHienThi(String hienThi) { this.hienThi = hienThi; }

    public String getMaKhoiLop() { return maKhoiLopPost; }
    public void setMaKhoiLop(String maKhoiLop) { this.maKhoiLopPost = maKhoiLop; }

    public String getMaHocKyNamHoc() { return maHocKyNamHocPost; }
    public void setMaHocKyNamHoc(String maHocKy) { this.maHocKyNamHocPost = maHocKy; }

    public Integer getLoaiHocKy() { return loaiHocKyPost; }
    public void setLoaiHocKy(Integer loaiHocKy) { this.loaiHocKyPost = loaiHocKy; }
    
    public List<String> getDanhSachMaHS() { return danhSachMaHS; }
    public void setDanhSachMaHS(List<String> danhSachMaHS) { this.danhSachMaHS = danhSachMaHS; }

    @Override
    public String toString() {
        // Trả về tenLop để AutoCompleteTextView hiển thị gọn gàng sau khi chọn
        return getTenLop();
    }
}
