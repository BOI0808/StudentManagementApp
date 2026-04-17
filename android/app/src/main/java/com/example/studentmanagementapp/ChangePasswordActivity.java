package com.example.studentmanagementapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText edtUsername;
    private TextInputEditText edtOldPassword;
    private TextInputEditText edtNewPassword;
    private TextInputEditText edtConfirmPassword;
    private MaterialButton btnCancel;
    private MaterialButton btnSavePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        edtUsername = findViewById(R.id.edtUsernameChangePass);
        edtOldPassword = findViewById(R.id.edtOldPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnCancel = findViewById(R.id.btnCancel);
        btnSavePassword = findViewById(R.id.btnSavePassword);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Quay về màn hình trước đó
                finish();
            }
        });

        btnSavePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performChangePassword();
            }
        });
    }

    private void performChangePassword() {
        String username = edtUsername.getText().toString().trim();
        String oldPass = edtOldPassword.getText().toString().trim();
        String newPass = edtNewPassword.getText().toString().trim();
        String confirmPass = edtConfirmPassword.getText().toString().trim();

        if (username.isEmpty() || oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(ChangePasswordActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(ChangePasswordActivity.this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(ChangePasswordActivity.this, "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("TenDangNhap", username);
        data.put("MatKhauCu", oldPass);
        data.put("MatKhauMoi", newPass);
        data.put("XacNhanMatKhau", confirmPass);

        btnSavePassword.setEnabled(false);
        ApiClient.getApiService().changePassword(data).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                btnSavePassword.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(ChangePasswordActivity.this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMsg = "Lỗi đổi mật khẩu";
                    try {
                        if (response.errorBody() != null) {
                            android.util.Log.e("ChangePass", "Error body: " + response.errorBody().string());
                            // Bạn có thể parse JSON để lấy message lỗi cụ thể từ backend ở đây nếu cần
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(ChangePasswordActivity.this, "Lỗi: Mật khẩu cũ không đúng hoặc tài khoản sai", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                btnSavePassword.setEnabled(true);
                Toast.makeText(ChangePasswordActivity.this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
