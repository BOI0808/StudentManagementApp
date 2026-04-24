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
import com.example.studentmanagementapp.model.Block;
import com.google.android.material.button.MaterialButton;
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
        loadBlockList();

        btnBack.setOnClickListener(v -> finish());
        btnThem.setOnClickListener(v -> performAddBlock());
        
        // Trải nghiệm: Tự động focus và mở bàn phím
        edtTenKhoi.requestFocus();
        showKeyboard(edtTenKhoi);
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

    private void showLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
        btnThem.setEnabled(false);
    }

    private void hideLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
        btnThem.setEnabled(true);
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
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
                Toast.makeText(CategoryGradeActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
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
                        .setMessage("Bạn có chắc chắn muốn xóa khối lớp này không?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Xóa", (dialog, which) -> performDeleteBlock(item.getMaKhoiLop()))
                        .show();
            });
        });
        rvKhoiLop.setAdapter(adapter);
    }

    private void performAddBlock() {
        String tenKhoi = edtTenKhoi.getText() != null ? edtTenKhoi.getText().toString().trim() : "";
        
        tilTenKhoi.setError(null);
        if (tenKhoi.isEmpty()) {
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
                    Toast.makeText(CategoryGradeActivity.this, "Thêm thành công", Toast.LENGTH_SHORT).show();
                    edtTenKhoi.setText("");
                    loadBlockList();
                } else {
                    Toast.makeText(CategoryGradeActivity.this, "Khối đã tồn tại hoặc lỗi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                Toast.makeText(CategoryGradeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(CategoryGradeActivity.this, "Xóa thành công", Toast.LENGTH_SHORT).show();
                    loadBlockList();
                } else {
                    Toast.makeText(CategoryGradeActivity.this, "Không thể xóa khối đang có lớp", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                Toast.makeText(CategoryGradeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
