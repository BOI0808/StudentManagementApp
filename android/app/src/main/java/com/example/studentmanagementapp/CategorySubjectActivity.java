package com.example.studentmanagementapp;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.Subject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategorySubjectActivity extends AppCompatActivity {

    private TextInputLayout tilTenMon;
    private TextInputEditText edtTenMon;
    private MaterialButton btnThem;
    private RecyclerView rvMonHoc;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;
    private List<Subject> subjectList = new ArrayList<>();
    private GenericAdapter<Subject> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_subject);

        initViews();
        loadSubjectList();

        btnBack.setOnClickListener(v -> finish());
        btnThem.setOnClickListener(v -> performAddSubject());
    }

    private void initViews() {
        tilTenMon = findViewById(R.id.tilTenMonHoc);
        edtTenMon = findViewById(R.id.edtTenMonHoc);
        btnThem = findViewById(R.id.btnThem);
        rvMonHoc = findViewById(R.id.rvMonHoc);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);

        rvMonHoc.setLayoutManager(new LinearLayoutManager(this));
    }

    private void showLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
        btnThem.setEnabled(false);
    }

    private void hideLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
        btnThem.setEnabled(true);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void loadSubjectList() {
        showLoading();
        ApiClient.getApiService().getSubjectList().enqueue(new Callback<List<Subject>>() {
            @Override
            public void onResponse(@NonNull Call<List<Subject>> call, @NonNull Response<List<Subject>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    subjectList = response.body();
                    updateRecyclerView();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Subject>> call, @NonNull Throwable t) {
                hideLoading();
                Toast.makeText(CategorySubjectActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecyclerView() {
        adapter = new GenericAdapter<>(subjectList, R.layout.item_category, (subject, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvMa)).setText(subject.getMaMonHoc());
            ((TextView) itemView.findViewById(R.id.tvTen)).setText(subject.getTenMonHoc());
            
            itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
                String maMon = subject.getMaMonHoc();
                if (maMon != null) {
                    new MaterialAlertDialogBuilder(CategorySubjectActivity.this)
                            .setTitle("Xác nhận xóa")
                            .setMessage("Bạn có chắc chắn muốn ẩn môn học này khỏi danh sách không?")
                            .setNegativeButton("Hủy", null)
                            .setPositiveButton("Đồng ý", (dialog, which) -> performSoftDeleteSubject(maMon))
                            .show();
                }
            });
        });
        rvMonHoc.setAdapter(adapter);
    }

    private void performSoftDeleteSubject(String maMon) {
        Map<String, Integer> status = new HashMap<>();
        status.put("TrangThai", 0);

        showLoading();
        ApiClient.getApiService().updateSubjectStatus(maMon, status).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    Toast.makeText(CategorySubjectActivity.this, "Xóa môn học thành công", Toast.LENGTH_SHORT).show();
                    loadSubjectList();
                } else {
                    Toast.makeText(CategorySubjectActivity.this, "Không thể xóa môn học", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                Toast.makeText(CategorySubjectActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performAddSubject() {
        String tenMon = edtTenMon.getText() != null ? edtTenMon.getText().toString().trim() : "";
        
        tilTenMon.setError(null);
        if (tenMon.isEmpty()) {
            tilTenMon.setError("Vui lòng nhập tên môn học");
            return;
        }

        Subject newSubject = new Subject();
        newSubject.setTenMonHocInput(tenMon);

        showLoading();
        ApiClient.getApiService().createSubject(newSubject).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    Toast.makeText(CategorySubjectActivity.this, "Thêm môn học thành công", Toast.LENGTH_SHORT).show();
                    edtTenMon.setText("");
                    hideKeyboard();
                    loadSubjectList();
                } else {
                    showErrorDetails(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                Toast.makeText(CategorySubjectActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showErrorDetails(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                JSONObject jObjError = new JSONObject(errorBody);
                String errorMsg = jObjError.optString("error", "Dữ liệu không hợp lệ");
                Toast.makeText(this, "Thất bại: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
        }
    }
}
