package com.example.studentmanagementapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AdminUserListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_list);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); // Quay lại màn hình trước đó
                }
            });
        }

        // Ánh xạ nút hình chữ "i" (btnHelpRights)
        ImageButton btnHelpRights = findViewById(R.id.btnHelpRights);
        if (btnHelpRights != null) {
            btnHelpRights.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showRightsHelpDialog();
                }
            });
        }
    }

    private void showRightsHelpDialog() {
        String helpMessage = "1: Tiếp nhận học sinh\n" +
                "2: Lập danh sách lớp\n" +
                "3: Lập danh sách học sinh cho lớp\n" +
                "4: Lập danh sách Năm học\n" +
                "5: Lập danh sách Khối lớp\n" +
                "6: Lập danh sách Môn học\n" +
                "7: Tra cứu học sinh\n" +
                "8: Nhập bảng điểm\n" +
                "9: Nhập danh sách loại kiểm tra\n" +
                "10: Lập báo cáo tổng kết môn\n" +
                "11: Lập báo cáo tổng kết học kỳ\n" +
                "12: Cài đặt tham số hệ thống";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Danh sách mã quyền hệ thống")
                .setMessage(helpMessage)
                .setPositiveButton("Đã hiểu", null)
                .show();
    }
}