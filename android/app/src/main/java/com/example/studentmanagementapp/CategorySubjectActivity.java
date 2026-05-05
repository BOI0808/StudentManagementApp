package com.example.studentmanagementapp;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.Subject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
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

    private TextInputEditText edtSearch;
    private ProgressBar pbSearchLoading;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final List<Subject> fullSubjectList = new ArrayList<>();
    private final List<Subject> displayList = new ArrayList<>();
    private GenericAdapter<Subject> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_subject);

        initViews();
        setupListeners();
        loadSubjectList();
    }

    private void initViews() {
        tilTenMon = findViewById(R.id.tilTenMonHoc);
        edtTenMon = findViewById(R.id.edtTenMonHoc);
        btnThem = findViewById(R.id.btnThem);
        rvMonHoc = findViewById(R.id.rvMonHoc);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);
        
        edtSearch = findViewById(R.id.edtSearchSubject);
        pbSearchLoading = findViewById(R.id.pbSearchLoading);

        rvMonHoc.setLayoutManager(new LinearLayoutManager(this));
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

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnThem.setOnClickListener(v -> performAddSubject());

        edtTenMon.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilTenMon != null) {
                    tilTenMon.setError(null);
                    tilTenMon.setErrorEnabled(false);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                pbSearchLoading.setVisibility(View.VISIBLE);
                
                searchRunnable = () -> performFilter(s.toString());
                searchHandler.postDelayed(searchRunnable, 300);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performFilter(String query) {
        String filterPattern = query.toLowerCase().trim();
        displayList.clear();
        
        if (filterPattern.isEmpty()) {
            displayList.addAll(fullSubjectList);
        } else {
            for (Subject item : fullSubjectList) {
                if (item.getTenMonHoc() != null && item.getTenMonHoc().toLowerCase().contains(filterPattern)) {
                    displayList.add(item);
                }
            }
        }
        
        pbSearchLoading.setVisibility(View.GONE);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showLoading() {
        runOnUiThread(() -> {
            if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
            btnThem.setEnabled(false);
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> {
            if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
            btnThem.setEnabled(true);
        });
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
                    fullSubjectList.clear();
                    fullSubjectList.addAll(response.body());
                    displayList.clear();
                    displayList.addAll(fullSubjectList);
                    updateRecyclerView();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Subject>> call, @NonNull Throwable t) {
                hideLoading();
                showRetrySnackbar("Lỗi tải danh sách môn học", CategorySubjectActivity.this::loadSubjectList);
            }
        });
    }

    private void updateRecyclerView() {
        adapter = new GenericAdapter<>(displayList, R.layout.item_category, (subject, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvMa)).setText(subject.getMaMonHoc());
            ((TextView) itemView.findViewById(R.id.tvTen)).setText(subject.getTenMonHoc());
            
            itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
                String maMon = subject.getMaMonHoc();
                if (maMon != null) {
                    new MaterialAlertDialogBuilder(CategorySubjectActivity.this)
                            .setTitle("Xác nhận xóa")
                            .setMessage("Bạn có chắc chắn muốn ẩn môn học " + subject.getTenMonHoc() + " khỏi danh sách không?")
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
                    Snackbar.make(findViewById(android.R.id.content), "Đã ẩn môn học thành công", Snackbar.LENGTH_SHORT).show();
                    loadSubjectList();
                } else {
                    showErrorDialog("Thất bại", "Không thể xóa môn học này. Có thể môn học đang có dữ liệu điểm liên quan.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                showRetrySnackbar("Lỗi kết nối khi xóa môn học", () -> performSoftDeleteSubject(maMon));
            }
        });
    }

    private void performAddSubject() {
        String tenMon = edtTenMon.getText() != null ? edtTenMon.getText().toString().trim() : "";
        
        if (tenMon.isEmpty()) {
            tilTenMon.setErrorEnabled(true);
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
                    new MaterialAlertDialogBuilder(CategorySubjectActivity.this)
                            .setTitle("Thành công")
                            .setMessage("Đã thêm môn học " + tenMon + " vào hệ thống.")
                            .setCancelable(false)
                            .setPositiveButton("Tạo tiếp", (dialog, which) -> {
                                edtTenMon.setText("");
                                tilTenMon.setError(null);
                                tilTenMon.setErrorEnabled(false);
                                loadSubjectList();
                            })
                            .setNegativeButton("Đóng", (dialog, which) -> finish())
                            .show();
                } else {
                    showErrorDetails(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                showRetrySnackbar("Lỗi kết nối khi thêm môn học", CategorySubjectActivity.this::performAddSubject);
            }
        });
    }

    private void showErrorDetails(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                JSONObject jObjError = new JSONObject(errorBody);
                String errorMsg = jObjError.optString("error", "Dữ liệu không hợp lệ");
                showErrorDialog("Thất bại", errorMsg);
            } else {
                showErrorDialog("Lỗi", "Lỗi Server: " + response.code());
            }
        } catch (Exception e) {
            showErrorDialog("Lỗi", "Lỗi xử lý thông báo lỗi từ server.");
        }
    }
}
