package com.example.studentmanagementapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.LoginResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtUsername;
    private TextInputEditText edtPassword;
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

        edtPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                performLogin();
                return true;
            }
            return false;
        });

        btnLogin.setOnClickListener(v -> performLogin());

        tvGoToChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> loginData = new HashMap<>();
        loginData.put("TenDangNhap", username);
        loginData.put("MatKhau", password);

        btnLogin.setEnabled(false);

        ApiClient.getApiService().login(loginData).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    
                    // LƯU QUYỀN VÀO SHAREDPREFERENCES
                    saveUserPermissions(loginResponse);

                    String phanQuyen = loginResponse.getUser().getPhanQuyen();
                    
                    if (phanQuyen.equalsIgnoreCase("Admin") || phanQuyen.equalsIgnoreCase("Quản trị viên")) {
                        Intent intent = new Intent(LoginActivity.this, AdminCreateUserActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    
                    Toast.makeText(LoginActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Lỗi kết nối Server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserPermissions(LoginResponse response) {
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        
        List<String> permissions = response.getUser().getQuyen();
        // Chuyển List thành Set để lưu vào SharedPreferences
        Set<String> set = new HashSet<>(permissions);
        editor.putStringSet("user_permissions", set);
        editor.apply();
    }
}
