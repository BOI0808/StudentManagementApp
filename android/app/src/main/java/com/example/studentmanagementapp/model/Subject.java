package com.example.studentmanagementapp.model;

import com.google.gson.annotations.SerializedName;

public class Subject {
    @SerializedName("mamonhoc")
    private String maMonHoc;
    
    @SerializedName("tenmonhoc")
    private String tenMonHoc;

    @SerializedName("TenMonHoc")
    private String tenMonHocInput; // Dùng cho POST

    public String getMaMonHoc() { return maMonHoc; }
    public String getTenMonHoc() { return tenMonHoc; }
    public void setTenMonHocInput(String ten) { this.tenMonHocInput = ten; }
}
