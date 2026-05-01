package com.example.studentmanagementapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditExamTypeActivity extends AppCompatActivity {

    private TextInputEditText edtCurrentTypeName, edtCurrentCoefficient, edtNewCoefficient;
    private TextInputLayout tilNewCoefficient;
    private LinearProgressIndicator progressIndicator;
    private MaterialButton btnSaveChange;
    private String maLoaiKiemTra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_exam_type);

        initViews();
        loadDataFromIntent();

        btnSaveChange.setOnClickListener(v -> performUpdate());
    }

    private void initViews() {
        edtCurrentTypeName = findViewById(R.id.edtCurrentTypeName);
        edtCurrentCoefficient = findViewById(R.id.edtCurrentCoefficient);
        edtNewCoefficient = findViewById(R.id.edtNewCoefficient);
        tilNewCoefficient = findViewById(R.id.tilNewCoefficient);
        progressIndicator = findViewById(R.id.progressIndicator);
        btnSaveChange = findViewById(R.id.btnSaveChange);
        ImageButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        edtNewCoefficient.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilNewCoefficient != null) {
                    tilNewCoefficient.setError(null);
                    tilNewCoefficient.setErrorEnabled(false);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
        if (btnSaveChange != null) btnSaveChange.setEnabled(false);
    }

    private void hideLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
        if (btnSaveChange != null) btnSaveChange.setEnabled(true);
    }

    private void loadDataFromIntent() {
        if (getIntent() != null) {
            maLoaiKiemTra = getIntent().getStringExtra("MaLoaiKiemTra");
            String tenLoai = getIntent().getStringExtra("TenLoaiKiemTra");
            String heSo = getIntent().getStringExtra("HeSo");

            edtCurrentTypeName.setText(tenLoai);
            edtCurrentCoefficient.setText(heSo);
        }
    }

    private void performUpdate() {
        String newHeSoStr = edtNewCoefficient.getText().toString().trim();

        if (TextUtils.isEmpty(newHeSoStr)) {
            if (tilNewCoefficient != null) {
                tilNewCoefficient.setErrorEnabled(true);
                tilNewCoefficient.setError("Vui lòng nhập hệ số mới");
            }
            return;
        }

        double newHeSo;
        try {
            newHeSo = Double.parseDouble(newHeSoStr);
        } catch (NumberFormatException e) {
            if (tilNewCoefficient != null) {
                tilNewCoefficient.setErrorEnabled(true);
                tilNewCoefficient.setError("Hệ số không hợp lệ");
            }
            return;
        }

        if (newHeSo <= 0) {
            if (tilNewCoefficient != null) {
                tilNewCoefficient.setErrorEnabled(true);
                tilNewCoefficient.setError("Hệ số phải lớn hơn 0");
            }
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("HeSo", newHeSo);

        showLoading();
        ApiClient.getApiService().updateTestTypeWeight(maLoaiKiemTra, body).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    new MaterialAlertDialogBuilder(EditExamTypeActivity.this)
                            .setTitle("Thành công")
                            .setMessage("Cập nhật hệ số mới thành công!")
                            .setCancelable(false)
                            .setPositiveButton("Đóng", (dialog, which) -> {
                                setResult(RESULT_OK);
                                finish();
                            })
                            .show();
                } else {
                    String errorMsg = "Lỗi từ hệ thống (" + response.code() + ")";
                    try (ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            JSONObject jObjError = new JSONObject(errorBody.string());
                            errorMsg = jObjError.optString("error", errorMsg);
                        }
                    } catch (Exception ignored) {}
                    
                    new MaterialAlertDialogBuilder(EditExamTypeActivity.this)
                            .setTitle("Thất bại")
                            .setMessage(errorMsg)
                            .setPositiveButton("Đóng", null)
                            .show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                new MaterialAlertDialogBuilder(EditExamTypeActivity.this)
                        .setTitle("Lỗi kết nối")
                        .setMessage("Không thể kết nối đến máy chủ. Vui lòng thử lại sau.")
                        .setPositiveButton("Đóng", null)
                        .show();
            }
        });
    }
}
