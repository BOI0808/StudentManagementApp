package com.example.studentmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AdminCreateUserActivity extends AppCompatActivity {

    private TextView btnLogout;
    private TextInputEditText edtFullName, edtUsername, edtPassword, edtEmail, edtPhone;
    private MaterialButton btnCreateAccount, btnXemDanhSach;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_create_user);

        // Ánh xạ các thành phần dựa trên ID trong XML của bạn
        btnLogout = findViewById(R.id.btnLogout);
        edtFullName = findViewById(R.id.edtFullName);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnXemDanhSach = findViewById(R.id.btnXemDanhSach);

        // Xử lý sự kiện Đăng xuất
        if (btnLogout != null) {
            btnLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(AdminCreateUserActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }

        // Xử lý sự kiện Tạo tài khoản và Reset dữ liệu
        if (btnCreateAccount != null) {
            btnCreateAccount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Giả lập lưu dữ liệu thành công
                    Toast.makeText(AdminCreateUserActivity.this, "Đã lưu tài khoản thành công!", Toast.LENGTH_SHORT).show();
                    
                    // Reset mọi thứ đã nhập
                    resetFields();
                }
            });
        }
        
        // Chuyển sang màn hình danh sách khi nhấn nút Xem danh sách
        if (btnXemDanhSach != null) {
            btnXemDanhSach.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(AdminCreateUserActivity.this, AdminUserListActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    private void resetFields() {
        if (edtFullName != null) edtFullName.setText("");
        if (edtUsername != null) edtUsername.setText("");
        if (edtPassword != null) edtPassword.setText("");
        if (edtEmail != null) edtEmail.setText("");
        if (edtPhone != null) edtPhone.setText("");
        
        // Reset tất cả CheckBox trong layout (Phân quyền)
        clearCheckBoxes((ViewGroup) findViewById(android.R.id.content));
    }

    private void clearCheckBoxes(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View view = viewGroup.getChildAt(i);
            if (view instanceof CheckBox) {
                ((CheckBox) view).setChecked(false);
            } else if (view instanceof ViewGroup) {
                clearCheckBoxes((ViewGroup) view);
            }
        }
    }
}