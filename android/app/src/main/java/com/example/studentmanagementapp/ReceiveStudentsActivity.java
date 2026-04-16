package com.example.studentmanagementapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.Student;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReceiveStudentsActivity extends AppCompatActivity {

    private TextInputEditText edtHoTen, edtNgaySinh, edtDiaChi, edtEmail, edtGioiTinhKhac;
    private RadioGroup rgGioiTinh;
    private RadioButton rbNam, rbNu, rbKhac;
    private TextInputLayout tilGioiTinhKhac;
    private MaterialButton btnTiepNhan, btnSuaHoSo;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_students);

        initViews();
        setupGenderLogic();

        edtNgaySinh.setOnClickListener(v -> showDatePicker());
        btnBack.setOnClickListener(v -> finish());
        btnTiepNhan.setOnClickListener(v -> performSaveStudent());
        
        btnSuaHoSo.setOnClickListener(v -> {
            Intent intent = new Intent(ReceiveStudentsActivity.this, EditStudentActivity.class);
            startActivity(intent);
        });
    }

    private void initViews() {
        edtHoTen = findViewById(R.id.edtHoTen);
        edtNgaySinh = findViewById(R.id.edtNgaySinh);
        edtDiaChi = findViewById(R.id.edtDiaChi);
        edtEmail = findViewById(R.id.edtEmail);
        edtGioiTinhKhac = findViewById(R.id.edtGioiTinhKhac);
        rgGioiTinh = findViewById(R.id.rgGioiTinh);
        rbNam = findViewById(R.id.rbNam);
        rbNu = findViewById(R.id.rbNu);
        rbKhac = findViewById(R.id.rbKhac);
        tilGioiTinhKhac = findViewById(R.id.tilGioiTinhKhac);
        btnTiepNhan = findViewById(R.id.btnTiepNhan);
        btnSuaHoSo = findViewById(R.id.btnSuaHoSo);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupGenderLogic() {
        rgGioiTinh.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbKhac) {
                tilGioiTinhKhac.setVisibility(View.VISIBLE);
            } else {
                tilGioiTinhKhac.setVisibility(View.GONE);
                edtGioiTinhKhac.setText("");
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
            edtNgaySinh.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void performSaveStudent() {
        String hoTen = (edtHoTen.getText() != null) ? edtHoTen.getText().toString().trim() : "";
        String ngaySinh = (edtNgaySinh.getText() != null) ? edtNgaySinh.getText().toString().trim() : "";
        String diaChi = (edtDiaChi.getText() != null) ? edtDiaChi.getText().toString().trim() : "";
        String email = (edtEmail.getText() != null) ? edtEmail.getText().toString().trim() : "";
        
        String maGioiTinh = "GT1"; // Mặc định Nam
        if (rbNu.isChecked()) {
            maGioiTinh = "GT2";
        } else if (rbKhac.isChecked()) {
            maGioiTinh = "GT3";
        }

        if (hoTen.isEmpty() || ngaySinh.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ các trường bắt buộc (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        Student student = new Student();
        student.setHoTen(hoTen);
        student.setNgaySinh(ngaySinh);
        student.setDiaChi(diaChi);
        student.setEmail(email);
        student.setMaGioiTinh(maGioiTinh);

        btnTiepNhan.setEnabled(false);
        ApiClient.getApiService().receiveStudent(student).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                btnTiepNhan.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    String maHS = response.body().get("MaHocSinh");
                    Toast.makeText(ReceiveStudentsActivity.this, "Tiếp nhận thành công! Mã HS: " + maHS, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    try (ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            String errorContent = errorBody.string();
                            JSONObject jObjError = new JSONObject(errorContent);
                            String errorMsg = jObjError.has("error") ? jObjError.getString("error") : "Dữ liệu không hợp lệ";
                            Toast.makeText(ReceiveStudentsActivity.this, "Thất bại: " + errorMsg, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ReceiveStudentsActivity.this, "Lỗi hệ thống: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ReceiveStudentsActivity.this, "Lỗi hệ thống: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                btnTiepNhan.setEnabled(true);
                Toast.makeText(ReceiveStudentsActivity.this, "Không thể kết nối Server. Vui lòng kiểm tra mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
