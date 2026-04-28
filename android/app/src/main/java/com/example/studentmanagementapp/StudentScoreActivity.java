package com.example.studentmanagementapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;
import java.util.Map;

public class StudentScoreActivity extends AppCompatActivity {

    private TextView tvHoTen, tvMaHS, tvLop, tvDiemHK1, tvDiemHK2, tvDiemCaNam;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_score);

        initViews();

        Map<String, Object> studentData = (Map<String, Object>) getIntent().getSerializableExtra("student_data");
        if (studentData != null) {
            displayData(studentData);
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvHoTen = findViewById(R.id.tvHoTen);
        tvMaHS = findViewById(R.id.tvMaHS);
        tvLop = findViewById(R.id.tvLop);
        tvDiemHK1 = findViewById(R.id.tvDiemHK1);
        tvDiemHK2 = findViewById(R.id.tvDiemHK2);
        tvDiemCaNam = findViewById(R.id.tvDiemCaNam);
        btnBack = findViewById(R.id.btnBack);
    }

    private void displayData(Map<String, Object> data) {
        tvHoTen.setText("Họ tên: " + getValue(data, "HoTen", "hoTen", "HOTEN", "TenHocSinh"));
        tvMaHS.setText("Mã HS: " + getValue(data, "MaHocSinh", "maHocSinh", "MAHOCSINH", "MaHS"));
        tvLop.setText("Lớp: " + getValue(data, "TenLop", "lop", "TENLOP", "TenLopHoc"));

        // Thêm các trường key có thể có từ API
        tvDiemHK1.setText(formatScore(findValue(data, "DiemHK1", "DTB_HK1", "diemHK1", "dtbHK1", "DiemTrungBinhHK1")));
        tvDiemHK2.setText(formatScore(findValue(data, "DiemHK2", "DTB_HK2", "diemHK2", "dtbHK2", "DiemTrungBinhHK2")));
        tvDiemCaNam.setText(formatScore(findValue(data, "DiemCaNam", "TBCN", "DiemTrungBinhMon", "diemCaNam", "tbcn", "DiemTrungBinhCaNam")));
    }

    private Object findValue(Map<String, Object> map, String... keys) {
        if (map == null) return null;
        for (String key : keys) {
            if (map.containsKey(key)) return map.get(key);
            for (String actualKey : map.keySet()) {
                if (actualKey.equalsIgnoreCase(key)) return map.get(actualKey);
            }
        }
        return null;
    }

    private String formatScore(Object obj) {
        if (obj == null || obj.toString().isEmpty() || obj.toString().equalsIgnoreCase("null")) return "-";
        try {
            double score = Double.parseDouble(obj.toString());
            // Làm tròn 1 chữ số thập phân
            return String.format(Locale.getDefault(), "%.1f", score);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String getValue(Map<String, Object> map, String... keys) {
        if (map == null) return "N/A";
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) return map.get(key).toString();
            for (String k : map.keySet()) {
                if (k.equalsIgnoreCase(key) && map.get(k) != null) return map.get(k).toString();
            }
        }
        return "N/A";
    }
}
