package com.example.studentmanagementapp;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.Student;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditStudentActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteMaHS;
    private TextInputEditText edtTenHS, edtNgaySinh, edtDiaChi, edtEmail;
    private TextInputLayout tilMaHS, tilTenHS, tilNgaySinh, tilDiaChi, tilEmail;
    private RadioGroup rgGioiTinh;
    private RadioButton rbNam, rbNu, rbKhac;
    private MaterialButton btnLuuCapNhat;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;
    private ProgressBar pbSearchLoading;
    
    private Student currentStudent = null;
    private List<Student> lastSearchResults = new ArrayList<>();

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_student);

        initViews();
        setupSearchLogic();
        setupErrorClearing();

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
        
        tilMaHS = findViewById(R.id.tilMaHS);
        tilTenHS = findViewById(R.id.tilTenHS);
        tilNgaySinh = findViewById(R.id.tilNgaySinh);
        tilDiaChi = findViewById(R.id.tilDiaChi);
        tilEmail = findViewById(R.id.tilEmail);
        
        rgGioiTinh = findViewById(R.id.rgGioiTinh);
        rbNam = findViewById(R.id.rbNam);
        rbNu = findViewById(R.id.rbNu);
        rbKhac = findViewById(R.id.rbKhac);
        btnLuuCapNhat = findViewById(R.id.btnLuuCapNhat);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);
        pbSearchLoading = findViewById(R.id.pbSearchLoading);
        
        enableFields(false);
    }

    private void setupErrorClearing() {
        edtTenHS.addTextChangedListener(new SimpleTextWatcher(tilTenHS));
        edtNgaySinh.addTextChangedListener(new SimpleTextWatcher(tilNgaySinh));
        edtDiaChi.addTextChangedListener(new SimpleTextWatcher(tilDiaChi));
        edtEmail.addTextChangedListener(new SimpleTextWatcher(tilEmail));
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final TextInputLayout layout;
        public SimpleTextWatcher(TextInputLayout layout) { this.layout = layout; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (layout != null) {
                layout.setError(null);
                layout.setErrorEnabled(false);
            }
        }
        @Override public void afterTextChanged(Editable s) {}
    }

    private void showLoading() {
        runOnUiThread(() -> {
            if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
            if (btnLuuCapNhat != null) {
                btnLuuCapNhat.setEnabled(false);
                btnLuuCapNhat.setAlpha(0.5f);
            }
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> {
            if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
            if (btnLuuCapNhat != null) {
                btnLuuCapNhat.setEnabled(true);
                btnLuuCapNhat.setAlpha(1.0f);
            }
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void showErrorDialog(String title, String message) {
        runOnUiThread(() -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Đóng", null)
                    .show();
        });
    }

    private void showRetrySnackbar(String message, Runnable retryAction) {
        runOnUiThread(() -> {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                    .setAction("Thử lại", v -> retryAction.run())
                    .show();
        });
    }

    private void setupSearchLogic() {
        autoCompleteMaHS.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                if (s.length() == 0) {
                    currentStudent = null;
                    clearFields();
                    enableFields(false);
                    if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
                    return;
                }

                if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.VISIBLE);

                searchRunnable = () -> {
                    if (s.length() >= 1) {
                        ApiClient.getApiService().searchStudent(s.toString()).enqueue(new Callback<List<Student>>() {
                            @Override
                            public void onResponse(@NonNull Call<List<Student>> call, @NonNull Response<List<Student>> response) {
                                if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
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
                            public void onFailure(@NonNull Call<List<Student>> call, @NonNull Throwable t) {
                                if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
                                showRetrySnackbar("Lỗi kết nối khi tìm kiếm", () -> {
                                    String text = autoCompleteMaHS.getText().toString();
                                    autoCompleteMaHS.setText(text);
                                    autoCompleteMaHS.setSelection(text.length());
                                });
                            }
                        });
                    } else {
                        if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
                    }
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        autoCompleteMaHS.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            String selectedMaHS = selectedItem.split(" - ")[0];
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
        edtTenHS.setText(student.getHoTen() != null ? student.getHoTen() : "");
        edtNgaySinh.setText(student.getNgaySinh() != null ? student.getNgaySinh() : "");
        edtDiaChi.setText(student.getDiaChi() != null ? student.getDiaChi() : "");
        edtEmail.setText(student.getEmail() != null ? student.getEmail() : "");
        String maGT = student.getMaGioiTinh();
        if ("GT1".equals(maGT)) rbNam.setChecked(true);
        else if ("GT2".equals(maGT)) rbNu.setChecked(true);
        else if ("GT3".equals(maGT)) rbKhac.setChecked(true);
        enableFields(true);
        edtTenHS.postDelayed(() -> {
            edtTenHS.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(edtTenHS, InputMethodManager.SHOW_IMPLICIT);
        }, 100);
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
        btnLuuCapNhat.setAlpha(enabled ? 1.0f : 0.5f);
        if (enabled) {
            Animation shake = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            btnLuuCapNhat.startAnimation(shake);
        }
    }

    private void clearFields() {
        edtTenHS.setText("");
        edtNgaySinh.setText("");
        edtDiaChi.setText("");
        edtEmail.setText("");
        rgGioiTinh.clearCheck();
        tilTenHS.setError(null);
        tilNgaySinh.setError(null);
        tilDiaChi.setError(null);
        tilEmail.setError(null);
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày sinh")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String dateString = sdf.format(new Date(selection));
            edtNgaySinh.setText(dateString);
            if (tilNgaySinh != null) {
                tilNgaySinh.setError(null);
                tilNgaySinh.setErrorEnabled(false);
            }
        });
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void performUpdate() {
        if (currentStudent == null) return;
        
        hideKeyboard();

        String hoTen = edtTenHS.getText().toString().trim();
        String ngaySinh = edtNgaySinh.getText().toString().trim();
        String diaChi = edtDiaChi.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        boolean hasError = false;

        if (TextUtils.isEmpty(hoTen)) {
            tilTenHS.setError("Họ và tên không được để trống");
            hasError = true;
        }
        if (TextUtils.isEmpty(ngaySinh)) {
            tilNgaySinh.setError("Vui lòng chọn ngày sinh");
            hasError = true;
        }
        if (TextUtils.isEmpty(diaChi)) {
            tilDiaChi.setError("Địa chỉ không được để trống");
            hasError = true;
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email không được để trống");
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            hasError = true;
        }

        if (hasError) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận thay đổi")
                .setMessage("Bạn có chắc chắn muốn cập nhật thông tin cho học sinh " + hoTen + " không?")
                .setPositiveButton("Lưu", (dialog, which) -> {
                    currentStudent.setHoTen(hoTen);
                    currentStudent.setNgaySinh(ngaySinh);
                    currentStudent.setDiaChi(diaChi);
                    currentStudent.setEmail(email);
                    String maGT = "GT1";
                    if (rbNu.isChecked()) maGT = "GT2";
                    else if (rbKhac.isChecked()) maGT = "GT3";
                    currentStudent.setMaGioiTinh(maGT);

                    showLoading();
                    ApiClient.getApiService().updateStudent(currentStudent).enqueue(new Callback<Map<String, String>>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                            hideLoading();
                            if (response.isSuccessful()) {
                                new MaterialAlertDialogBuilder(EditStudentActivity.this)
                                        .setTitle("Thành công")
                                        .setMessage("Đã cập nhật thông tin học sinh thành công.")
                                        .setPositiveButton("OK", (d, w) -> finish())
                                        .setCancelable(false)
                                        .show();
                            } else {
                                String errorMsg = "Cập nhật thất bại";
                                try (ResponseBody errorBody = response.errorBody()) {
                                    if (errorBody != null) {
                                        JSONObject jObjError = new JSONObject(errorBody.string());
                                        errorMsg = jObjError.optString("error", "Dữ liệu không hợp lệ");
                                    }
                                } catch (Exception ignored) {}
                                showErrorDialog("Thất bại", errorMsg);
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                            hideLoading();
                            showRetrySnackbar("Lỗi kết nối máy chủ", EditStudentActivity.this::performUpdate);
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
