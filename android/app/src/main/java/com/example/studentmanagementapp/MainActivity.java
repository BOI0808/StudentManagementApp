package com.example.studentmanagementapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TextView tvUserName;
    private ImageButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        displayUserInfo();
        setupFeaturesWithPermissions();

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void displayUserInfo() {
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String fullName = sharedPref.getString("user_fullname", "Người dùng");
        tvUserName.setText(fullName);
    }

    private void setupFeaturesWithPermissions() {
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        Set<String> permissions = sharedPref.getStringSet("user_permissions", new HashSet<>());

        setupCard(R.id.cardTiepNhan, "CNTNHS", permissions, ReceiveStudentsActivity.class);
        setupCard(R.id.cardDanhSachLop, "CNLDSL", permissions, CreateClassActivity.class);
        setupCard(R.id.cardDanhSachHSChoLop, "CNLDSHSCL", permissions, CreateClassListActivity.class);
        setupCard(R.id.cardTraCuu, "CNTCHS", permissions, SearchStudentsActivity.class);
        setupCard(R.id.cardNhapDiem, "CNNBD", permissions, GradeEntryActivity.class);

        setupCard(R.id.cardBaoCaoMon, "CNLBCTKM", permissions, SubjectReportActivity.class);
        setupCard(R.id.cardBaoCaoHocKy, "CNLBCTKHK", permissions, TermReportActivity.class);

        setupCard(R.id.cardNamHoc, "CNLDSNH", permissions, CategoryTermActivity.class);
        setupCard(R.id.cardKhoiLop, "CNLDSKL", permissions, CategoryGradeActivity.class);
        setupCard(R.id.cardMonHoc, "CNLDSMH", permissions, CategorySubjectActivity.class);
        setupCard(R.id.cardLoaiHinhKiemTra, "CNNDSCLKT", permissions, ExamTypeManagementActivity.class);
        setupCard(R.id.cardCaiDatThamSo, "CNCDTSHT", permissions, SystemParametersActivity.class);
    }

    private void setupCard(int cardId, String permissionCode, Set<String> userPermissions, Class<?> targetActivity) {
        MaterialCardView card = findViewById(cardId);
        if (card != null) {
            if (userPermissions.contains(permissionCode)) {
                card.setVisibility(View.VISIBLE);
                card.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, targetActivity);
                    startActivity(intent);
                });
            } else {
                card.setVisibility(View.GONE);
            }
        }
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất", (dialog, which) -> performLogout())
                .show();
    }

    private void performLogout() {
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        sharedPref.edit().clear().apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
