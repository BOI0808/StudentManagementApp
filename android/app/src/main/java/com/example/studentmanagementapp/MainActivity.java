package com.example.studentmanagementapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupFeaturesWithPermissions();

        // Nút Đăng xuất
        LinearLayout headerLayout = findViewById(R.id.headerLayout);
        if (headerLayout != null) {
            TextView btnLogout = new TextView(this);
            btnLogout.setText("Đăng xuất");
            btnLogout.setTextColor(Color.WHITE);
            btnLogout.setTextSize(16);
            btnLogout.setTypeface(null, Typeface.BOLD);
            btnLogout.setPadding(0, 20, 0, 0);
            btnLogout.setClickable(true);
            btnLogout.setFocusable(true);
            
            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });

            headerLayout.addView(btnLogout);
        }
    }

    private void setupFeaturesWithPermissions() {
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        Set<String> permissions = sharedPref.getStringSet("user_permissions", new HashSet<>());

        // 1. Tiếp nhận học sinh
        setupCard(R.id.cardTiepNhan, "CNTNHS", permissions, ReceiveStudentsActivity.class);
        
        // 2. Lập danh sách lớp
        setupCard(R.id.cardDanhSachLop, "CNLDSL", permissions, CreateClassActivity.class);
        
        // 3. Lập danh sách học sinh cho lớp
        setupCard(R.id.cardDanhSachHSChoLop, "CNLDSHSCL", permissions, CreateClassListActivity.class);
        
        // 4. Lập danh sách Năm học
        setupCard(R.id.cardNamHoc, "CNLDSNH", permissions, CategoryTermActivity.class);
        
        // 5. Lập danh sách Khối lớp
        setupCard(R.id.cardKhoiLop, "CNLDSKL", permissions, CategoryGradeActivity.class);
        
        // 6. Lập danh sách Môn học
        setupCard(R.id.cardMonHoc, "CNLDSMH", permissions, CategorySubjectActivity.class);
        
        // 7. Tra cứu học sinh
        setupCard(R.id.cardTraCuu, "CNTCHS", permissions, SearchStudentsActivity.class);
        
        // 8. Nhập bảng điểm
        setupCard(R.id.cardNhapDiem, "CNNBD", permissions, GradeEntryActivity.class);
        
        // 9. Nhập danh sách loại kiểm tra
        setupCard(R.id.cardLoaiHinhKiemTra, "CNNDSCLKT", permissions, ExamTypeManagementActivity.class);
        
        // 10. Lập báo cáo tổng kết môn
        setupCard(R.id.cardBaoCaoMon, "CNLBCTKM", permissions, SubjectReportActivity.class);
        
        // 11. Lập báo cáo tổng kết học kỳ
        setupCard(R.id.cardBaoCaoHocKy, "CNLBCTKHK", permissions, TermReportActivity.class);
        
        // 12. Cài đặt tham số hệ thống
        setupCard(R.id.cardCaiDatThamSo, "CNCDTSHT", permissions, SystemParametersActivity.class);
    }

    private void setupCard(int cardId, String permissionCode, Set<String> userPermissions, Class<?> targetActivity) {
        CardView card = findViewById(cardId);
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
}
