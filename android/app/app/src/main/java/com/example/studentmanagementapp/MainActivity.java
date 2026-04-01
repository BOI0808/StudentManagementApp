package com.example.studentmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private ImageButton btnLogout;
    private CardView cardTiepNhan, cardDanhMuc, cardTraCuu, cardLoaiHinhKiemTra, cardBaoCaoMon, cardBaoCaoHocKy, cardNhapDiem;

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

        initViews();
        setEvents();
    }

    private void initViews() {
        btnLogout = findViewById(R.id.btnLogout);
        cardTiepNhan = findViewById(R.id.cardTiepNhan);
        cardDanhMuc = findViewById(R.id.cardDanhMuc);
        cardTraCuu = findViewById(R.id.cardTraCuu);
        cardLoaiHinhKiemTra = findViewById(R.id.cardLoaiHinhKiemTra);
        cardBaoCaoMon = findViewById(R.id.cardBaoCaoMon);
        cardBaoCaoHocKy = findViewById(R.id.cardBaoCaoHocKy);
        cardNhapDiem = findViewById(R.id.cardNhapDiem);
    }

    private void setEvents() {
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        cardTiepNhan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReceiveStudentsActivity.class);
            startActivity(intent);
        });

        cardDanhMuc.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CategoryManagementActivity.class);
            startActivity(intent);
        });

        cardTraCuu.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchStudentsActivity.class);
            startActivity(intent);
        });

        cardLoaiHinhKiemTra.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ExamTypeManagementActivity.class);
            startActivity(intent);
        });

        cardBaoCaoMon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SubjectReportActivity.class);
            startActivity(intent);
        });

        cardBaoCaoHocKy.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TermReportActivity.class);
            startActivity(intent);
        });

        cardNhapDiem.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GradeEntryActivity.class);
            startActivity(intent);
        });
    }
}