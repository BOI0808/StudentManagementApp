package com.example.studentmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
    
    private List<User> originalUserList = new ArrayList<>();
    private List<User> filteredUserList = new ArrayList<>();
    private GenericAdapter<User> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_list);

        initViews();
        setupListeners();
        loadUserList();
    }

    private void initViews() {
        rvUserList = findViewById(R.id.rvDanhSachNhanVien);
        btnBack = findViewById(R.id.btnBack);
        btnHelpRights = findViewById(R.id.btnHelpRights);
        btnAddUser = findViewById(R.id.btnLuuDanhSach);
        edtSearch = findViewById(R.id.edtSearchUser);

        rvUserList.setLayoutManager(new LinearLayoutManager(this));
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
                filterUserList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUserList() {
        ApiClient.getApiService().getAccountList().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    originalUserList = response.body();
                    filteredUserList.clear();
                    filteredUserList.addAll(originalUserList);
                    setupAdapter();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(AdminUserListActivity.this, "Lỗi tải danh sách", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterUserList(String query) {
        filteredUserList.clear();
        if (query.isEmpty()) {
            filteredUserList.addAll(originalUserList);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (User user : originalUserList) {
                String name = (user.getHoTen() != null) ? user.getHoTen().toLowerCase() : "";
                String code = (user.getMaSo() != null) ? user.getMaSo().toLowerCase() : "";
                if (name.contains(lowerCaseQuery) || code.contains(lowerCaseQuery)) {
                    filteredUserList.add(user);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void setupAdapter() {
        adapter = new GenericAdapter<>(filteredUserList, R.layout.item_admin_user_row, (user, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvMaNV)).setText(user.getMaSo() != null ? user.getMaSo() : "");
            ((TextView) itemView.findViewById(R.id.tvHoTen)).setText(user.getHoTen() != null ? user.getHoTen() : "");
            ((TextView) itemView.findViewById(R.id.tvSoDienThoai)).setText(user.getSoDienThoai() != null ? user.getSoDienThoai() : "");
            ((TextView) itemView.findViewById(R.id.tvTaiKhoan)).setText(user.getTenDangNhap() != null ? user.getTenDangNhap() : "");
            ((TextView) itemView.findViewById(R.id.tvMatKhau)).setText(user.getMatKhau() != null ? user.getMatKhau() : "");
            
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

    private void confirmDeleteUser(User user) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận")
                .setMessage("Bạn có chắc muốn ngưng hoạt động tài khoản " + user.getHoTen() + "?")
                .setPositiveButton("Ngưng hoạt động", (dialog, which) -> {
                    ApiClient.getApiService().deleteAccount(user.getMaSo()).enqueue(new Callback<Map<String, String>>() {
                        @Override
                        public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminUserListActivity.this, "Đã ngưng hoạt động tài khoản", Toast.LENGTH_SHORT).show();
                                loadUserList();
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, String>> call, Throwable t) {
                            Toast.makeText(AdminUserListActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showRightsHelpDialog() {
        String helpMessage = "Tiếp nhận học sinh (1)\n" +
                "Lập danh sách lớp (2)\n" +
                "Lập danh sách học sinh cho lớp (3)\n" +
                "Lập danh sách Năm học (4)\n" +
                "Lập danh sách Khối lớp (5)\n" +
                "Lập danh sách Môn học (6)\n" +
                "Tra cứu học sinh (7)\n" +
                "Nhập bảng điểm (8)\n" +
                "Nhập danh sách loại kiểm tra (9)\n" +
                "Lập báo cáo tổng kết môn (10)\n" +
                "Lập báo cáo tổng kết học kỳ (11)\n" +
                "Cài đặt tham số hệ thống (12)";
        new MaterialAlertDialogBuilder(this)
                .setTitle("Phân quyền hệ thống")
                .setMessage(helpMessage)
                .setPositiveButton("Đã hiểu", null)
                .show();
    }
}
