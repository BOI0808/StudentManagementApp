package com.example.studentmanagementapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Calendar;

public class ReceiveStudentsActivity extends AppCompatActivity {

    // 1. Khai báo các thành phần giao diện
    private TextInputEditText edtHoTen, edtNgaySinh, edtDiaChi, edtEmail, edtGioiTinhKhac;
    private TextInputLayout tilGioiTinhKhac;
    private RadioGroup rgGioiTinh;
    private Button btnTiepNhan;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // KẾT NỐI VỚI FILE UI CỦA VINH
        setContentView(R.layout.activity_receive_students);

        initViews();
        setEvents();
    }

    private void initViews() {
        // Ánh xạ các ID từ file XML (activity_receive_students.xml)
        edtHoTen = findViewById(R.id.edtHoTen);
        edtNgaySinh = findViewById(R.id.edtNgaySinh);
        edtDiaChi = findViewById(R.id.edtDiaChi);
        edtEmail = findViewById(R.id.edtEmail);
        edtGioiTinhKhac = findViewById(R.id.edtGioiTinhKhac);
        tilGioiTinhKhac = findViewById(R.id.tilGioiTinhKhac);
        rgGioiTinh = findViewById(R.id.rgGioiTinh);
        btnTiepNhan = findViewById(R.id.btnTiepNhan);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setEvents() {
        // 1. Xử lý nút Back (Quay lại Dashboard)
        btnBack.setOnClickListener(v -> finish());

        // 2. Xử lý ẩn/hiện ô nhập giới tính khi chọn "Khác"
        rgGioiTinh.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbKhac) {
                tilGioiTinhKhac.setVisibility(View.VISIBLE);
            } else {
                tilGioiTinhKhac.setVisibility(View.GONE);
            }
        });

        // 3. Hiển thị bảng chọn ngày khi bấm vào ô Ngày sinh
        edtNgaySinh.setOnClickListener(v -> showDatePicker());

        // 4. Xử lý nút Tiếp nhận hồ sơ
        btnTiepNhan.setOnClickListener(v -> {
            String hoTen = edtHoTen.getText().toString().trim();
            if (hoTen.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập họ tên học sinh!", Toast.LENGTH_SHORT).show();
            } else {
                // Sau này Khôi sẽ viết logic lưu database ở đây
                Toast.makeText(this, "Đã tiếp nhận hồ sơ học sinh: " + hoTen, Toast.LENGTH_SHORT).show();
                finish(); // Đóng màn hình sau khi thành công
            }
        });
    }

    // Hàm hiển thị lịch (DatePicker)
    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    edtNgaySinh.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }
}