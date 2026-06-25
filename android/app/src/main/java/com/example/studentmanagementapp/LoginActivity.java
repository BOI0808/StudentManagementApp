package com.example.studentmanagementapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
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
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okhttp3.ResponseBody;
import org.json.JSONArray;
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
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilUsername.setError(null);
                tilUsername.setErrorEnabled(false);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        edtPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilPassword.setError(null);
                tilPassword.setErrorEnabled(false);
            }
            @Override public void afterTextChanged(Editable s) {}
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
        tilUsername.setError(null);
        tilPassword.setError(null);

        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

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

                    String accessToken = loginResponse.getAccessToken();
                    String refreshToken = loginResponse.getRefreshToken();
                    LoginResponse.UserData userData = loginResponse.getUser();

                    if (accessToken == null || accessToken.isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Lỗi: Không nhận được token đăng nhập", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveUserData(userData, accessToken, refreshToken);

                    String phanQuyen = (userData != null) ? userData.getPhanQuyen() : "";
                    
                    // Nếu userData null, thử lấy phanQuyen từ JWT payload đã lưu
                    if (phanQuyen.isEmpty()) {
                        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                        phanQuyen = sharedPref.getString("user_phanquyen", "");
                    }

                    Intent intent;
                    if ("Admin".equalsIgnoreCase(phanQuyen) || "Quản trị viên".equalsIgnoreCase(phanQuyen)) {
                        intent = new Intent(LoginActivity.this, AdminCreateUserActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    }

                    startActivity(intent);

                    if (loginResponse.getMessage() != null) {
                        Toast.makeText(LoginActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                    }

                    finish();
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                hideLoading();
                Toast.makeText(LoginActivity.this, "Lỗi kết nối Server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleErrorResponse(Response<LoginResponse> response) {
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

            if (errorMsg.contains("tên đăng nhập hoặc mật khẩu không đúng")) {
                tilUsername.setErrorEnabled(true);
                tilUsername.setError("Tên đăng nhập hoặc mật khẩu không đúng.");
            } else if (errorMsg.contains("tài khoản không tồn tại") || errorMsg.contains("tài khoản sai")) {
                tilUsername.setErrorEnabled(true);
                tilUsername.setError("Tài khoản không tồn tại");
            } else {
                Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + (errorMsg.isEmpty() ? "Lỗi " + response.code() : errorMsg), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserData(LoginResponse.UserData user, String accessToken, String refreshToken) {
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("access_token", accessToken);
        editor.putString("refresh_token", refreshToken);

        if (user != null && user.getMaSo() != null) {
            editor.putString("user_fullname", user.getHoTen());
            editor.putString("user_maso", user.getMaSo());
            editor.putString("user_phanquyen", user.getPhanQuyen());
            
            if (user.getDanhSachQuyen() != null) {
                editor.putStringSet("user_permissions", new HashSet<>(user.getDanhSachQuyen()));
            } else {
                editor.remove("user_permissions");
            }
        } else {
            // Fallback decoding from JWT
            try {
                String[] parts = accessToken.split("\\.");
                if (parts.length >= 2) {
                    String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
                    JSONObject json = new JSONObject(payload);
                    editor.putString("user_fullname", json.optString("HoTen", ""));
                    editor.putString("user_maso", json.optString("MaSo", ""));
                    String pq = json.optString("PhanQuyen", "");
                    editor.putString("user_phanquyen", pq);
                    
                    if (json.has("DanhSachQuyen")) {
                        JSONArray array = json.getJSONArray("DanhSachQuyen");
                        Set<String> perms = new HashSet<>();
                        for (int i = 0; i < array.length(); i++) {
                            perms.add(array.getString(i));
                        }
                        editor.putStringSet("user_permissions", perms);
                    } else {
                        editor.remove("user_permissions");
                    }
                }
            } catch (Exception e) {
                Log.e("JWT", "Error decoding token", e);
            }
        }
        
        editor.apply();
    }
}
