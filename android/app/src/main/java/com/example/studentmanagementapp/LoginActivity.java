package com.example.studentmanagementapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.LoginResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtUsername;
    private TextInputEditText edtPassword;
    private TextInputLayout tilUsername;
    private TextInputLayout tilPassword;
    private MaterialButton btnLogin;
    private TextView tvGoToChangePassword;
    private LinearProgressIndicator loginProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupTextWatchers();

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

    private void initViews() {
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToChangePassword = findViewById(R.id.tvGoToChangePassword);
        loginProgress = findViewById(R.id.loginProgress);
    }

    private void setupTextWatchers() {
        edtUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilUsername.setError(null);
                tilUsername.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        edtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilPassword.setError(null);
                tilPassword.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showLoading() {
        loginProgress.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
    }

    private void hideLoading() {
        loginProgress.setVisibility(View.GONE);
        btnLogin.setEnabled(true);
    }

    private void performLogin() {
        // Dọn dẹp UI: Reset lỗi của cả 2 ô khi bắt đầu nhấn Đăng nhập
        tilUsername.setError(null);
        tilPassword.setError(null);

        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Validation tuần tự
        if (username.isEmpty()) {
            tilUsername.setErrorEnabled(true);
            tilUsername.setError("Vui lòng nhập tài khoản");
            return;
        }

        if (password.isEmpty()) {
            tilPassword.setErrorEnabled(true);
            tilPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }

        Map<String, String> loginData = new HashMap<>();
        loginData.put("TenDangNhap", username);
        loginData.put("MatKhau", password);

        showLoading();

        ApiClient.getApiService().login(loginData).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                hideLoading();
                
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
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
                    // Xử lý lỗi từ Server
                    try {
                        String errorJson = "";
                        try (ResponseBody errorBody = response.errorBody()) {
                            if (errorBody != null) {
                                errorJson = errorBody.string();
                            }
                        }

                        String errorMsg = "";
                        if (!errorJson.isEmpty()) {
                            JSONObject jObjError = new JSONObject(errorJson);
                            errorMsg = jObjError.optString("error", jObjError.optString("message", "")).toLowerCase();
                        }

                        // Logic báo lỗi chi tiết theo từ khóa
                        if (errorMsg.contains("không tồn tại") || errorMsg.contains("không tìm thấy") || errorMsg.contains("tài khoản sai")) {
                            tilUsername.setErrorEnabled(true);
                            tilUsername.setError("Tài khoản không tồn tại");
                            tilPassword.setError(null); // Xóa lỗi ô còn lại
                        } else if (errorMsg.contains("mật khẩu")) {
                            tilPassword.setErrorEnabled(true);
                            tilPassword.setError("Mật khẩu không chính xác");
                            tilUsername.setError(null); // Xóa lỗi ô còn lại
                        } else {
                            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                hideLoading();
                Toast.makeText(LoginActivity.this, "Lỗi kết nối Server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserPermissions(LoginResponse response) {
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        
        List<String> permissions = response.getUser().getQuyen();
        Set<String> set = new HashSet<>(permissions);
        editor.putStringSet("user_permissions", set);
        editor.apply();
    }
}
