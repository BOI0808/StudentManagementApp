package com.example.studentmanagementapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CategoryManagementActivity extends AppCompatActivity {

    // Khai báo View
    private TextInputEditText edtNamHoc, edtNgayBatDau, edtNgayKetThuc, edtTenMonHoc, edtMaMonHoc;
    private RadioGroup rgHocKy;
    private Button btnThemMon, btnSuaMon, btnXoaMon;
    private ImageButton btnBack;
    private RecyclerView rvMonHoc, rvKhoiLop;

    // Khai báo dữ liệu và Adapter
    private List<MonHoc> danhSachMonHoc;
    private MonHocAdapter adapter;
    private int selectedPosition = -1; // Lưu vị trí môn đang chọn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        initViews();
        setupRecyclerView();
        setEventHandlers();
    }

    private void initViews() {
        // Ánh xạ Header & Back
        btnBack = findViewById(R.id.btnBack);

        // Ánh xạ phần Năm học - Học kỳ
        edtNamHoc = findViewById(R.id.edtNamHoc);
        rgHocKy = findViewById(R.id.rgHocKy);
        edtNgayBatDau = findViewById(R.id.edtNgayBatDau);
        edtNgayKetThuc = findViewById(R.id.edtNgayKetThuc);

        // Ánh xạ phần Môn học
        edtTenMonHoc = findViewById(R.id.edtTenMonHoc);
        edtMaMonHoc = findViewById(R.id.edtMaMonHoc);
        btnThemMon = findViewById(R.id.btnThemMon);
        btnSuaMon = findViewById(R.id.btnSuaMon);
        btnXoaMon = findViewById(R.id.btnXoaMon);
        rvMonHoc = findViewById(R.id.rvMonHoc);
        rvKhoiLop = findViewById(R.id.rvKhoiLop); 
    }

    private void setupRecyclerView() {
        // Khởi tạo dữ liệu mẫu
        danhSachMonHoc = new ArrayList<>();
        danhSachMonHoc.add(new MonHoc("MH01", "Toán"));
        danhSachMonHoc.add(new MonHoc("MH02", "Vật lý"));
        danhSachMonHoc.add(new MonHoc("MH03", "Hóa học"));

        // Cài đặt Adapter
        adapter = new MonHocAdapter(danhSachMonHoc, position -> {
            // Khi click vào item trong danh sách
            selectedPosition = position;
            MonHoc mon = danhSachMonHoc.get(position);
            edtTenMonHoc.setText(mon.getTenMon());
            edtMaMonHoc.setText(mon.getMaMon());
        });

        rvMonHoc.setLayoutManager(new LinearLayoutManager(this));
        rvMonHoc.setAdapter(adapter);
    }

    private void setEventHandlers() {
        // 1. Nút Back
        btnBack.setOnClickListener(v -> finish());

        // 2. Chọn ngày bắt đầu/kết thúc
        edtNgayBatDau.setOnClickListener(v -> showDatePicker(edtNgayBatDau));
        edtNgayKetThuc.setOnClickListener(v -> showDatePicker(edtNgayKetThuc));

        // 3. Nút THÊM môn học
        btnThemMon.setOnClickListener(v -> {
            String ten = edtTenMonHoc.getText().toString().trim();
            if (ten.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên môn học!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Tự động tạo mã MH01, MH02... dựa trên mã lớn nhất hiện có hoặc size
            String ma = "MH" + String.format("%02d", danhSachMonHoc.size() + 1);
            
            MonHoc monMoi = new MonHoc(ma, ten);
            danhSachMonHoc.add(monMoi);
            
            // Thông báo Adapter cập nhật
            adapter.notifyItemInserted(danhSachMonHoc.size() - 1);
            rvMonHoc.scrollToPosition(danhSachMonHoc.size() - 1);
            
            Toast.makeText(this, "Đã thêm môn học: " + ten, Toast.LENGTH_SHORT).show();
            clearInputs();
        });

        // 4. Nút SỬA môn học
        btnSuaMon.setOnClickListener(v -> {
            if (selectedPosition == -1) {
                Toast.makeText(this, "Vui lòng chọn một môn từ danh sách để sửa!", Toast.LENGTH_SHORT).show();
                return;
            }
            String tenMoi = edtTenMonHoc.getText().toString().trim();
            if (tenMoi.isEmpty()) {
                Toast.makeText(this, "Tên môn không được để trống!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            danhSachMonHoc.get(selectedPosition).setTenMon(tenMoi);
            adapter.notifyItemChanged(selectedPosition);
            
            Toast.makeText(this, "Đã cập nhật môn học thành công", Toast.LENGTH_SHORT).show();
            clearInputs();
        });

        // 5. Nút XÓA môn học
        btnXoaMon.setOnClickListener(v -> {
            if (selectedPosition == -1) {
                Toast.makeText(this, "Vui lòng chọn một môn từ danh sách để xóa!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String tenXoa = danhSachMonHoc.get(selectedPosition).getTenMon();
            danhSachMonHoc.remove(selectedPosition);
            adapter.notifyItemRemoved(selectedPosition);
            // Quan trọng: notifyDataSetChanged để cập nhật lại các index nếu cần, 
            // hoặc đơn giản là notifyItemRemoved là đủ cho animation
            
            Toast.makeText(this, "Đã xóa môn học: " + tenXoa, Toast.LENGTH_SHORT).show();
            clearInputs();
        });
    }

    private void showDatePicker(TextInputEditText editText) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            editText.setText(day + "/" + (month + 1) + "/" + year);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void clearInputs() {
        edtTenMonHoc.setText("");
        edtMaMonHoc.setText("Tự động");
        selectedPosition = -1;
        edtTenMonHoc.clearFocus();
    }
}