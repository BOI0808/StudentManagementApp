package com.example.studentmanagementapp;

import android.content.Intent;
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

public class SystemParametersActivity extends AppCompatActivity {

    private TextInputEditText edtTuoiMin, edtTuoiMax, edtSiSoMin, edtSiSoMax, edtDiemMin, edtDiemMax, edtDiemDatMon, edtDiemDatHK;
    private TextInputLayout tilTuoiMin, tilTuoiMax, tilSiSoMin, tilSiSoMax, tilDiemMin, tilDiemMax, tilDiemDatMon, tilDiemDatHK;
    private MaterialButton btnUpdate;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_parameters);

        initViews();
        setupTextWatchers();
        loadCurrentParameters();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(SystemParametersActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        btnUpdate.setOnClickListener(v -> performUpdate());
    }

    private void initViews() {
        edtTuoiMin = findViewById(R.id.edtTuoiToiThieu);
        edtTuoiMax = findViewById(R.id.edtTuoiToiDa);
        edtSiSoMin = findViewById(R.id.edtSiSoToiThieu);
        edtSiSoMax = findViewById(R.id.edtSiSoToiDa);
        edtDiemMin = findViewById(R.id.edtDiemToiThieu);
        edtDiemMax = findViewById(R.id.edtDiemToiDa);
        edtDiemDatMon = findViewById(R.id.edtDiemDat);
        edtDiemDatHK = findViewById(R.id.edtDiemTBDat);

        tilTuoiMin = findViewById(R.id.tilTuoiToiThieu);
        tilTuoiMax = findViewById(R.id.tilTuoiToiDa);
        tilSiSoMin = findViewById(R.id.tilSiSoToiThieu);
        tilSiSoMax = findViewById(R.id.tilSiSoToiDa);
        tilDiemMin = findViewById(R.id.tilDiemToiThieu);
        tilDiemMax = findViewById(R.id.tilDiemToiDa);
        tilDiemDatMon = findViewById(R.id.tilDiemDat);
        tilDiemDatHK = findViewById(R.id.tilDiemTBDat);

        btnUpdate = findViewById(R.id.btnCapNhatQuyDinh);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);
    }

    private void showLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
        btnUpdate.setEnabled(false);
    }

    private void hideLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
        btnUpdate.setEnabled(true);
    }

    private void setupTextWatchers() {
        addWatcher(edtTuoiMin, tilTuoiMin);
        addWatcher(edtTuoiMax, tilTuoiMax);
        addWatcher(edtSiSoMin, tilSiSoMin);
        addWatcher(edtSiSoMax, tilSiSoMax);
        addWatcher(edtDiemMin, tilDiemMin);
        addWatcher(edtDiemMax, tilDiemMax);
        addWatcher(edtDiemDatMon, tilDiemDatMon);
        addWatcher(edtDiemDatHK, tilDiemDatHK);
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

    private void loadCurrentParameters() {
        showLoading();
        ApiClient.getApiService().getSystemParameters().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> p = response.body();
                    edtTuoiMin.setText(formatValue(p.get("TuoiToiThieu")));
                    edtTuoiMax.setText(formatValue(p.get("TuoiToiDa")));
                    edtSiSoMin.setText(formatValue(p.get("SiSoToiThieu")));
                    edtSiSoMax.setText(formatValue(p.get("SiSoToiDa")));
                    edtDiemMin.setText(formatValue(p.get("DiemToiThieu")));
                    edtDiemMax.setText(formatValue(p.get("DiemToiDa")));
                    edtDiemDatMon.setText(formatValue(p.get("DiemDatMon")));
                    edtDiemDatHK.setText(formatValue(p.get("DiemDat")));
                }
            }
            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                hideLoading();
                showErrorDialog("Lỗi", "Không thể tải quy định hiện tại. Vui lòng thử lại sau.");
            }
        });
    }

    private String formatValue(Object obj) {
        if (obj == null) return "";
        String s = String.valueOf(obj);
        if (s.endsWith(".0")) {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }

    private void performUpdate() {
        boolean isValid = true;
        Map<String, Object> params = new HashMap<>();

        try {
            params.put("TuoiToiThieu", parseInputInt(edtTuoiMin, tilTuoiMin, "Tuổi tối thiểu"));
            params.put("TuoiToiDa", parseInputInt(edtTuoiMax, tilTuoiMax, "Tuổi tối đa"));
            params.put("SiSoToiThieu", parseInputInt(edtSiSoMin, tilSiSoMin, "Sĩ số tối thiểu"));
            params.put("SiSoToiDa", parseInputInt(edtSiSoMax, tilSiSoMax, "Sĩ số tối đa"));
            params.put("DiemToiThieu", parseInputFloat(edtDiemMin, tilDiemMin, "Điểm tối thiểu"));
            params.put("DiemToiDa", parseInputFloat(edtDiemMax, tilDiemMax, "Điểm tối đa"));
            params.put("DiemDatMon", parseInputFloat(edtDiemDatMon, tilDiemDatMon, "Điểm đạt môn"));
            params.put("DiemDat", parseInputFloat(edtDiemDatHK, tilDiemDatHK, "Điểm đạt học kỳ"));
        } catch (Exception e) {
            isValid = false;
        }

        if (!isValid) return;

        showLoading();
        ApiClient.getApiService().updateSystemParameters(params).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    new MaterialAlertDialogBuilder(SystemParametersActivity.this)
                            .setTitle("Thành công")
                            .setMessage("Đã cập nhật quy định hệ thống mới.")
                            .setPositiveButton("OK", (dialog, which) -> loadCurrentParameters())
                            .show();
                } else {
                    String errorMsg = "Dữ liệu không hợp lệ";
                    try (ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            JSONObject jObjError = new JSONObject(errorBody.string());
                            if (jObjError.has("error")) errorMsg = jObjError.getString("error");
                        }
                    } catch (Exception ignored) {}
                    showErrorDialog("Thất bại", errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                hideLoading();
                showErrorDialog("Lỗi kết nối", "Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại mạng.");
            }
        });
    }

    private int parseInputInt(TextInputEditText edt, TextInputLayout til, String label) throws Exception {
        String s = edt.getText().toString().trim();
        if (TextUtils.isEmpty(s)) {
            til.setErrorEnabled(true);
            til.setError("Nhập " + label.toLowerCase());
            throw new Exception();
        }
        try {
            return (int) Float.parseFloat(s);
        } catch (Exception e) {
            til.setErrorEnabled(true);
            til.setError("Số không hợp lệ");
            throw new Exception();
        }
    }

    private float parseInputFloat(TextInputEditText edt, TextInputLayout til, String label) throws Exception {
        String s = edt.getText().toString().trim().replace(',', '.');
        if (TextUtils.isEmpty(s)) {
            til.setErrorEnabled(true);
            til.setError("Nhập " + label.toLowerCase());
            throw new Exception();
        }
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            til.setErrorEnabled(true);
            til.setError("Số không hợp lệ");
            throw new Exception();
        }
    }

    private void showErrorDialog(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đóng", null)
                .show();
    }
}
