package com.example.studentmanagementapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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

    private TextView tvTitle;
    private ImageButton btnLogout;
    private TextInputLayout tilFullName, tilEmail, tilPhone, tilUsername, tilPassword;
    private TextInputEditText edtFullName, edtUsername, edtPassword, edtEmail, edtPhone;
    private MaterialButton btnCreateAccount, btnXemDanhSach;
    private ImageButton btnImportExcel;
    private MaterialButton btnQuickAdmin, btnQuickClear;
    private ImageButton btnAddCustomRole;
    private LinearLayout llQuickButtons;
    private LinearProgressIndicator loadingIndicator;
    private User editingUser = null;
    private Uri selectedFileUri;

    private static final String PREFS_NAME = "CustomRolesPrefs";
    private static final String KEY_ROLES = "custom_roles";

    private final int[] permissionIds = {R.id.cbTiepNhanHS, R.id.cbLapDanhSachLop, R.id.cbLapDanhSachHSChoLop, 
                R.id.cbLapDanhSachNamHoc, R.id.cbLapDanhSachKhoiLop, R.id.cbLapDanhSachMonHoc,
                R.id.cbTraCuuHS, R.id.cbNhapDiem, R.id.cbLoaiKiemTra, R.id.cbBaoCaoMon, 
                R.id.cbBaoCaoHocKy, R.id.cbCaiDatThamSo};
                
    private final String[] permissionCodes = {"CNTNHS", "CNLDSL", "CNLDSHSCL", "CNLDSNH", "CNLDSKL", "CNLDSMH",
                     "CNTCHS", "CNNBD", "CNNDSCLKT", "CNLBCTKM", "CNLBCTKHK", "CNCDTSHT"};

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    showLoading();
                    new Thread(() -> processExcelFile(uri)).start();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_create_user);

        System.setProperty("java.io.tmpdir", getCacheDir().getAbsolutePath());

        initViews();
        setupListeners();
        setupErrorClearing();
        checkEditMode();
    }

    private void initViews() {
        btnLogout = findViewById(R.id.btnLogout);
        tvTitle = findViewById(R.id.tvTitle);
        
        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);

        edtFullName = findViewById(R.id.edtFullName);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnXemDanhSach = findViewById(R.id.btnXemDanhSach);
        btnImportExcel = findViewById(R.id.btnImportExcel);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        
        btnQuickAdmin = findViewById(R.id.btnQuickAdmin);
        btnQuickClear = findViewById(R.id.btnQuickClear);
        btnAddCustomRole = findViewById(R.id.btnAddCustomRole);
        llQuickButtons = findViewById(R.id.llQuickButtons);

        loadCustomRolesDynamic();
    }

    private void loadCustomRolesDynamic() {
        // Clear old dynamic buttons (keep btnAddCustomRole, btnQuickAdmin, btnQuickClear)
        int childCount = llQuickButtons.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View v = llQuickButtons.getChildAt(i);
            if (v.getId() != R.id.btnAddCustomRole && v.getId() != R.id.btnQuickAdmin && v.getId() != R.id.btnQuickClear) {
                llQuickButtons.removeViewAt(i);
            }
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String rolesJson = prefs.getString(KEY_ROLES, "{}");
        try {
            JSONObject json = new JSONObject(rolesJson);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String roleName = keys.next();
                addDynamicButton(roleName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addDynamicButton(String roleName) {
        MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
        btn.setLayoutParams(params);
        btn.setText(roleName);
        btn.setAllCaps(false);
        btn.setTextSize(12);
        btn.setPadding(32, 0, 32, 0);
        btn.setStrokeColorResource(R.color.blue_primary);
        btn.setOnClickListener(v -> setQuickPermissions(roleName));
        
        // Add before btnQuickAdmin
        int index = llQuickButtons.indexOfChild(findViewById(R.id.btnQuickAdmin));
        llQuickButtons.addView(btn, index);
    }

    private void setupErrorClearing() {
        edtFullName.addTextChangedListener(new SimpleTextWatcher(tilFullName));
        edtEmail.addTextChangedListener(new SimpleTextWatcher(tilEmail));
        edtPhone.addTextChangedListener(new SimpleTextWatcher(tilPhone));
        edtUsername.addTextChangedListener(new SimpleTextWatcher(tilUsername));
        edtPassword.addTextChangedListener(new SimpleTextWatcher(tilPassword));
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

    private void setupListeners() {
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

        btnQuickAdmin.setOnClickListener(v -> setQuickPermissions("ADMIN"));
        btnQuickClear.setOnClickListener(v -> setQuickPermissions("CLEAR"));
        btnAddCustomRole.setOnClickListener(v -> showAddRoleDialog());
    }

    private void showAddRoleDialog() {
        List<String> selectedCodes = getSelectedPermissions();
        if (selectedCodes.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một quyền trước khi lưu nhóm!", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Ví dụ: Giáo vụ, Kế toán...");
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm nhóm quyền mới")
                .setMessage("Nhập tên cho nhóm quyền các tính năng bạn vừa chọn:")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        saveCustomRole(name, selectedCodes);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveCustomRole(String name, List<String> codes) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String rolesJson = prefs.getString(KEY_ROLES, "{}");
        try {
            JSONObject json = new JSONObject(rolesJson);
            JSONArray array = new JSONArray();
            for (String code : codes) array.put(code);
            json.put(name, array);
            
            prefs.edit().putString(KEY_ROLES, json.toString()).apply();
            loadCustomRolesDynamic();
            Toast.makeText(this, "Đã lưu nhóm quyền: " + name, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setQuickPermissions(String role) {
        List<String> targetCodes = new ArrayList<>();
        if (role.equals("ADMIN")) {
            targetCodes = Arrays.asList(permissionCodes);
        } else if (!role.equals("CLEAR")) {
            // Check dynamic roles
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String rolesJson = prefs.getString(KEY_ROLES, "{}");
            try {
                JSONObject json = new JSONObject(rolesJson);
                if (json.has(role)) {
                    JSONArray array = json.getJSONArray(role);
                    for (int i = 0; i < array.length(); i++) {
                        targetCodes.add(array.getString(i));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < permissionIds.length; i++) {
            CheckBox cb = findViewById(permissionIds[i]);
            if (cb != null) {
                cb.setChecked(targetCodes.contains(permissionCodes[i]));
            }
        }
    }

    private void showLoading() {
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.VISIBLE);
        setInputsEnabled(false);
        if (btnCreateAccount != null) btnCreateAccount.setText("Đang xử lý...");
    }

    private void hideLoading() {
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
        setInputsEnabled(true);
        if (btnCreateAccount != null) {
            btnCreateAccount.setText(editingUser != null ? "CẬP NHẬT TÀI KHOẢN" : "TẠO TÀI KHOẢN");
        }
    }

    private void setInputsEnabled(boolean enabled) {
        edtFullName.setEnabled(enabled);
        if (editingUser == null) edtUsername.setEnabled(enabled);
        edtPassword.setEnabled(enabled);
        edtEmail.setEnabled(enabled);
        edtPhone.setEnabled(enabled);
        btnCreateAccount.setEnabled(enabled);
        btnXemDanhSach.setEnabled(enabled);
        btnImportExcel.setEnabled(enabled);
        btnQuickAdmin.setEnabled(enabled);
        btnQuickClear.setEnabled(enabled);
        btnAddCustomRole.setEnabled(enabled);

        // Enable/Disable dynamic buttons
        for (int i = 0; i < llQuickButtons.getChildCount(); i++) {
            View v = llQuickButtons.getChildAt(i);
            if (v instanceof MaterialButton) {
                v.setEnabled(enabled);
            }
        }
        
        for (int id : permissionIds) {
            CheckBox cb = findViewById(id);
            if (cb != null) cb.setEnabled(enabled);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (editingUser == null) {
            resetFields();
        }
    }

    private void processExcelFile(Uri uri) {
        try {
            File tempFile = copyUriToInternalStorage(uri);
            List<User> usersFromExcel = new ArrayList<>();
            
            try (Workbook workbook = WorkbookFactory.create(tempFile)) {
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) throw new Exception("File Excel không có dữ liệu tiêu đề.");

                int idxHoTen = -1, idxUsername = -1, idxMatKhau = -1, idxEmail = -1, idxSdt = -1, idxQuyen = -1;

                for (Cell cell : headerRow) {
                    String title = formatter.formatCellValue(cell).trim().toLowerCase();
                    if (title.contains("họ và tên") || title.contains("hoten") || title.equals("họ tên")) idxHoTen = cell.getColumnIndex();
                    else if (title.contains("tên đăng nhập") || title.contains("tendangnhap") || title.contains("username") || title.contains("tài khoản")) idxUsername = cell.getColumnIndex();
                    else if (title.contains("mật khẩu") || title.contains("matkhau") || title.contains("password")) idxMatKhau = cell.getColumnIndex();
                    else if (title.contains("email")) idxEmail = cell.getColumnIndex();
                    else if (title.contains("số điện thoại") || title.contains("sodienthoai") || title.equals("sđt") || title.equals("phone")) idxSdt = cell.getColumnIndex();
                    else if (title.contains("quyền hạn") || title.contains("quyenhan") || title.contains("quyen") || title.contains("rights")) idxQuyen = cell.getColumnIndex();
                }

                if (idxHoTen == -1 || idxUsername == -1 || idxMatKhau == -1) {
                    throw new Exception("Không tìm thấy các cột bắt buộc trong file (Họ tên, Tên đăng nhập, Mật khẩu). Vui lòng kiểm tra lại tiêu đề cột.");
                }

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    User user = new User();
                    user.setHoTen(formatter.formatCellValue(row.getCell(idxHoTen)).trim());
                    user.setTenDangNhap(formatter.formatCellValue(row.getCell(idxUsername)).trim());
                    user.setMatKhau(formatter.formatCellValue(row.getCell(idxMatKhau)).trim());
                    
                    if (idxEmail != -1) user.setEmail(formatter.formatCellValue(row.getCell(idxEmail)).trim());
                    if (idxSdt != -1) user.setSoDienThoai(formatter.formatCellValue(row.getCell(idxSdt)).trim());
                    
                    if (idxQuyen != -1) {
                        String rightsStr = formatter.formatCellValue(row.getCell(idxQuyen)).trim();
                        if (!rightsStr.isEmpty()) {
                            user.setDanhSachQuyen(Arrays.asList(rightsStr.split(",")));
                        }
                    }
                    
                    if (!user.getTenDangNhap().isEmpty()) {
                        usersFromExcel.add(user);
                    }
                }
            }
            
            runOnUiThread(() -> {
                hideLoading();
                if (!usersFromExcel.isEmpty()) {
                    showPreviewDialog(usersFromExcel);
                } else {
                    new MaterialAlertDialogBuilder(this)
                        .setTitle("Thông báo")
                        .setMessage("Không tìm thấy dữ liệu hợp lệ trong file")
                        .setPositiveButton("OK", null)
                        .show();
                }
            });

        } catch (Exception e) {
            Log.e("ExcelError", "Lỗi xử lý file: ", e);
            runOnUiThread(() -> {
                hideLoading();
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Lỗi đọc file")
                        .setMessage(e.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
            });
        }
    }

    private File copyUriToInternalStorage(Uri uri) throws Exception {
        File destinationFile = new File(getCacheDir(), "import_cache.xlsx");
        int maxRetries = 3;
        int retryCount = 0;
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
                Thread.sleep(500);
            }
        }
        throw new Exception("Lỗi truy cập tệp.");
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

            showLoading();
            ApiClient.getApiService().importExcel(body).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    hideLoading();
                    if (response.isSuccessful()) {
                        new MaterialAlertDialogBuilder(AdminCreateUserActivity.this)
                                .setTitle("Thành công")
                                .setMessage("Đã tạo tài khoản từ tệp Excel thành công!")
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
                    new MaterialAlertDialogBuilder(AdminCreateUserActivity.this)
                            .setTitle("Thất bại")
                            .setMessage("Lỗi kết nối Server: " + t.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
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
                        if (row != -1) errorList.add("Dòng " + row + ": " + message);
                        else errorList.add(message);
                    }
                } else if (jsonObject.has("error")) {
                    errorList.add(jsonObject.getString("error"));
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
                .setMessage(sb.toString().trim())
                .setPositiveButton("OK", null)
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
            
            // Xóa bỏ hoàn toàn dòng edtPassword.setText(editingUser.getMatKhau());
            edtPassword.setText("");
            
            edtEmail.setText(editingUser.getEmail());
            edtPhone.setText(editingUser.getSoDienThoai());
            setPermissions(editingUser.getDanhSachQuyen());
            
            // Hiển thị helper text khi cập nhật tài khoản
            if (tilPassword != null) {
                tilPassword.setHelperText("Để trống nếu không muốn thay đổi mật khẩu");
            }
        } else {
            // Ẩn helper text khi tạo mới tài khoản
            if (tilPassword != null) {
                tilPassword.setHelperText(null);
            }
        }
    }

    private void setPermissions(List<String> permissions) {
        if (permissions == null) return;
        for (int i = 0; i < permissionIds.length; i++) {
            CheckBox cb = findViewById(permissionIds[i]);
            if (cb != null) cb.setChecked(permissions.contains(permissionCodes[i]));
        }
    }

    private void performCreateOrUpdateAccount() {
        clearAllErrors();
        
        String fullName = edtFullName.getText().toString().trim();
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();

        boolean hasError = false;

        if (fullName.isEmpty()) { tilFullName.setError("Vui lòng nhập họ và tên"); hasError = true; }
        if (username.isEmpty()) { tilUsername.setError("Vui lòng nhập tên đăng nhập"); hasError = true; }
        
        // Khi cập nhật, mật khẩu có thể để trống. Chỉ bắt buộc nhập khi tạo tài khoản mới.
        if (editingUser == null && password.isEmpty()) { 
            tilPassword.setError("Vui lòng nhập mật khẩu"); 
            hasError = true; 
        }
        
        if (email.isEmpty()) { tilEmail.setError("Vui lòng nhập email"); hasError = true; }
        if (phone.isEmpty()) { tilPhone.setError("Vui lòng nhập số điện thoại"); hasError = true; }

        if (hasError) return;

        List<String> permissions = getSelectedPermissions();
        if (permissions.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Thất bại")
                    .setMessage("Vui lòng chọn ít nhất một quyền hạn cho tài khoản này.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        User user = new User();
        user.setHoTen(fullName);
        user.setTenDangNhap(username);
        
        // Đảm bảo mật khẩu gửi lên là chuỗi rỗng "" khi Admin để trống ô nhập mật khẩu ở chế độ cập nhật
        if (editingUser != null && password.isEmpty()) {
            user.setMatKhau("");
        } else {
            user.setMatKhau(password);
        }

        user.setEmail(email);
        user.setSoDienThoai(phone);
        user.setDanhSachQuyen(permissions);

        showLoading();
        Call<Map<String, String>> call = (editingUser != null) ? 
                ApiClient.getApiService().updateAccount(editingUser.getMaSo(), user) :
                ApiClient.getApiService().createAccount(user);

        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    String action = (editingUser != null) ? "cập nhật" : "tạo mới";
                    String message = "Đã " + action + " tài khoản cho " + fullName + " thành công!";

                    new MaterialAlertDialogBuilder(AdminCreateUserActivity.this)
                            .setTitle("Thành công")
                            .setMessage(message)
                            .setCancelable(false)
                            .setNeutralButton("Xem danh sách", (dialog, which) -> {
                                Intent intent = new Intent(AdminCreateUserActivity.this, AdminUserListActivity.class);
                                startActivity(intent);
                            })
                            .setPositiveButton((editingUser != null) ? "Đóng" : "Tạo tiếp", (dialog, which) -> {
                                if (editingUser != null) {
                                    finish();
                                } else {
                                    resetFields();
                                }
                            })
                            .show();
                } else {
                    try {
                        ResponseBody errorBody = response.errorBody();
                        if (errorBody != null) {
                            JSONObject jsonError = new JSONObject(errorBody.string());
                            String errorMsg = jsonError.optString("error", "Lỗi xử lý dữ liệu");
                            new MaterialAlertDialogBuilder(AdminCreateUserActivity.this)
                                    .setTitle("Thông báo")
                                    .setMessage(errorMsg)
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(AdminCreateUserActivity.this, "Lỗi server", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                hideLoading();
                new MaterialAlertDialogBuilder(AdminCreateUserActivity.this)
                        .setTitle("Lỗi kết nối")
                        .setMessage("Không thể kết nối đến máy chủ. Vui lòng thử lại sau.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void clearAllErrors() {
        tilFullName.setError(null);
        tilUsername.setError(null);
        tilPassword.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
    }

    private List<String> getSelectedPermissions() {
        List<String> permissions = new ArrayList<>();
        for (int i = 0; i < permissionIds.length; i++) {
            CheckBox cb = findViewById(permissionIds[i]);
            if (cb != null && cb.isChecked()) permissions.add(permissionCodes[i]);
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
        clearAllErrors();
        
        // Reset helper text when switching back to create mode
        if (tilPassword != null) {
            tilPassword.setHelperText(null);
        }
    }

    private void clearCheckBoxes(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View view = viewGroup.getChildAt(i);
            if (view instanceof CheckBox) ((CheckBox) view).setChecked(false);
            else if (view instanceof ViewGroup) clearCheckBoxes((ViewGroup) view);
        }
    }
}
