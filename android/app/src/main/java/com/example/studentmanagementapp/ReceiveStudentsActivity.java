package com.example.studentmanagementapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.Student;
import com.example.studentmanagementapp.model.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReceiveStudentsActivity extends AppCompatActivity {

    private TextInputEditText edtHoTen, edtNgaySinh, edtDiaChi, edtEmail, edtGioiTinhKhac;
    private RadioGroup rgGioiTinh;
    private RadioButton rbNam, rbNu, rbKhac;
    private TextInputLayout tilHoTen, tilNgaySinh, tilDiaChi, tilEmail, tilGioiTinhKhac;
    private MaterialButton btnTiepNhan, btnSuaHoSo;
    private ImageButton btnBack, btnImportExcel;
    private LinearProgressIndicator progressIndicator;
    private Uri selectedFileUri;

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    processExcelFile(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_students);

        System.setProperty("java.io.tmpdir", getCacheDir().getAbsolutePath());

        initViews();
        setupGenderLogic();
        setupErrorClearing();

        View.OnClickListener dateClickListener = v -> showDatePicker();
        edtNgaySinh.setOnClickListener(dateClickListener);
        if (tilNgaySinh != null) {
            tilNgaySinh.setEndIconOnClickListener(dateClickListener);
        }
        
        btnBack.setOnClickListener(v -> finish());
        btnTiepNhan.setOnClickListener(v -> performSaveStudent());
        
        btnSuaHoSo.setOnClickListener(v -> {
            Intent intent = new Intent(ReceiveStudentsActivity.this, EditStudentActivity.class);
            startActivity(intent);
        });

        btnImportExcel.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
    }

    private void initViews() {
        edtHoTen = findViewById(R.id.edtHoTen);
        edtNgaySinh = findViewById(R.id.edtNgaySinh);
        edtDiaChi = findViewById(R.id.edtDiaChi);
        edtEmail = findViewById(R.id.edtEmail);
        edtGioiTinhKhac = findViewById(R.id.edtGioiTinhKhac);
        
        tilHoTen = findViewById(R.id.tilHoTen);
        tilNgaySinh = findViewById(R.id.tilNgaySinh);
        tilDiaChi = findViewById(R.id.tilDiaChi);
        tilEmail = findViewById(R.id.tilEmail);
        tilGioiTinhKhac = findViewById(R.id.tilGioiTinhKhac);
        
        rgGioiTinh = findViewById(R.id.rgGioiTinh);
        rbNam = findViewById(R.id.rbNam);
        rbNu = findViewById(R.id.rbNu);
        rbKhac = findViewById(R.id.rbKhac);
        
        btnTiepNhan = findViewById(R.id.btnTiepNhan);
        btnSuaHoSo = findViewById(R.id.btnSuaHoSo);
        btnImportExcel = findViewById(R.id.btnImportExcel);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);
    }

    private void setupErrorClearing() {
        edtHoTen.addTextChangedListener(new SimpleTextWatcher(tilHoTen));
        edtNgaySinh.addTextChangedListener(new SimpleTextWatcher(tilNgaySinh));
        edtDiaChi.addTextChangedListener(new SimpleTextWatcher(tilDiaChi));
        edtEmail.addTextChangedListener(new SimpleTextWatcher(tilEmail));
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final TextInputLayout layout;
        public SimpleTextWatcher(TextInputLayout layout) { this.layout = layout; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (layout != null) layout.setError(null);
        }
        @Override public void afterTextChanged(Editable s) {}
    }

    private void showLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
        if (btnTiepNhan != null) {
            btnTiepNhan.setEnabled(false);
            btnTiepNhan.setText("Đang xử lý...");
        }
        if (btnImportExcel != null) btnImportExcel.setEnabled(false);
        if (btnSuaHoSo != null) btnSuaHoSo.setEnabled(false);
    }

    private void hideLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
        if (btnTiepNhan != null) {
            btnTiepNhan.setEnabled(true);
            btnTiepNhan.setText("TIẾP NHẬN HỌC SINH");
        }
        if (btnImportExcel != null) btnImportExcel.setEnabled(true);
        if (btnSuaHoSo != null) btnSuaHoSo.setEnabled(true);
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
            if (tilNgaySinh != null) tilNgaySinh.setError(null);
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void processExcelFile(Uri uri) {
        try {
            File tempFile = copyUriToInternalStorage(uri);
            List<Student> studentsFromExcel = new ArrayList<>();
            
            try (Workbook workbook = WorkbookFactory.create(tempFile)) {
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();

                // 1. Tìm vị trí cột dựa trên Header (Hàng 0)
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) throw new Exception("File Excel không có dữ liệu tiêu đề.");

                int idxHoTen = -1, idxGioiTinh = -1, idxNgaySinh = -1, idxDiaChi = -1, idxEmail = -1;

                for (Cell cell : headerRow) {
                    String title = formatter.formatCellValue(cell).trim().toLowerCase();
                    if (title.contains("họ và tên") || title.contains("hoten") || title.equals("họ tên")) idxHoTen = cell.getColumnIndex();
                    else if (title.contains("giới tính") || title.contains("gioitinh") || title.contains("gender")) idxGioiTinh = cell.getColumnIndex();
                    else if (title.contains("ngày sinh") || title.contains("ngaysinh") || title.contains("birthday")) idxNgaySinh = cell.getColumnIndex();
                    else if (title.contains("địa chỉ") || title.contains("diachi") || title.contains("address")) idxDiaChi = cell.getColumnIndex();
                    else if (title.contains("email")) idxEmail = cell.getColumnIndex();
                }

                // Kiểm tra xem các cột bắt buộc có tồn tại không (Họ tên, Ngày sinh, Giới tính)
                if (idxHoTen == -1 || idxNgaySinh == -1 || idxGioiTinh == -1) {
                    throw new Exception("Không tìm thấy các cột bắt buộc trong file (Họ tên, Ngày sinh, Giới tính). Vui lòng kiểm tra lại tiêu đề cột.");
                }

                // 2. Đọc dữ liệu từ các hàng tiếp theo
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Student student = new Student();
                    student.setHoTen(formatter.formatCellValue(row.getCell(idxHoTen)).trim());
                    
                    // Xử lý Giới tính: Nam -> GT1, Nữ -> GT2, Khác -> GT3
                    String genderText = formatter.formatCellValue(row.getCell(idxGioiTinh)).trim();
                    if (genderText.equalsIgnoreCase("Nam") || genderText.equalsIgnoreCase("GT1")) student.setMaGioiTinh("GT1");
                    else if (genderText.equalsIgnoreCase("Nữ") || genderText.equalsIgnoreCase("Nu") || genderText.equalsIgnoreCase("GT2")) student.setMaGioiTinh("GT2");
                    else student.setMaGioiTinh("GT3");

                    student.setNgaySinh(formatter.formatCellValue(row.getCell(idxNgaySinh)).trim());
                    
                    if (idxDiaChi != -1) student.setDiaChi(formatter.formatCellValue(row.getCell(idxDiaChi)).trim());
                    if (idxEmail != -1) student.setEmail(formatter.formatCellValue(row.getCell(idxEmail)).trim());
                    
                    if (!student.getHoTen().isEmpty()) {
                        studentsFromExcel.add(student);
                    }
                }
            }
            
            if (!studentsFromExcel.isEmpty()) {
                showStudentPreviewDialog(studentsFromExcel);
            } else {
                Toast.makeText(this, "Không tìm thấy dữ liệu học sinh hợp lệ", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("ExcelError", "Lỗi xử lý file: ", e);
            new AlertDialog.Builder(this).setTitle("Lỗi đọc file").setMessage(e.getMessage()).show();
        }
    }

    private void showStudentPreviewDialog(List<Student> students) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_student_preview, null);
        
        RecyclerView rvPreview = view.findViewById(R.id.rvStudentPreview);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmImport);
        
        rvPreview.setLayoutManager(new LinearLayoutManager(this));
        rvPreview.setAdapter(new GenericAdapter<>(students, R.layout.item_student_import_row, (student, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvHoTen)).setText(student.getHoTen());
            ((TextView) itemView.findViewById(R.id.tvNgaySinh)).setText(student.getNgaySinh());
            
            String gt = "Nam";
            if ("GT2".equals(student.getMaGioiTinh())) gt = "Nữ";
            else if ("GT3".equals(student.getMaGioiTinh())) gt = "Khác";
            ((TextView) itemView.findViewById(R.id.tvGioiTinh)).setText(gt);
            
            ((TextView) itemView.findViewById(R.id.tvDiaChi)).setText(student.getDiaChi());
            ((TextView) itemView.findViewById(R.id.tvEmail)).setText(student.getEmail());
        }));

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            uploadExcelFile();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private File copyUriToInternalStorage(Uri uri) throws Exception {
        File destinationFile = new File(getCacheDir(), "import_students_tmp.xlsx");
        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;
        while (retryCount < maxRetries) {
            try (InputStream is = getContentResolver().openInputStream(uri);
                 FileOutputStream os = new FileOutputStream(destinationFile)) {
                if (is == null) throw new Exception("Không thể mở tệp.");
                byte[] buffer = new byte[8192];
                int length;
                while ((length = is.read(buffer)) != -1) os.write(buffer, 0, length);
                os.flush();
                if (destinationFile.length() > 0) return destinationFile;
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                Thread.sleep(500);
            }
        }
        if (uri.getAuthority().contains("com.google.android.apps.docs")) {
            throw new Exception("Không thể đọc file từ Drive. Vui lòng tải file về máy rồi thử lại.");
        }
        throw new Exception("Lỗi truy cập tệp: " + (lastException != null ? lastException.getLocalizedMessage() : "Không rõ"));
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        return (result != null) ? result : "temp.xlsx";
    }

    private void uploadExcelFile() {
        if (selectedFileUri == null) return;
        try {
            File file = new File(getCacheDir(), "import_students_tmp.xlsx");
            RequestBody requestFile = RequestBody.create(MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", getFileName(selectedFileUri), requestFile);

            showLoading();
            ApiClient.getApiService().importStudentExcel(body).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    hideLoading();
                    if (response.isSuccessful()) {
                        new MaterialAlertDialogBuilder(ReceiveStudentsActivity.this)
                                .setTitle("Thành công")
                                .setMessage("Tiếp nhận học sinh từ Excel thành công!")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        List<String> errors = parseErrorResponse(response);
                        showValidationErrorDialog(errors);
                    }
                }
                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    hideLoading();
                    Toast.makeText(ReceiveStudentsActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi chuẩn bị file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> parseErrorResponse(Response<?> response) {
        List<String> errorList = new ArrayList<>();
        try {
            ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
                JSONObject jsonObject = new JSONObject(errorBody.string());
                if (jsonObject.has("errors")) {
                    JSONArray errorsArray = jsonObject.getJSONArray("errors");
                    for (int i = 0; i < errorsArray.length(); i++) {
                        JSONObject errorObj = errorsArray.getJSONObject(i);
                        errorList.add("Dòng " + errorObj.optInt("row") + ": " + errorObj.optString("message"));
                    }
                } else {
                    errorList.add(jsonObject.optString("error", "Lỗi server"));
                }
            }
        } catch (Exception e) {
            errorList.add("Lỗi hệ thống: " + response.code());
        }
        return errorList;
    }

    private void showValidationErrorDialog(List<String> errors) {
        StringBuilder sb = new StringBuilder();
        for (String err : errors) sb.append("• ").append(err).append("\n");
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Lỗi Import")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(sb.toString().trim())
                .setPositiveButton("OK", null)
                .show();
    }

    private void resetFields() {
        edtHoTen.setText("");
        edtNgaySinh.setText("");
        edtDiaChi.setText("");
        edtEmail.setText("");
        rbNam.setChecked(true);
        edtGioiTinhKhac.setText("");
        tilHoTen.setError(null);
        tilNgaySinh.setError(null);
        tilDiaChi.setError(null);
        tilEmail.setError(null);
        edtHoTen.requestFocus();
    }

    private void performSaveStudent() {
        if (tilHoTen != null) tilHoTen.setError(null);
        if (tilNgaySinh != null) tilNgaySinh.setError(null);
        if (tilDiaChi != null) tilDiaChi.setError(null);
        if (tilEmail != null) tilEmail.setError(null);

        String hoTen = (edtHoTen.getText() != null) ? edtHoTen.getText().toString().trim() : "";
        String ngaySinh = (edtNgaySinh.getText() != null) ? edtNgaySinh.getText().toString().trim() : "";
        String diaChi = (edtDiaChi.getText() != null) ? edtDiaChi.getText().toString().trim() : "";
        String email = (edtEmail.getText() != null) ? edtEmail.getText().toString().trim() : "";
        
        boolean hasError = false;

        if (hoTen.isEmpty()) {
            if (tilHoTen != null) tilHoTen.setError("Họ và tên không được để trống");
            hasError = true;
        }

        if (ngaySinh.isEmpty()) {
            if (tilNgaySinh != null) tilNgaySinh.setError("Vui lòng chọn ngày sinh");
            hasError = true;
        }

        if (diaChi.isEmpty()) {
            if (tilDiaChi != null) tilDiaChi.setError("Địa chỉ không được để trống");
            hasError = true;
        }

        if (email.isEmpty()) {
            if (tilEmail != null) tilEmail.setError("Email không được để trống");
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (tilEmail != null) tilEmail.setError("Email không hợp lệ");
            hasError = true;
        }

        if (hasError) return;

        String maGioiTinh = "GT1";
        if (rbNu.isChecked()) {
            maGioiTinh = "GT2";
        } else if (rbKhac.isChecked()) {
            maGioiTinh = "GT3";
        }

        Student student = new Student();
        student.setHoTen(hoTen);
        student.setNgaySinh(ngaySinh);
        student.setDiaChi(diaChi);
        student.setEmail(email);
        student.setMaGioiTinh(maGioiTinh);

        showLoading();
        ApiClient.getApiService().receiveStudent(student).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    String maHS = response.body().get("MaHocSinh");
                    String tenHS = hoTen;
                    
                    new MaterialAlertDialogBuilder(ReceiveStudentsActivity.this)
                            .setTitle("Tiếp nhận thành công!")
                            .setMessage("Học sinh " + tenHS + " đã được cấp mã " + maHS + ". Bạn có muốn xếp lớp cho học sinh này ngay bây giờ không?")
                            .setPositiveButton("Xếp lớp ngay", (dialog, which) -> {
                                try {
                                    Class<?> targetClass = Class.forName("com.example.studentmanagementapp.CreateClassListActivity");
                                    Intent intent = new Intent(ReceiveStudentsActivity.this, targetClass);
                                    intent.putExtra("MaHocSinh", maHS);
                                    intent.putExtra("HoTen", tenHS);
                                    startActivity(intent);
                                    finish();
                                } catch (ClassNotFoundException e) {
                                    Toast.makeText(ReceiveStudentsActivity.this, "Không tìm thấy màn hình lập danh sách lớp!", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            })
                            .setNeutralButton("Tiếp nhận tiếp", (dialog, which) -> resetFields())
                            .setNegativeButton("Đóng", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
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
                hideLoading();
                Toast.makeText(ReceiveStudentsActivity.this, "Không thể kết nối Server. Vui lòng kiểm tra mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
