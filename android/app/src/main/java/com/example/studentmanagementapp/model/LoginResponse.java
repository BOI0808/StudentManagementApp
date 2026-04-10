package com.example.studentmanagementapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LoginResponse {
    @SerializedName("message")
    private String message;
    
    @SerializedName("user")
    private UserData user;

    public String getMessage() { return message; }
    public UserData getUser() { return user; }

    public static class UserData {
        @SerializedName("MaSo")
        private String maSo;
        @SerializedName("HoTen")
        private String hoTen;
        @SerializedName("PhanQuyen")
        private String phanQuyen;
        @SerializedName("quyen")
        private List<String> quyen;

        public String getMaSo() { return maSo; }
        public String getHoTen() { return hoTen; }
        public String getPhanQuyen() { return phanQuyen; }
        public List<String> getQuyen() { return quyen; }
    }
}
