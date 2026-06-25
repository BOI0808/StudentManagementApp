package com.example.studentmanagementapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LoginResponse {
    @SerializedName("success")
    private Boolean success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("data")
    private LoginData data;

    @SerializedName("accessToken")
    private String accessToken;
    
    @SerializedName("refreshToken")
    private String refreshToken;
    
    @SerializedName("user")
    private UserData user;

    public boolean isSuccess() { 
        if (success != null) return success;
        return getAccessToken() != null;
    }
    
    public String getMessage() { return message; }
    public LoginData getData() { return data; }

    public String getAccessToken() {
        return (data != null) ? data.getAccessToken() : accessToken;
    }

    public String getRefreshToken() {
        return (data != null) ? data.getRefreshToken() : refreshToken;
    }

    public UserData getUser() {
        return (data != null) ? data.getUser() : user;
    }

    public static class LoginData {
        @SerializedName("accessToken")
        private String accessToken;
        
        @SerializedName("refreshToken")
        private String refreshToken;
        
        @SerializedName("user")
        private UserData user;

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public UserData getUser() { return user; }
    }

    public static class UserData {
        @SerializedName("MaSo")
        private String maSo;
        @SerializedName("HoTen")
        private String hoTen;
        
        @SerializedName("PhanQuyen")
        private String phanQuyen;
        
        @SerializedName("QuyenHeThong")
        private String quyenHeThong;

        @SerializedName("DanhSachQuyen")
        private List<String> danhSachQuyen;

        public String getMaSo() { return maSo; }
        public void setMaSo(String maSo) { this.maSo = maSo; }
        
        public String getHoTen() { return hoTen; }
        public void setHoTen(String hoTen) { this.hoTen = hoTen; }
        
        public String getPhanQuyen() { 
            if (phanQuyen != null && !phanQuyen.isEmpty()) return phanQuyen;
            return quyenHeThong;
        }
        public void setPhanQuyen(String phanQuyen) { this.phanQuyen = phanQuyen; }

        public List<String> getDanhSachQuyen() { return danhSachQuyen; }
        public void setDanhSachQuyen(List<String> danhSachQuyen) { this.danhSachQuyen = danhSachQuyen; }
    }
}
