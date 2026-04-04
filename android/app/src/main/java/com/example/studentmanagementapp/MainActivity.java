package com.example.studentmanagementapp;

import android.content.Intent;
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

        // Xử lý nút Tiếp nhận học sinh
        CardView cardTiepNhan = findViewById(R.id.cardTiepNhan);
        if (cardTiepNhan != null) {
            cardTiepNhan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, ReceiveStudentsActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Xử lý nút Lập danh sách lớp
        CardView cardDanhSachLop = findViewById(R.id.cardDanhSachLop);
        if (cardDanhSachLop != null) {
            cardDanhSachLop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, CreateClassActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Xử lý nút Lập danh sách học sinh cho lớp
        CardView cardDanhSachHSChoLop = findViewById(R.id.cardDanhSachHSChoLop);
        if (cardDanhSachHSChoLop != null) {
            cardDanhSachHSChoLop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, CreateClassListActivity.class);
                    startActivity(intent);
                }
            });
        }

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
            
            btnLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });

            headerLayout.addView(btnLogout);
        }
    }
}