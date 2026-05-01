package com.example.studentmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.api.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExamTypeManagementActivity extends AppCompatActivity {

    private EditText edtTestTypeName, edtCoefficient;
    private TextInputLayout tilTestTypeName, tilCoefficient;
    private MaterialButton btnAddExamType;
    private RecyclerView rvExamTypes;
    private LinearProgressIndicator progressIndicator;
    private GenericAdapter<Map<String, Object>> adapter;
    private final List<Map<String, Object>> examTypeList = new ArrayList<>();
    private ApiService apiService;

    private final ActivityResultLauncher<Intent> editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadExamTypes();
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
        
        progressIndicator = findViewById(R.id.progressIndicator);
        tilTestTypeName = findViewById(R.id.tilTestTypeName);
        tilCoefficient = findViewById(R.id.tilCoefficient);
        edtTestTypeName = findViewById(R.id.edtTestTypeName);
        edtCoefficient = findViewById(R.id.edtCoefficient);
        btnAddExamType = findViewById(R.id.btnAddExamType);
        rvExamTypes = findViewById(R.id.rvExamTypes);

        edtTestTypeName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilTestTypeName.setError(null);
                tilTestTypeName.setErrorEnabled(false);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        edtCoefficient.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilCoefficient.setError(null);
                tilCoefficient.setErrorEnabled(false);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showLoading() {
        runOnUiThread(() -> {
            if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
            if (btnAddExamType != null) btnAddExamType.setEnabled(false);
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> {
            if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
            if (btnAddExamType != null) btnAddExamType.setEnabled(true);
        });
    }

    private void showErrorDialog(String title, String message) {
        runOnUiThread(() -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    private void showRetrySnackbar(String message, Runnable retryAction) {
        runOnUiThread(() -> {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                    .setAction("Thử lại", v -> retryAction.run())
                    .show();
        });
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
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa loại kiểm tra '" + item.get("TenLoaiKiemTra") + "' không?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Xóa", (dialog, which) -> deleteExamType(id))
                        .show();
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
        showLoading();
        apiService.getTestTypeList().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    examTypeList.clear();
                    examTypeList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    showRetrySnackbar("Không thể tải danh sách loại kiểm tra", ExamTypeManagementActivity.this::loadExamTypes);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                hideLoading();
                showRetrySnackbar("Lỗi kết nối máy chủ", ExamTypeManagementActivity.this::loadExamTypes);
            }
        });
    }

    private void createExamType() {
        String name = edtTestTypeName.getText().toString().trim();
        String coeffStr = edtCoefficient.getText().toString().trim();

        boolean hasError = false;

        if (TextUtils.isEmpty(name)) {
            tilTestTypeName.setErrorEnabled(true);
            tilTestTypeName.setError("Vui lòng nhập tên loại kiểm tra");
            hasError = true;
        }

        if (TextUtils.isEmpty(coeffStr)) {
            tilCoefficient.setErrorEnabled(true);
            tilCoefficient.setError("Vui lòng nhập hệ số");
            hasError = true;
        }

        if (hasError) return;

        double coefficient;
        try {
            coefficient = Double.parseDouble(coeffStr);
        } catch (NumberFormatException e) {
            tilCoefficient.setErrorEnabled(true);
            tilCoefficient.setError("Hệ số không hợp lệ");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("TenLoaiKiemTra", name);
        data.put("HeSo", coefficient);

        showLoading();
        apiService.createTestType(data).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    new MaterialAlertDialogBuilder(ExamTypeManagementActivity.this)
                            .setTitle("Thành công")
                            .setMessage("Đã thêm loại kiểm tra mới vào hệ thống.")
                            .setCancelable(false)
                            .setPositiveButton("Thêm tiếp", (dialog, which) -> {
                                edtTestTypeName.setText("");
                                edtCoefficient.setText("");
                                loadExamTypes();
                            })
                            .setNegativeButton("Đóng", (dialog, which) -> finish())
                            .show();
                } else {
                    showErrorDialog("Thất bại", "Không thể tạo loại kiểm tra. Có thể tên đã tồn tại hoặc dữ liệu không hợp lệ.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                hideLoading();
                showRetrySnackbar("Lỗi kết nối khi tạo mới", ExamTypeManagementActivity.this::createExamType);
            }
        });
    }

    private void deleteExamType(String id) {
        showLoading();
        apiService.deleteTestType(id).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    Snackbar.make(findViewById(android.R.id.content), "Đã xóa thành công", Snackbar.LENGTH_SHORT).show();
                    loadExamTypes();
                } else {
                    showErrorDialog("Thất bại", "Không thể xóa loại kiểm tra này. Có thể nó đang được sử dụng trong hệ thống điểm.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                showRetrySnackbar("Lỗi kết nối khi xóa", () -> deleteExamType(id));
            }
        });
    }
}
