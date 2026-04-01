package com.example.studentmanagementapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText edtUsername, edtOldPassword, edtNewPassword, edtConfirmPassword;
    private Button btnCancel, btnSavePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password); // Gắn layout bạn vừa tạo

        // 1. Ánh xạ View
        edtUsername = findViewById(R.id.edtUsernameChangePass);
        edtOldPassword = findViewById(R.id.edtOldPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnCancel = findViewById(R.id.btnCancel);
        btnSavePassword = findViewById(R.id.btnSavePassword);

        // 2. Bắt sự kiện nút Hủy -> Đóng màn hình này, tự động quay lại Login
        btnCancel.setOnClickListener(v -> finish());

        // 3. Bắt sự kiện nút Xác nhận
        btnSavePassword.setOnClickListener(v -> handleChangePassword());
    }

    private void handleChangePassword() {
        String username = edtUsername.getText().toString().trim();
        String oldPass = edtOldPassword.getText().toString().trim();
        String newPass = edtNewPassword.getText().toString().trim();
        String confirmPass = edtConfirmPassword.getText().toString().trim();

        // Kiểm tra rỗng
        if (username.isEmpty() || oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra mật khẩu mới và xác nhận có khớp không
        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Sau này Giang sẽ gọi API Backend ở đây để đổi pass thật trong Database

        Toast.makeText(this, "Đổi mật khẩu thành công (Demo)!", Toast.LENGTH_SHORT).show();
        finish(); // Đổi xong thì đóng màn hình, quay lại Login
    }
}