package com.example.studentmanagementapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class SearchStudentsActivity extends AppCompatActivity {

    private TextInputEditText edtSearchTen, edtSearchMaHS, edtSearchLop;
    private Button btnTimKiem;
    private ImageButton btnBack;
    private RecyclerView rvKetQuaTraCuu;

    private List<HocSinh> listHocSinhGoc; // Danh sách tổng
    private List<HocSinh> listKetQua;     // Danh sách sau khi lọc


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_students); // Tên file XML bạn vừa gửi

        initViews();
        setupData();
        setEvents();
    }

    private void initViews() {
        edtSearchTen = findViewById(R.id.edtSearchTen);
        edtSearchMaHS = findViewById(R.id.edtSearchMaHS);
        edtSearchLop = findViewById(R.id.edtSearchLop);
        btnTimKiem = findViewById(R.id.btnTimKiem);
        btnBack = findViewById(R.id.btnBack);
        rvKetQuaTraCuu = findViewById(R.id.rvKetQuaTraCuu);
    }

    private void setupData() {
        // Dữ liệu mẫu để Giang test UI
        listHocSinhGoc = new ArrayList<>();
        listHocSinhGoc.add(new HocSinh("HS001", "Nguyễn Văn An", "10A1"));
        listHocSinhGoc.add(new HocSinh("HS002", "Trần Thị Bình", "10A2"));
        listHocSinhGoc.add(new HocSinh("HS003", "Lê Văn Cường", "11B1"));

        listKetQua = new ArrayList<>(listHocSinhGoc);

        // Cài đặt RecyclerView (Tạm thời dùng Adapter đơn giản)
        rvKetQuaTraCuu.setLayoutManager(new LinearLayoutManager(this));
        // Lưu ý: Giang cần tạo file SearchStudentAdapter để hiển thị đẹp hơn
        // Hiện tại mình giả định bạn đã có Adapter này
    }

    private void setEvents() {
        btnBack.setOnClickListener(v -> finish());

        btnTimKiem.setOnClickListener(v -> {
            String ten = edtSearchTen.getText().toString().toLowerCase().trim();
            String ma = edtSearchMaHS.getText().toString().toLowerCase().trim();
            String lop = edtSearchLop.getText().toString().toLowerCase().trim();

            // Logic tìm kiếm ảo
            Toast.makeText(this, "Đang tìm kiếm: " + ten, Toast.LENGTH_SHORT).show();

            // Sau này Khôi sẽ viết câu lệnh SQL hoặc gọi API tìm kiếm ở đây
        });
    }
}