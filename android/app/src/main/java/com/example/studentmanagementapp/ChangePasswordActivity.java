package com.example.studentmanagementapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText edtUsername, edtOldPassword, edtNewPassword, edtConfirmPassword;
    private TextInputLayout tilUsername, tilOldPassword, tilNewPassword, tilConfirmPassword;
    private MaterialButton btnSavePassword;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        initViews();
        setupTextWatchers();

        btnBack.setOnClickListener(v -> finish());
        btnSavePassword.setOnClickListener(v -> performChangePassword());
    }

    private void initViews() {
        edtUsername = findViewById(R.id.edtUsernameChangePass);
        edtOldPassword = findViewById(R.id.edtOldPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        tilUsername = findViewById(R.id.tilUsername);
        tilOldPassword = findViewById(R.id.tilOldPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        btnSavePassword = findViewById(R.id.btnSavePassword);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);
    }

    private void showLoading() {
        progressIndicator.setVisibility(View.VISIBLE);
        btnSavePassword.setEnabled(false);
    }

    private void hideLoading() {
        progressIndicator.setVisibility(View.GONE);
        btnSavePassword.setEnabled(true);
    }

    private void setupTextWatchers() {
        addWatcher(edtUsername, tilUsername);
        addWatcher(edtOldPassword, tilOldPassword);
        addWatcher(edtNewPassword, tilNewPassword);
        addWatcher(edtConfirmPassword, tilConfirmPassword);
    }

    private void addWatcher(TextInputEditText edt, TextInputLayout til) {
        edt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                til.setError(null);
                til.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performChangePassword() {
        String username = edtUsername.getText().toString().trim();
        String oldPass = edtOldPassword.getText().toString().trim();
        String newPass = edtNewPassword.getText().toString().trim();
        String confirmPass = edtConfirmPassword.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(username)) {
            tilUsername.setError("Vui lòng nhập tên tài khoản");
            isValid = false;
        }
        if (TextUtils.isEmpty(oldPass)) {
            tilOldPassword.setError("Vui lòng nhập mật khẩu cũ");
            isValid = false;
        }
        if (TextUtils.isEmpty(newPass)) {
            tilNewPassword.setError("Vui lòng nhập mật khẩu mới");
            isValid = false;
        } else if (newPass.length() < 6) {
            tilNewPassword.setError("Mật khẩu mới phải có ít nhất 6 ký tự");
            isValid = false;
        }
        if (TextUtils.isEmpty(confirmPass)) {
            tilConfirmPassword.setError("Vui lòng xác nhận mật khẩu mới");
            isValid = false;
        } else if (!newPass.equals(confirmPass)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            isValid = false;
        }

        if (!isValid) return;

        Map<String, String> data = new HashMap<>();
        data.put("TenDangNhap", username);
        data.put("MatKhauCu", oldPass);
        data.put("MatKhauMoi", newPass);
        data.put("XacNhanMatKhau", confirmPass);

        showLoading();
        ApiClient.getApiService().changePassword(data).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    new MaterialAlertDialogBuilder(ChangePasswordActivity.this)
                            .setTitle("Thành công")
                            .setMessage("Đã đổi mật khẩu thành công.")
                            .setPositiveButton("OK", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                } else {
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

                        if (errorMsg.contains("tài khoản") || errorMsg.contains("không tồn tại")) {
                            tilUsername.setErrorEnabled(true);
                            tilUsername.setError("Tài khoản không chính xác hoặc không tồn tại");
                        } else if (errorMsg.contains("mật khẩu cũ") || errorMsg.contains("không đúng")) {
                            tilOldPassword.setErrorEnabled(true);
                            tilOldPassword.setError("Mật khẩu cũ không chính xác");
                        } else {
                            new MaterialAlertDialogBuilder(ChangePasswordActivity.this)
                                    .setTitle("Thất bại")
                                    .setMessage(!errorMsg.isEmpty() ? errorMsg : "Không thể đổi mật khẩu. Vui lòng thử lại sau.")
                                    .setPositiveButton("Đóng", null)
                                    .show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                new MaterialAlertDialogBuilder(ChangePasswordActivity.this)
                        .setTitle("Lỗi kết nối")
                        .setMessage("Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại mạng.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }
}
