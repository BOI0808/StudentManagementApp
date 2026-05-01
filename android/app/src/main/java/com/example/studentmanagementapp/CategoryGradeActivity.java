package com.example.studentmanagementapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.Block;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryGradeActivity extends AppCompatActivity {

    private TextInputLayout tilTenKhoi;
    private TextInputEditText edtTenKhoi;
    private MaterialButton btnThem;
    private RecyclerView rvKhoiLop;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;
    private List<Block> blockList = new ArrayList<>();
    private GenericAdapter<Block> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_grade);

        initViews();
        setupListeners();
        loadBlockList();
    }

    private void initViews() {
        tilTenKhoi = findViewById(R.id.tilTenKhoi);
        edtTenKhoi = findViewById(R.id.edtTenKhoi);
        btnThem = findViewById(R.id.btnThem);
        rvKhoiLop = findViewById(R.id.rvKhoiLop);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);

        rvKhoiLop.setLayoutManager(new LinearLayoutManager(this));
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
        btnThem.setOnClickListener(v -> performAddBlock());

        edtTenKhoi.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilTenKhoi != null) {
                    tilTenKhoi.setError(null);
                    tilTenKhoi.setErrorEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
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

    private void loadBlockList() {
        showLoading();
        ApiClient.getApiService().getBlockList().enqueue(new Callback<List<Block>>() {
            @Override
            public void onResponse(@NonNull Call<List<Block>> call, @NonNull Response<List<Block>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    blockList.clear();
                    blockList.addAll(response.body());
                    updateRecyclerView();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Block>> call, @NonNull Throwable t) {
                hideLoading();
                showRetrySnackbar("Lỗi tải danh sách khối lớp", () -> loadBlockList());
            }
        });
    }

    private void updateRecyclerView() {
        adapter = new GenericAdapter<>(blockList, R.layout.item_category, (item, itemView, position) -> {
            TextView tvSTT = itemView.findViewById(R.id.tvSTT);
            TextView tvMa = itemView.findViewById(R.id.tvMa);
            TextView tvTen = itemView.findViewById(R.id.tvTen);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDelete);

            tvSTT.setText(String.valueOf(position + 1));
            tvMa.setText(item.getMaKhoiLop());
            tvTen.setText(item.getTenKhoiLop());

            btnDelete.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa khối lớp " + item.getTenKhoiLop() + " không?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Xóa", (dialog, which) -> performDeleteBlock(item.getMaKhoiLop()))
                        .show();
            });
        });
        rvKhoiLop.setAdapter(adapter);
    }

    private void performAddBlock() {
        final String tenKhoi = edtTenKhoi.getText() != null ? edtTenKhoi.getText().toString().trim() : "";
        
        if (tenKhoi.isEmpty()) {
            tilTenKhoi.setErrorEnabled(true);
            tilTenKhoi.setError("Vui lòng nhập tên khối");
            return;
        }

        Block newBlock = new Block();
        newBlock.setTenKhoiLopInput(tenKhoi);

        showLoading();
        ApiClient.getApiService().createBlock(newBlock).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    new MaterialAlertDialogBuilder(CategoryGradeActivity.this)
                            .setTitle("Thành công")
                            .setMessage("Đã tạo khối lớp " + tenKhoi + " thành công")
                            .setCancelable(false)
                            .setPositiveButton("Tạo tiếp", (dialog, which) -> {
                                edtTenKhoi.setText("");
                                tilTenKhoi.setError(null);
                                tilTenKhoi.setErrorEnabled(false);
                                loadBlockList();
                            })
                            .setNegativeButton("Đóng", (dialog, which) -> finish())
                            .show();
                } else {
                    showErrorDialog("Thất bại", "Khối lớp " + tenKhoi + " đã tồn tại hoặc dữ liệu không hợp lệ.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                showRetrySnackbar("Lỗi kết nối khi thêm khối lớp", () -> performAddBlock());
            }
        });
    }

    private void performDeleteBlock(String maKhoiLop) {
        Map<String, Integer> status = new HashMap<>();
        status.put("TrangThai", 0);

        showLoading();
        ApiClient.getApiService().updateBlockStatus(maKhoiLop, status).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    Snackbar.make(findViewById(android.R.id.content), "Xóa thành công", Snackbar.LENGTH_SHORT).show();
                    loadBlockList();
                } else {
                    showErrorDialog("Thất bại", "Không thể xóa khối lớp này. Có thể khối đang có các lớp học liên quan.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                showRetrySnackbar("Lỗi kết nối khi xóa khối lớp", () -> performDeleteBlock(maKhoiLop));
            }
        });
    }
}
