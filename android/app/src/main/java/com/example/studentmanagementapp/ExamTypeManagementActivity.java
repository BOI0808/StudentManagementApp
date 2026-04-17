package com.example.studentmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.api.ApiService;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExamTypeManagementActivity extends AppCompatActivity {

    private EditText edtTestTypeName, edtCoefficient;
    private MaterialButton btnAddExamType;
    private RecyclerView rvExamTypes;
    private GenericAdapter<Map<String, Object>> adapter;
    private List<Map<String, Object>> examTypeList = new ArrayList<>();
    private ApiService apiService;

    // Launcher để nhận kết quả khi quay về từ màn hình Sửa
    private final ActivityResultLauncher<Intent> editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadExamTypes(); // Tải lại danh sách sau khi sửa thành công
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_type_management);

        initViews();
        apiService = ApiClient.getApiService();
        setupRecyclerView();
        loadExamTypes();

        btnAddExamType.setOnClickListener(v -> createExamType());
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        edtTestTypeName = findViewById(R.id.edtTestTypeName);
        edtCoefficient = findViewById(R.id.edtCoefficient);
        btnAddExamType = findViewById(R.id.btnAddExamType);
        rvExamTypes = findViewById(R.id.rvExamTypes);
    }

    private void setupRecyclerView() {
        rvExamTypes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(examTypeList, R.layout.item_exam_type, (item, itemView, position) -> {
            TextView tvSTT = itemView.findViewById(R.id.tvSTT);
            TextView tvExamTypeName = itemView.findViewById(R.id.tvExamTypeName);
            TextView tvCoefficient = itemView.findViewById(R.id.tvCoefficient);
            ImageButton btnEdit = itemView.findViewById(R.id.btnEdit);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDelete);

            tvSTT.setText(String.valueOf(position + 1));
            tvExamTypeName.setText(String.valueOf(item.get("TenLoaiKiemTra")));
            tvCoefficient.setText(String.valueOf(item.get("HeSo")));

            btnDelete.setOnClickListener(v -> {
                String id = String.valueOf(item.get("MaLoaiKiemTra"));
                deleteExamType(id);
            });
            
            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(ExamTypeManagementActivity.this, EditExamTypeActivity.class);
                intent.putExtra("MaLoaiKiemTra", String.valueOf(item.get("MaLoaiKiemTra")));
                intent.putExtra("TenLoaiKiemTra", String.valueOf(item.get("TenLoaiKiemTra")));
                intent.putExtra("HeSo", String.valueOf(item.get("HeSo")));
                editLauncher.launch(intent);
            });
        });
        rvExamTypes.setAdapter(adapter);
    }

    private void loadExamTypes() {
        apiService.getTestTypeList().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    examTypeList.clear();
                    examTypeList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(ExamTypeManagementActivity.this, "Không thể tải danh sách", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(ExamTypeManagementActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createExamType() {
        String name = edtTestTypeName.getText().toString().trim();
        String coeffStr = edtCoefficient.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(coeffStr)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        double coefficient;
        try {
            coefficient = Double.parseDouble(coeffStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Hệ số không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("TenLoaiKiemTra", name);
        data.put("HeSo", coefficient);

        apiService.createTestType(data).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ExamTypeManagementActivity.this, "Thêm thành công", Toast.LENGTH_SHORT).show();
                    edtTestTypeName.setText("");
                    edtCoefficient.setText("");
                    loadExamTypes(); // Refresh list
                } else {
                    Toast.makeText(ExamTypeManagementActivity.this, "Lỗi: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ExamTypeManagementActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteExamType(String id) {
        apiService.deleteTestType(id).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ExamTypeManagementActivity.this, "Xóa thành công", Toast.LENGTH_SHORT).show();
                    loadExamTypes();
                } else {
                    Toast.makeText(ExamTypeManagementActivity.this, "Lỗi khi xóa", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(ExamTypeManagementActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
