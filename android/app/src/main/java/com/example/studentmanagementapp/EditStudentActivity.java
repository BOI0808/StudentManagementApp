package com.example.studentmanagementapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.Student;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditStudentActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteMaHS;
    private TextInputEditText edtTenHS, edtNgaySinh, edtDiaChi, edtEmail;
    private RadioGroup rgGioiTinh;
    private RadioButton rbNam, rbNu, rbKhac;
    private MaterialButton btnLuuCapNhat;
    private ImageButton btnBack;
    
    private Student currentStudent = null;
    private List<Student> lastSearchResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_student);

        initViews();
        setupSearchLogic();

        btnBack.setOnClickListener(v -> finish());
        edtNgaySinh.setOnClickListener(v -> {
            if (edtNgaySinh.isEnabled()) {
                showDatePicker();
            }
        });
        btnLuuCapNhat.setOnClickListener(v -> performUpdate());
    }

    private void initViews() {
        autoCompleteMaHS = findViewById(R.id.autoCompleteMaHS);
        edtTenHS = findViewById(R.id.edtTenHS);
        edtNgaySinh = findViewById(R.id.edtNgaySinh);
        edtDiaChi = findViewById(R.id.edtDiaChi);
        edtEmail = findViewById(R.id.edtEmail);
        rgGioiTinh = findViewById(R.id.rgGioiTinh);
        rbNam = findViewById(R.id.rbNam);
        rbNu = findViewById(R.id.rbNu);
        rbKhac = findViewById(R.id.rbKhac);
        btnLuuCapNhat = findViewById(R.id.btnLuuCapNhat);
        btnBack = findViewById(R.id.btnBack);
        
        enableFields(false);
    }

    private void setupSearchLogic() {
        autoCompleteMaHS.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (currentStudent != null && !s.toString().equals(currentStudent.getMaHocSinh())) {
                    currentStudent = null;
                    clearFields();
                    enableFields(false);
                }

                if (s.length() >= 2) {
                    ApiClient.getApiService().searchStudent(s.toString()).enqueue(new Callback<List<Student>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<Student>> call, @NonNull Response<List<Student>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                lastSearchResults = response.body();
                                List<String> suggestions = new ArrayList<>();
                                for (Student st : lastSearchResults) {
                                    suggestions.add(st.getMaHocSinh() + " - " + st.getHoTen());
                                }
                                
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(EditStudentActivity.this,
                                        android.R.layout.simple_dropdown_item_1line, suggestions);
                                autoCompleteMaHS.setAdapter(adapter);
                                autoCompleteMaHS.showDropDown();
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<List<Student>> call, @NonNull Throwable t) {}
                    });
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        autoCompleteMaHS.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            String selectedMaHS = selectedItem.split(" - ")[0];
            
            // Tìm chính xác đối tượng student trong kết quả tìm kiếm mới nhất
            for (Student s : lastSearchResults) {
                if (s.getMaHocSinh().equals(selectedMaHS)) {
                    fillStudentData(s);
                    break;
                }
            }
        });
    }

    private void fillStudentData(Student student) {
        if (student == null) return;
        
        currentStudent = student;
        autoCompleteMaHS.setText(student.getMaHocSinh(), false);
        
        // LOG ĐỂ KIỂM TRA DỮ LIỆU TỪ SERVER TRẢ VỀ
        Log.d("EDIT_STUDENT", "SERVER_DATA: " + student.getHoTen() + " | DC: " + student.getDiaChi() + " | EM: " + student.getEmail());

        edtTenHS.setText(student.getHoTen() != null ? student.getHoTen() : "");
        edtNgaySinh.setText(student.getNgaySinh() != null ? student.getNgaySinh() : "");
        edtDiaChi.setText(student.getDiaChi() != null ? student.getDiaChi() : "");
        edtEmail.setText(student.getEmail() != null ? student.getEmail() : "");
        
        String maGT = student.getMaGioiTinh();
        if ("GT1".equals(maGT)) rbNam.setChecked(true);
        else if ("GT2".equals(maGT)) rbNu.setChecked(true);
        else if ("GT3".equals(maGT)) rbKhac.setChecked(true);

        enableFields(true);
    }

    private void enableFields(boolean enabled) {
        edtTenHS.setEnabled(enabled);
        edtNgaySinh.setEnabled(enabled);
        edtDiaChi.setEnabled(enabled);
        edtEmail.setEnabled(enabled);
        rbNam.setEnabled(enabled);
        rbNu.setEnabled(enabled);
        rbKhac.setEnabled(enabled);
        btnLuuCapNhat.setEnabled(enabled);
    }

    private void clearFields() {
        edtTenHS.setText("");
        edtNgaySinh.setText("");
        edtDiaChi.setText("");
        edtEmail.setText("");
        rgGioiTinh.clearCheck();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
            edtNgaySinh.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void performUpdate() {
        if (currentStudent == null) return;

        String hoTen = edtTenHS.getText().toString().trim();
        String ngaySinh = edtNgaySinh.getText().toString().trim();
        String diaChi = edtDiaChi.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        
        if (hoTen.isEmpty() || ngaySinh.isEmpty()) {
            Toast.makeText(this, "Họ tên và ngày sinh bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        currentStudent.setHoTen(hoTen);
        currentStudent.setNgaySinh(ngaySinh);
        currentStudent.setDiaChi(diaChi);
        currentStudent.setEmail(email);
        
        String maGT = "GT1";
        if (rbNu.isChecked()) maGT = "GT2";
        else if (rbKhac.isChecked()) maGT = "GT3";
        currentStudent.setMaGioiTinh(maGT);

        btnLuuCapNhat.setEnabled(false);
        ApiClient.getApiService().updateStudent(currentStudent).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                btnLuuCapNhat.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(EditStudentActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditStudentActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                btnLuuCapNhat.setEnabled(true);
                Toast.makeText(EditStudentActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
