package com.example.studentmanagementapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryTermActivity extends AppCompatActivity {

    private TextInputLayout tilNamBatDau, tilNamKetThuc;
    private TextInputEditText edtNamBatDau, edtNamKetThuc;
    private RadioGroup rgHocKy;
    private RecyclerView rvNamHoc;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;
    private List<Map<String, String>> termList = new ArrayList<>();
    private GenericAdapter<Map<String, String>> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_term);

        initViews();
        setupListeners();
        loadTermList();
    }

    private void initViews() {
        tilNamBatDau = findViewById(R.id.tilNamBatDau);
        tilNamKetThuc = findViewById(R.id.tilNamKetThuc);
        edtNamBatDau = findViewById(R.id.edtNamBatDau);
        edtNamKetThuc = findViewById(R.id.edtNamKetThuc);
        rgHocKy = findViewById(R.id.rgHocKy);
        rvNamHoc = findViewById(R.id.rvNamHoc);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);

        rvNamHoc.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.btnThemNamHoc).setOnClickListener(v -> performAddTerm());

        // Auto-fill: Năm kết thúc = Năm bắt đầu + 1
        edtNamBatDau.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                tilNamBatDau.setError(null);
                if (s.length() == 4) {
                    try {
                        int namBD = Integer.parseInt(s.toString());
                        edtNamKetThuc.setText(String.valueOf(namBD + 1));
                        tilNamKetThuc.setError(null);
                    } catch (NumberFormatException ignored) {}
                }
            }
        });

        edtNamKetThuc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tilNamKetThuc.setError(null);
            }
        });
    }

    private void loadTermList() {
        progressIndicator.setVisibility(View.VISIBLE);
        ApiClient.getApiService().getSemesterList().enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                progressIndicator.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    termList.clear();
                    termList.addAll(response.body());
                    updateRecyclerView();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(CategoryTermActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecyclerView() {
        adapter = new GenericAdapter<>(termList, R.layout.item_term_row, (term, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvNamHoc)).setText(term.get("namhoc"));
            ((TextView) itemView.findViewById(R.id.tvHocKy)).setText(term.get("hocky"));
            
            itemView.findViewById(R.id.btnDeleteTerm).setOnClickListener(v -> {
                String ma = term.get("ma");
                if (ma != null) {
                    showDeleteConfirmation(ma);
                }
            });
        });
        rvNamHoc.setAdapter(adapter);
    }

    private void showDeleteConfirmation(String ma) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa học kỳ này không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> deleteTerm(ma))
                .show();
    }

    private void deleteTerm(String ma) {
        progressIndicator.setVisibility(View.VISIBLE);
        ApiClient.getApiService().deleteSemester(ma).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                progressIndicator.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(CategoryTermActivity.this, "Đã xóa thành công", Toast.LENGTH_SHORT).show();
                    loadTermList();
                } else {
                    Toast.makeText(CategoryTermActivity.this, "Không thể xóa học kỳ này", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(CategoryTermActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performAddTerm() {
        String strNamBD = edtNamBatDau.getText().toString().trim();
        String strNamKT = edtNamKetThuc.getText().toString().trim();

        boolean isValid = true;

        if (strNamBD.isEmpty()) {
            tilNamBatDau.setError("Không được để trống");
            isValid = false;
        }
        if (strNamKT.isEmpty()) {
            tilNamKetThuc.setError("Không được để trống");
            isValid = false;
        }

        if (!isValid) return;

        try {
            int namBD = Integer.parseInt(strNamBD);
            int namKT = Integer.parseInt(strNamKT);

            if (namBD >= namKT) {
                tilNamBatDau.setError("Năm bắt đầu phải nhỏ hơn năm kết thúc");
                isValid = false;
            } else if (namKT - namBD != 1) {
                tilNamKetThuc.setError("Khoảng cách phải đúng bằng 1 năm");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            tilNamBatDau.setError("Năm không hợp lệ");
            isValid = false;
        }

        if (!isValid) return;

        int hockyValue = 3;
        int checkedId = rgHocKy.getCheckedRadioButtonId();
        if (checkedId == R.id.rbHocKy1) hockyValue = 1;
        else if (checkedId == R.id.rbHocKy2) hockyValue = 2;

        Map<String, Object> data = new HashMap<>();
        data.put("NamHocBatDau", Integer.parseInt(strNamBD));
        data.put("NamHocKetThuc", Integer.parseInt(strNamKT));
        data.put("HocKy", hockyValue);

        progressIndicator.setVisibility(View.VISIBLE);
        ApiClient.getApiService().createSemester(data).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                progressIndicator.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(CategoryTermActivity.this, "Thêm thành công", Toast.LENGTH_SHORT).show();
                    edtNamBatDau.setText("");
                    edtNamKetThuc.setText("");
                    loadTermList();
                } else {
                    Toast.makeText(CategoryTermActivity.this, "Học kỳ đã tồn tại hoặc dữ liệu lỗi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(CategoryTermActivity.this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
