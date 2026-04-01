package com.example.studentmanagementapp;

public class Student {
    private String hoTen;
    private String ngaySinh;
    private String email;

    public Student(String hoTen, String ngaySinh, String email) {
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.email = email;
    }

    public String getHoTen() { return hoTen; }
    public String getNgaySinh() { return ngaySinh; }
    public String getEmail() { return email; }
}