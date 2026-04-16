package com.example.studentmanagementapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
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

public class EditExamTypeActivity extends AppCompatActivity {

    private TextInputEditText edtCurrentTypeName, edtCurrentCoefficient, edtNewCoefficient;
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
        btnSaveChange = findViewById(R.id.btnSaveChange);
        ImageButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
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
            Toast.makeText(this, "Vui lòng nhập hệ số mới", Toast.LENGTH_SHORT).show();
            return;
        }

        double newHeSo;
        try {
            newHeSo = Double.parseDouble(newHeSoStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Hệ số không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newHeSo <= 0) {
            Toast.makeText(this, "Hệ số phải lớn hơn 0", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("HeSo", newHeSo);

        btnSaveChange.setEnabled(false);
        ApiClient.getApiService().updateTestTypeWeight(maLoaiKiemTra, body).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                btnSaveChange.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(EditExamTypeActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EditExamTypeActivity.this, "Lỗi: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                btnSaveChange.setEnabled(true);
                Toast.makeText(EditExamTypeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
