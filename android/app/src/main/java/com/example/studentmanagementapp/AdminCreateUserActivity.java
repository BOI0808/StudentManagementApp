package com.example.studentmanagementapp;

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
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCreateUserActivity extends AppCompatActivity {

    private TextView btnLogout, tvTitle;
    private TextInputEditText edtFullName, edtUsername, edtPassword, edtEmail, edtPhone;
    private MaterialButton btnCreateAccount, btnXemDanhSach, btnImportExcel;
    private User editingUser = null;
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
        setContentView(R.layout.activity_admin_create_user);

        System.setProperty("java.io.tmpdir", getCacheDir().getAbsolutePath());

        btnLogout = findViewById(R.id.btnLogout);
        tvTitle = findViewById(R.id.tvTitle);
        edtFullName = findViewById(R.id.edtFullName);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnXemDanhSach = findViewById(R.id.btnXemDanhSach);
        btnImportExcel = findViewById(R.id.btnImportExcel);

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(AdminCreateUserActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        if (btnCreateAccount != null) {
            btnCreateAccount.setOnClickListener(v -> performCreateOrUpdateAccount());
        }
        
        if (btnXemDanhSach != null) {
            btnXemDanhSach.setOnClickListener(v -> {
                Intent intent = new Intent(AdminCreateUserActivity.this, AdminUserListActivity.class);
                startActivity(intent);
            });
        }

        if (btnImportExcel != null) {
            btnImportExcel.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        }

        checkEditMode();
    }

    private void processExcelFile(Uri uri) {
        try {
            File tempFile = copyUriToInternalStorage(uri);
            List<User> usersFromExcel = new ArrayList<>();
            
            try (Workbook workbook = WorkbookFactory.create(tempFile)) {
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    User user = new User();
                    user.setHoTen(formatter.formatCellValue(row.getCell(0)));
                    user.setTenDangNhap(formatter.formatCellValue(row.getCell(1)));
                    user.setMatKhau(formatter.formatCellValue(row.getCell(2)));
                    user.setEmail(formatter.formatCellValue(row.getCell(3)));
                    user.setSoDienThoai(formatter.formatCellValue(row.getCell(4)));
                    
                    String rightsStr = formatter.formatCellValue(row.getCell(5));
                    if (rightsStr != null && !rightsStr.isEmpty()) {
                        user.setDanhSachQuyen(Arrays.asList(rightsStr.split(",")));
                    }
                    
                    if (user.getTenDangNhap() != null && !user.getTenDangNhap().trim().isEmpty()) {
                        usersFromExcel.add(user);
                    }
                }
            }
            
            if (!usersFromExcel.isEmpty()) {
                showPreviewDialog(usersFromExcel);
            } else {
                Toast.makeText(this, "Không tìm thấy dữ liệu hợp lệ trong file", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("ExcelError", "Lỗi xử lý file: ", e);
            new AlertDialog.Builder(this)
                    .setTitle("Lỗi đọc file")
                    .setMessage(e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private File copyUriToInternalStorage(Uri uri) throws Exception {
        File destinationFile = new File(getCacheDir(), "import_cache.xlsx");
        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try (InputStream is = getContentResolver().openInputStream(uri);
                 FileOutputStream os = new FileOutputStream(destinationFile)) {
                
                if (is == null) throw new Exception("Không thể mở tệp từ nguồn cung cấp.");
                
                byte[] buffer = new byte[8192];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    os.write(buffer, 0, length);
                }
                os.flush();
                
                if (destinationFile.exists() && destinationFile.length() > 0) {
                    return destinationFile;
                } else {
                    throw new Exception("Tệp sao chép bị trống (0 bytes).");
                }
            } catch (Exception e) {
                retryCount++;
                lastException = e;
                if (retryCount < maxRetries) Thread.sleep(500);
            }
        }

        if (isGoogleDriveUri(uri)) {
            throw new Exception("Không thể đọc file từ Drive. Vui lòng tải file về máy rồi thử lại.");
        }
        throw new Exception("Lỗi truy cập tệp sau " + maxRetries + " lần thử: " + (lastException != null ? lastException.getLocalizedMessage() : "Không rõ"));
    }

    private boolean isGoogleDriveUri(Uri uri) {
        return uri != null && uri.getAuthority() != null && 
               uri.getAuthority().contains("com.google.android.apps.docs");
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

    private void showPreviewDialog(List<User> users) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_excel_preview, null);
        
        RecyclerView rvPreview = view.findViewById(R.id.rvExcelPreview);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmImport);
        
        rvPreview.setLayoutManager(new LinearLayoutManager(this));
        rvPreview.setAdapter(new GenericAdapter<>(users, R.layout.item_excel_import_row, (user, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvHoTen)).setText(user.getHoTen());
            ((TextView) itemView.findViewById(R.id.tvTaiKhoan)).setText(user.getTenDangNhap());
            ((TextView) itemView.findViewById(R.id.tvMatKhau)).setText(user.getMatKhau());
            ((TextView) itemView.findViewById(R.id.tvEmail)).setText(user.getEmail());
            ((TextView) itemView.findViewById(R.id.tvSoDienThoai)).setText(user.getSoDienThoai());
            ((TextView) itemView.findViewById(R.id.tvQuyen)).setText(user.getDanhSachQuyen() != null ? String.join(",", user.getDanhSachQuyen()) : "");
        }));

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            uploadExcelFile();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void uploadExcelFile() {
        if (selectedFileUri == null) return;

        try {
            File file = new File(getCacheDir(), "import_cache.xlsx");
            RequestBody requestFile = RequestBody.create(MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", getFileName(selectedFileUri), requestFile);

            btnImportExcel.setEnabled(false);
            ApiClient.getApiService().importExcel(body).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    btnImportExcel.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(AdminCreateUserActivity.this, "Import thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        List<String> errors = parseErrorResponse(response);
                        showValidationErrorDialog(errors);
                    }
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    btnImportExcel.setEnabled(true);
                    Toast.makeText(AdminCreateUserActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
                        int row = errorObj.optInt("row", -1);
                        String message = errorObj.optString("message", "Lỗi không xác định");
                        if (row != -1) {
                            errorList.add("Dòng " + row + ": " + message);
                        } else {
                            errorList.add(message);
                        }
                    }
                } else if (jsonObject.has("error")) {
                    errorList.add(jsonObject.getString("error"));
                }
            }
        } catch (Exception e) {
            errorList.add("Lỗi hệ thống: " + response.code());
        }
        if (errorList.isEmpty()) errorList.add("Lỗi không xác định từ Server (" + response.code() + ")");
        return errorList;
    }

    private void showValidationErrorDialog(List<String> errors) {
        StringBuilder sb = new StringBuilder();
        for (String err : errors) {
            sb.append("• ").append(err).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Thông báo lỗi Import")
                .setMessage(sb.toString().trim())
                .setPositiveButton("Đã hiểu", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void checkEditMode() {
        editingUser = (User) getIntent().getSerializableExtra("user_data");
        if (editingUser != null) {
            if (tvTitle != null) tvTitle.setText("Cập Nhật Tài Khoản");
            btnCreateAccount.setText("CẬP NHẬT TÀI KHOẢN");
            
            edtFullName.setText(editingUser.getHoTen());
            edtUsername.setText(editingUser.getTenDangNhap());
            edtUsername.setEnabled(false);
            edtPassword.setText(editingUser.getMatKhau());
            edtEmail.setText(editingUser.getEmail());
            edtPhone.setText(editingUser.getSoDienThoai());
            
            setPermissions(editingUser.getDanhSachQuyen());
        }
    }

    private void setPermissions(List<String> permissions) {
        if (permissions == null) return;
        int[] ids = {R.id.cbTiepNhanHS, R.id.cbLapDanhSachLop, R.id.cbLapDanhSachHSChoLop, 
                    R.id.cbLapDanhSachNamHoc, R.id.cbLapDanhSachKhoiLop, R.id.cbLapDanhSachMonHoc,
                    R.id.cbTraCuuHS, R.id.cbNhapDiem, R.id.cbLoaiKiemTra, R.id.cbBaoCaoMon, 
                    R.id.cbBaoCaoHocKy, R.id.cbCaiDatThamSo};
        String[] codes = {"CNTNHS", "CNLDSL", "CNLDSHSCL", "CNLDSNH", "CNLDSKL", "CNLDSMH",
                         "CNTCHS", "CNNBD", "CNNDSCLKT", "CNLBCTKM", "CNLBCTKHK", "CNCDTSHT"};
        
        for (int i = 0; i < ids.length; i++) {
            CheckBox cb = findViewById(ids[i]);
            if (cb != null) cb.setChecked(permissions.contains(codes[i]));
        }
    }

    private void performCreateOrUpdateAccount() {
        String fullName = edtFullName.getText().toString().trim();
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> permissions = getSelectedPermissions();
        if (permissions.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một quyền", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User();
        user.setHoTen(fullName);
        user.setTenDangNhap(username);
        user.setMatKhau(password);
        user.setEmail(email);
        user.setSoDienThoai(phone);
        user.setDanhSachQuyen(permissions);

        btnCreateAccount.setEnabled(false);
        Call<Map<String, String>> call = (editingUser != null) ? 
                ApiClient.getApiService().updateAccount(editingUser.getMaSo(), user) :
                ApiClient.getApiService().createAccount(user);

        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                btnCreateAccount.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(AdminCreateUserActivity.this, "Thành công!", Toast.LENGTH_SHORT).show();
                    if (editingUser != null) finish(); else resetFields();
                } else {
                    // PHẦN SỬA ĐỔI: Parse lỗi từ errorBody
                    try {
                        ResponseBody errorBody = response.errorBody();
                        if (errorBody != null) {
                            JSONObject jsonError = new JSONObject(errorBody.string());
                            String errorMsg = jsonError.optString("error", "Lỗi xử lý dữ liệu");
                            Toast.makeText(AdminCreateUserActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AdminCreateUserActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(AdminCreateUserActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                btnCreateAccount.setEnabled(true);
                Toast.makeText(AdminCreateUserActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> getSelectedPermissions() {
        List<String> permissions = new ArrayList<>();
        int[] ids = {R.id.cbTiepNhanHS, R.id.cbLapDanhSachLop, R.id.cbLapDanhSachHSChoLop, 
                    R.id.cbLapDanhSachNamHoc, R.id.cbLapDanhSachKhoiLop, R.id.cbLapDanhSachMonHoc,
                    R.id.cbTraCuuHS, R.id.cbNhapDiem, R.id.cbLoaiKiemTra, R.id.cbBaoCaoMon, 
                    R.id.cbBaoCaoHocKy, R.id.cbCaiDatThamSo};
        String[] codes = {"CNTNHS", "CNLDSL", "CNLDSHSCL", "CNLDSNH", "CNLDSKL", "CNLDSMH",
                         "CNTCHS", "CNNBD", "CNNDSCLKT", "CNLBCTKM", "CNLBCTKHK", "CNCDTSHT"};
        for (int i = 0; i < ids.length; i++) {
            CheckBox cb = findViewById(ids[i]);
            if (cb != null && cb.isChecked()) permissions.add(codes[i]);
        }
        return permissions;
    }

    private void resetFields() {
        edtFullName.setText("");
        edtUsername.setText("");
        edtPassword.setText("");
        edtEmail.setText("");
        edtPhone.setText("");
        clearCheckBoxes((ViewGroup) findViewById(android.R.id.content));
    }

    private void clearCheckBoxes(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View view = viewGroup.getChildAt(i);
            if (view instanceof CheckBox) ((CheckBox) view).setChecked(false);
            else if (view instanceof ViewGroup) clearCheckBoxes((ViewGroup) view);
        }
    }
}
