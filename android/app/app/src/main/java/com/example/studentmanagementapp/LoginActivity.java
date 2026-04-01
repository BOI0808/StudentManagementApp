package com.example.studentmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtUsername, edtPassword;
    private MaterialButton btnLogin;
    private TextView tvGoToChangePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToChangePassword = findViewById(R.id.tvGoToChangePassword);

        // Xử lý khi nhấn nút Đăng nhập
        btnLogin.setOnClickListener(v -> performLogin());

        // Xử lý khi nhấn Enter từ bàn phím tại ô Mật khẩu
        edtPassword.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                performLogin();
                return true;
            }
            return false;
        });

        // Xử lý khi nhấn "Đổi mật khẩu"
        tvGoToChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Logic phân quyền đăng nhập
        if (username.equalsIgnoreCase("admin") && password.equals("admin")) {
            // Đăng nhập với tư cách Admin
            Intent intent = new Intent(LoginActivity.this, AdminCreateUserActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Đăng nhập với tư cách Staff/User thông thường
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}