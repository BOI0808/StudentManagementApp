package com.example.studentmanagementapp.model;

import com.google.gson.annotations.SerializedName;

public class Block {
    @SerializedName("makhoilop")
    private String maKhoiLop;
    
    @SerializedName("tenkhoilop")
    private String tenKhoiLop;

    @SerializedName("TenKhoiLop")
    private String tenKhoiLopInput; // Dùng cho POST

    @SerializedName("TrangThai")
    private Integer trangThai;

    public String getMaKhoiLop() { return maKhoiLop; }
    public String getTenKhoiLop() { return tenKhoiLop; }
    public void setTenKhoiLopInput(String ten) { this.tenKhoiLopInput = ten; }
    public void setTrangThai(Integer status) { this.trangThai = status; }
}
