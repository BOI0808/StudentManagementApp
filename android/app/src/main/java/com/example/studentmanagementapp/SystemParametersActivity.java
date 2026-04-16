package com.example.studentmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SystemParametersActivity extends AppCompatActivity {

    private TextInputEditText edtTuoiMin, edtTuoiMax, edtSiSoMin, edtSiSoMax, edtDiemMin, edtDiemMax, edtDiemDatMon, edtDiemDatHK;
    private MaterialButton btnUpdate;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_parameters);

        initViews();
        loadCurrentParameters();

        // Cài đặt sự kiện nút quay lại để quay về màn hình chính
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
        btnUpdate = findViewById(R.id.btnCapNhatQuyDinh);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadCurrentParameters() {
        ApiClient.getApiService().getSystemParameters().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
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
                Toast.makeText(SystemParametersActivity.this, "Không thể tải quy định hiện tại", Toast.LENGTH_SHORT).show();
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
        Map<String, Object> params = new HashMap<>();
        try {
            params.put("TuoiToiThieu", parseSafeInt(edtTuoiMin, "Tuổi tối thiểu"));
            params.put("TuoiToiDa", parseSafeInt(edtTuoiMax, "Tuổi tối đa"));
            params.put("SiSoToiThieu", parseSafeInt(edtSiSoMin, "Sĩ số tối thiểu"));
            params.put("SiSoToiDa", parseSafeInt(edtSiSoMax, "Sĩ số tối đa"));
            params.put("DiemToiThieu", parseSafeFloat(edtDiemMin, "Điểm tối thiểu"));
            params.put("DiemToiDa", parseSafeFloat(edtDiemMax, "Điểm tối đa"));
            params.put("DiemDatMon", parseSafeFloat(edtDiemDatMon, "Điểm đạt môn"));
            params.put("DiemDat", parseSafeFloat(edtDiemDatHK, "Điểm đạt học kỳ"));
        } catch (NumberFormatException e) {
            return;
        }

        btnUpdate.setEnabled(false);
        ApiClient.getApiService().updateSystemParameters(params).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                btnUpdate.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(SystemParametersActivity.this, "Cập nhật quy định thành công!", Toast.LENGTH_SHORT).show();
                    loadCurrentParameters();
                } else {
                    try (ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            String errorContent = errorBody.string();
                            JSONObject jObjError = new JSONObject(errorContent);
                            String errorMsg = jObjError.has("error") ? jObjError.getString("error") : "Dữ liệu không hợp lệ";
                            Toast.makeText(SystemParametersActivity.this, "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(SystemParametersActivity.this, "Lỗi Server", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                btnUpdate.setEnabled(true);
                Toast.makeText(SystemParametersActivity.this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int parseSafeInt(TextInputEditText edt, String label) {
        String s = edt.getText().toString().trim();
        if (TextUtils.isEmpty(s)) {
            Toast.makeText(this, "Vui lòng nhập " + label, Toast.LENGTH_SHORT).show();
            throw new NumberFormatException();
        }
        try {
            if (s.contains(".")) {
                return (int) Float.parseFloat(s);
            }
            return Integer.parseInt(s);
        } catch (Exception e) {
            Toast.makeText(this, label + " không đúng định dạng số", Toast.LENGTH_SHORT).show();
            throw new NumberFormatException();
        }
    }

    private float parseSafeFloat(TextInputEditText edt, String label) {
        String s = edt.getText().toString().trim().replace(',', '.');
        if (TextUtils.isEmpty(s)) {
            Toast.makeText(this, "Vui lòng nhập " + label, Toast.LENGTH_SHORT).show();
            throw new NumberFormatException();
        }
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            Toast.makeText(this, label + " không đúng định dạng số", Toast.LENGTH_SHORT).show();
            throw new NumberFormatException();
        }
    }
}
