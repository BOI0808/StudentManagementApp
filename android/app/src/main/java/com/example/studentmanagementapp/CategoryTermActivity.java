package com.example.studentmanagementapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryTermActivity extends AppCompatActivity {

    private TextInputEditText edtNamBatDau, edtNamKetThuc;
    private RadioGroup rgHocKy;
    private MaterialButton btnThem;
    private RecyclerView rvNamHoc;
    private ImageButton btnBack;
    private List<Map<String, String>> termList = new ArrayList<>();
    private GenericAdapter<Map<String, String>> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_term);

        initViews();
        loadTermList();

        btnBack.setOnClickListener(v -> finish());
        btnThem.setOnClickListener(v -> performAddTerm());
    }

    private void initViews() {
        edtNamBatDau = findViewById(R.id.edtNamBatDau);
        edtNamKetThuc = findViewById(R.id.edtNamKetThuc);
        rgHocKy = findViewById(R.id.rgHocKy);
        btnThem = findViewById(R.id.btnThemNamHoc);
        rvNamHoc = findViewById(R.id.rvNamHoc);
        btnBack = findViewById(R.id.btnBack);

        rvNamHoc.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadTermList() {
        ApiClient.getApiService().getSemesterList().enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    termList.clear();
                    termList.addAll(response.body());
                    updateRecyclerView();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {
                Toast.makeText(CategoryTermActivity.this, "Lỗi tải dữ liệu học kỳ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecyclerView() {
        adapter = new GenericAdapter<>(termList, R.layout.item_term_row, (term, itemView, position) -> {
            TextView tvSTT = itemView.findViewById(R.id.tvSTT);
            TextView tvNamHoc = itemView.findViewById(R.id.tvNamHoc);
            TextView tvHocKy = itemView.findViewById(R.id.tvHocKy);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDeleteTerm);

            tvSTT.setText(String.valueOf(position + 1));
            tvNamHoc.setText(term.get("namhoc"));
            tvHocKy.setText(term.get("hocky"));
            
            btnDelete.setOnClickListener(v -> {
                String ma = term.get("ma");
                if (ma != null) {
                    ApiClient.getApiService().deleteSemester(ma).enqueue(new Callback<Map<String, String>>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(CategoryTermActivity.this, "Đã xóa học kỳ", Toast.LENGTH_SHORT).show();
                                loadTermList();
                            } else {
                                Toast.makeText(CategoryTermActivity.this, "Không thể xóa học kỳ đã có lớp học", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                            Toast.makeText(CategoryTermActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });
        rvNamHoc.setAdapter(adapter);
    }

    private void performAddTerm() {
        String namBD = (edtNamBatDau.getText() != null) ? edtNamBatDau.getText().toString().trim() : "";
        String namKT = (edtNamKetThuc.getText() != null) ? edtNamKetThuc.getText().toString().trim() : "";
        
        if (namBD.isEmpty() || namKT.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập năm học bắt đầu và kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }

        int hockyValue = 3; // Mặc định là Cả năm
        int checkedId = rgHocKy.getCheckedRadioButtonId();
        if (checkedId == R.id.rbHocKy1) hockyValue = 1;
        else if (checkedId == R.id.rbHocKy2) hockyValue = 2;

        Map<String, Object> data = new HashMap<>();
        try {
            data.put("NamHocBatDau", Integer.parseInt(namBD));
            data.put("NamHocKetThuc", Integer.parseInt(namKT));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Năm học phải là số nguyên (VD: 2025)", Toast.LENGTH_SHORT).show();
            return;
        }
        data.put("HocKy", hockyValue);

        ApiClient.getApiService().createSemester(data).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CategoryTermActivity.this, "Thêm học kỳ thành công!", Toast.LENGTH_SHORT).show();
                    edtNamBatDau.setText("");
                    edtNamKetThuc.setText("");
                    loadTermList();
                } else {
                    Toast.makeText(CategoryTermActivity.this, "Lỗi: Niên khóa không hợp lệ hoặc đã tồn tại", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                Toast.makeText(CategoryTermActivity.this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
