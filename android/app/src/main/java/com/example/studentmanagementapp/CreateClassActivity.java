package com.example.studentmanagementapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.Block;
import com.example.studentmanagementapp.model.ClassModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateClassActivity extends AppCompatActivity {

    private TextInputEditText edtTenLop;
    private AutoCompleteTextView autoCompleteKhoiLop, autoCompleteNamHoc, autoCompleteHocKy;
    private MaterialButton btnTaoLop;
    private ImageButton btnBack;
    
    private List<Block> blockList = new ArrayList<>();
    private List<Map<String, String>> termList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_class);

        initViews();
        loadData();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        btnTaoLop.setOnClickListener(v -> performCreateClass());
    }

    private void initViews() {
        edtTenLop = findViewById(R.id.edtTenLop);
        autoCompleteKhoiLop = findViewById(R.id.autoCompleteKhoiLop);
        autoCompleteNamHoc = findViewById(R.id.autoCompleteNamHoc);
        autoCompleteHocKy = findViewById(R.id.autoCompleteHocKy);
        btnTaoLop = findViewById(R.id.btnTaoLop);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadData() {
        // Tải danh sách khối
        ApiClient.getApiService().getBlockList().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Block>> call, @NonNull Response<List<Block>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    blockList = response.body();
                    List<String> names = new ArrayList<>();
                    for (Block b : blockList) {
                        if (b.getTenKhoiLop() != null) names.add(b.getTenKhoiLop());
                    }
                    autoCompleteKhoiLop.setAdapter(new ArrayAdapter<>(CreateClassActivity.this, android.R.layout.simple_list_item_1, names));
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Block>> call, @NonNull Throwable t) {}
        });

        // Tải danh sách Năm học & Học kỳ
        ApiClient.getApiService().getSemesterList().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    termList = response.body();
                    setupTermSpinners();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {}
        });
    }

    private void setupTermSpinners() {
        List<String> years = new ArrayList<>();
        for (Map<String, String> m : termList) {
            String namHoc = m.get("namhoc");
            if (namHoc != null && !years.contains(namHoc)) years.add(namHoc);
        }
        autoCompleteNamHoc.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));

        String[] types = {"Học kỳ 1", "Học kỳ 2", "Cả năm"};
        autoCompleteHocKy.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, types));
    }

    private void performCreateClass() {
        String tenLop = (edtTenLop.getText() != null) ? edtTenLop.getText().toString().trim() : "";
        String tenKhoi = autoCompleteKhoiLop.getText().toString();
        String namHoc = autoCompleteNamHoc.getText().toString();
        String hocKyStr = autoCompleteHocKy.getText().toString();

        if (tenLop.isEmpty() || tenKhoi.isEmpty() || namHoc.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        ClassModel classModel = new ClassModel();
        classModel.setTenLop(tenLop);
        
        // Tìm MaKhoi
        for (Block b : blockList) {
            if (tenKhoi.equals(b.getTenKhoiLop())) {
                classModel.setMaKhoiLop(b.getMaKhoiLop());
                break;
            }
        }

        // Tìm MaHocKyNamHoc
        String selectedHK = "Cả năm".equals(hocKyStr) ? "Học kỳ 1" : hocKyStr;
        for (Map<String, String> m : termList) {
            if (namHoc.equals(m.get("namhoc")) && selectedHK.equals(m.get("hocky"))) {
                classModel.setMaHocKyNamHoc(m.get("ma"));
                break;
            }
        }

        classModel.setLoaiHocKy("Cả năm".equals(hocKyStr) ? 3 : 1);

        btnTaoLop.setEnabled(false);
        ApiClient.getApiService().createClass(classModel).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                btnTaoLop.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(CreateClassActivity.this, "Tạo lớp thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreateClassActivity.this, "Lỗi: Lớp đã tồn tại trong niên khóa này", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                btnTaoLop.setEnabled(true);
                Toast.makeText(CreateClassActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
