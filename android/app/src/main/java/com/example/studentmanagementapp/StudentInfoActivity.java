package com.example.studentmanagementapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentInfoActivity extends AppCompatActivity {

    private LinearLayout layoutHistoryItems;
    private TextView tvEmptyHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_info);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        
        layoutHistoryItems = findViewById(R.id.layoutHistoryItems);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);

        Map<String, Object> studentData = (Map<String, Object>) getIntent().getSerializableExtra("student_data");
        if (studentData != null) {
            fillData(studentData);
            String maHS = getValue(studentData, "MaHocSinh", "maHocSinh");
            if (!maHS.equals("N/A")) {
                loadHistory(maHS);
            }
        }
    }

    private void fillData(Map<String, Object> data) {
        setupRow(R.id.rowMaHS, "Mã học sinh:", getValue(data, "MaHocSinh", "maHocSinh", "MAHOCSINH"));
        setupRow(R.id.rowHoTen, "Họ và tên:", getValue(data, "HoTen", "hoTen", "HOTEN"));
        setupRow(R.id.rowLop, "Lớp:", getValue(data, "TenLop", "lop", "TENLOP"));
        
        String ngaySinh = getValue(data, "NgaySinh", "ngaySinh", "NGAYSINH");
        setupRow(R.id.rowNgaySinh, "Ngày sinh:", formatDate(ngaySinh));
        
        String gioiTinh = getValue(data, "MaGioiTinh", "gioiTinh", "MAGIOITINH", "GioiTinh");
        if ("GT1".equals(gioiTinh) || "Nam".equalsIgnoreCase(gioiTinh)) gioiTinh = "Nam";
        else if ("GT2".equals(gioiTinh) || "Nữ".equalsIgnoreCase(gioiTinh)) gioiTinh = "Nữ";
        else if ("GT3".equals(gioiTinh) || "Khác".equalsIgnoreCase(gioiTinh)) gioiTinh = "Khác";
        setupRow(R.id.rowGioiTinh, "Giới tính:", gioiTinh);
        
        setupRow(R.id.rowDiaChi, "Địa chỉ:", getValue(data, "DiaChi", "diaChi", "DIACHI"));
        setupRow(R.id.rowEmail, "Email:", getValue(data, "Email", "email", "EMAIL"));
    }

    private void loadHistory(String maHS) {
        ApiClient.getApiService().getStudentHistory(maHS).enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayHistory(response.body());
                } else {
                    tvEmptyHistory.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {
                tvEmptyHistory.setVisibility(View.VISIBLE);
                Toast.makeText(StudentInfoActivity.this, "Không thể tải lịch sử", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayHistory(List<Map<String, String>> historyList) {
        layoutHistoryItems.removeAllViews();
        if (historyList.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyHistory.setVisibility(View.GONE);
        for (Map<String, String> item : historyList) {
            View itemView = getLayoutInflater().inflate(R.layout.view_info_row, layoutHistoryItems, false);
            String label = item.get("NamHoc") + " - " + item.get("TenHocKy");
            String value = "Lớp: " + item.get("TenLop");
            
            ((TextView) itemView.findViewById(R.id.tvLabel)).setText(label);
            ((TextView) itemView.findViewById(R.id.tvValue)).setText(value);
            
            layoutHistoryItems.addView(itemView);
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.equals("N/A")) return dateStr;
        try {
            SimpleDateFormat inputFormat;
            if (dateStr.contains("T")) {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            } else {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }
            
            Date date = inputFormat.parse(dateStr);
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            if (dateStr.length() >= 10) {
                return dateStr.substring(0, 10);
            }
            return dateStr;
        }
    }

    private void setupRow(int rowId, String label, String value) {
        View row = findViewById(rowId);
        if (row != null) {
            ((TextView) row.findViewById(R.id.tvLabel)).setText(label);
            ((TextView) row.findViewById(R.id.tvValue)).setText(value);
        }
    }

    private String getValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) return map.get(key).toString();
            for (String k : map.keySet()) {
                if (k.equalsIgnoreCase(key) && map.get(k) != null) return map.get(k).toString();
            }
        }
        return "N/A";
    }
}
