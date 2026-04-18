package com.example.studentmanagementapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
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
    private MaterialButton btnTiepNhan, btnSuaHoSo, btnImportExcel;
    private ImageButton btnBack;
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

        edtNgaySinh.setOnClickListener(v -> showDatePicker());
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
        rgGioiTinh = findViewById(R.id.rgGioiTinh);
        rbNam = findViewById(R.id.rbNam);
        rbNu = findViewById(R.id.rbNu);
        rbKhac = findViewById(R.id.rbKhac);
        tilGioiTinhKhac = findViewById(R.id.tilGioiTinhKhac);
        btnTiepNhan = findViewById(R.id.btnTiepNhan);
        btnSuaHoSo = findViewById(R.id.btnSuaHoSo);
        btnImportExcel = findViewById(R.id.btnImportExcel);
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

    private void processExcelFile(Uri uri) {
        try {
            File tempFile = copyUriToInternalStorage(uri);
            List<Student> studentsFromExcel = new ArrayList<>();
            
            try (Workbook workbook = WorkbookFactory.create(tempFile)) {
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Student student = new Student();
                    student.setHoTen(formatter.formatCellValue(row.getCell(0)));
                    student.setNgaySinh(formatter.formatCellValue(row.getCell(1)));
                    
                    // Giới tính xử lý từ text sang Mã (Nam -> GT1, Nữ -> GT2, Khác -> GT3)
                    String genderText = formatter.formatCellValue(row.getCell(2)).trim();
                    if (genderText.equalsIgnoreCase("Nam")) student.setMaGioiTinh("GT1");
                    else if (genderText.equalsIgnoreCase("Nữ")) student.setMaGioiTinh("GT2");
                    else student.setMaGioiTinh("GT3");
                    
                    student.setDiaChi(formatter.formatCellValue(row.getCell(3)));
                    student.setEmail(formatter.formatCellValue(row.getCell(4)));
                    
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
            new AlertDialog.Builder(this).setTitle("Lỗi").setMessage(e.getMessage()).show();
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
                retryCount++;
                lastException = e;
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
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void uploadExcelFile() {
        if (selectedFileUri == null) return;
        try {
            File file = new File(getCacheDir(), "import_students_tmp.xlsx");
            RequestBody requestFile = RequestBody.create(MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", getFileName(selectedFileUri), requestFile);

            btnImportExcel.setEnabled(false);
            ApiClient.getApiService().importStudentExcel(body).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    btnImportExcel.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(ReceiveStudentsActivity.this, "Import học sinh thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        List<String> errors = parseErrorResponse(response);
                        showValidationErrorDialog(errors);
                    }
                }
                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    btnImportExcel.setEnabled(true);
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
        new AlertDialog.Builder(this).setTitle("Lỗi Import").setMessage(sb.toString().trim()).setPositiveButton("OK", null).show();
    }

    private void performSaveStudent() {
        String hoTen = (edtHoTen.getText() != null) ? edtHoTen.getText().toString().trim() : "";
        String ngaySinh = (edtNgaySinh.getText() != null) ? edtNgaySinh.getText().toString().trim() : "";
        String diaChi = (edtDiaChi.getText() != null) ? edtDiaChi.getText().toString().trim() : "";
        String email = (edtEmail.getText() != null) ? edtEmail.getText().toString().trim() : "";
        
        String maGioiTinh = "GT1";
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
