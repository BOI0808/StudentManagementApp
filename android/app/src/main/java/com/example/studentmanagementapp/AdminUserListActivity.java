package com.example.studentmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserListActivity extends AppCompatActivity {

    private RecyclerView rvUserList;
    private ImageButton btnBack, btnHelpRights;
    private MaterialButton btnAddUser;
    private TextInputEditText edtSearch;
    private LinearProgressIndicator progressIndicator;
    private ProgressBar pbSearchLoading;
    private LinearLayout layoutEmpty;
    
    private final List<User> originalUserList = new ArrayList<>();
    private final List<User> filteredUserList = new ArrayList<>();
    private GenericAdapter<User> adapter;

    // Debouncing cho tìm kiếm
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_list);

        initViews();
        setupSingletonAdapter();
        setupListeners();
        loadUserList();
    }

    private void initViews() {
        rvUserList = findViewById(R.id.rvDanhSachNhanVien);
        btnBack = findViewById(R.id.btnBack);
        btnHelpRights = findViewById(R.id.btnHelpRights);
        btnAddUser = findViewById(R.id.btnLuuDanhSach);
        edtSearch = findViewById(R.id.edtSearchUser);
        progressIndicator = findViewById(R.id.progressIndicator);
        pbSearchLoading = findViewById(R.id.pbSearchLoading);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        rvUserList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupSingletonAdapter() {
        // Khởi tạo adapter một lần duy nhất với filteredUserList là nguồn dữ liệu
        adapter = new GenericAdapter<>(filteredUserList, R.layout.item_admin_user_row, (user, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvMaNV)).setText(user.getMaSo() != null ? user.getMaSo() : "");
            ((TextView) itemView.findViewById(R.id.tvHoTen)).setText(user.getHoTen() != null ? user.getHoTen() : "");
            ((TextView) itemView.findViewById(R.id.tvSoDienThoai)).setText(user.getSoDienThoai() != null ? user.getSoDienThoai() : "");
            ((TextView) itemView.findViewById(R.id.tvTaiKhoan)).setText(user.getTenDangNhap() != null ? user.getTenDangNhap() : "");
            
            // Bảo mật: Luôn hiển thị chuỗi cố định cho mật khẩu
            ((TextView) itemView.findViewById(R.id.tvMatKhau)).setText("••••••");
            
            String rightsDisplay = user.getQuyenHeThong() != null ? user.getQuyenHeThong() : "-";
            ((TextView) itemView.findViewById(R.id.tvQuyen)).setText(rightsDisplay);
            
            itemView.findViewById(R.id.btnEdit).setOnClickListener(v -> {
                Intent intent = new Intent(AdminUserListActivity.this, AdminCreateUserActivity.class);
                intent.putExtra("user_data", user);
                startActivity(intent);
            });

            itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDeleteUser(user));
        });
        rvUserList.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnHelpRights.setOnClickListener(v -> showRightsHelpDialog());
        btnAddUser.setOnClickListener(v -> {
            Intent intent = new Intent(AdminUserListActivity.this, AdminCreateUserActivity.class);
            startActivity(intent);
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Hiển thị loading tìm kiếm ngay khi gõ
                if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.VISIBLE);
                
                // Kỹ thuật Debouncing: Chỉ lọc sau khi người dùng ngừng gõ 300ms
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> filterUserList(s.toString());
                searchHandler.postDelayed(searchRunnable, 300);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
        if (btnAddUser != null) btnAddUser.setEnabled(false);
    }

    private void hideLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
        if (btnAddUser != null) btnAddUser.setEnabled(true);
    }

    private void loadUserList() {
        showLoading();
        ApiClient.getApiService().getAccountList().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    originalUserList.clear();
                    originalUserList.addAll(response.body());
                    filterUserList(edtSearch.getText() != null ? edtSearch.getText().toString() : "");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                hideLoading();
                Toast.makeText(AdminUserListActivity.this, "Lỗi tải danh sách", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterUserList(String query) {
        filteredUserList.clear();
        String lowerCaseQuery = query.trim().toLowerCase();

        if (lowerCaseQuery.isEmpty()) {
            filteredUserList.addAll(originalUserList);
        } else {
            for (User user : originalUserList) {
                String name = (user.getHoTen() != null) ? user.getHoTen().toLowerCase() : "";
                String code = (user.getMaSo() != null) ? user.getMaSo().toLowerCase() : "";
                if (name.contains(lowerCaseQuery) || code.contains(lowerCaseQuery)) {
                    filteredUserList.add(user);
                }
            }
        }

        // Cập nhật giao diện Empty State
        if (filteredUserList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvUserList.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvUserList.setVisibility(View.VISIBLE);
        }
        
        // Cập nhật adapter thông minh
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        // Ẩn loading tìm kiếm sau khi hoàn thành
        if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
    }

    private void confirmDeleteUser(User user) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận")
                .setMessage("Bạn có chắc muốn ngưng hoạt động tài khoản " + user.getHoTen() + "?")
                .setPositiveButton("Ngưng hoạt động", (dialog, which) -> {
                    showLoading();
                    ApiClient.getApiService().deleteAccount(user.getMaSo()).enqueue(new Callback<Map<String, String>>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                            hideLoading();
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminUserListActivity.this, "Đã ngưng hoạt động tài khoản", Toast.LENGTH_SHORT).show();
                                loadUserList();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                            hideLoading();
                            Toast.makeText(AdminUserListActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showRightsHelpDialog() {
        String helpMessage = "1: Tiếp nhận HS\n2: Lập lớp\n3: Xếp lớp\n4: Danh mục Học kỳ\n5: Danh mục Khối\n" +
                "6: Danh mục Môn\n7: Tra cứu HS\n8: Nhập điểm\n9: Loại kiểm tra\n" +
                "10: BC Tổng kết môn\n11: BC Tổng kết học kỳ\n12: Tham số hệ thống";
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hướng dẫn phân quyền")
                .setMessage(helpMessage)
                .setPositiveButton("Đã hiểu", null)
                .show();
    }
}
