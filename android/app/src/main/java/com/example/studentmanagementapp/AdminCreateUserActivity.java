package com.example.studentmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCreateUserActivity extends AppCompatActivity {

    private TextView btnLogout, tvTitle;
    private TextInputEditText edtFullName, edtUsername, edtPassword, edtEmail, edtPhone;
    private MaterialButton btnCreateAccount, btnXemDanhSach;
    private User editingUser = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_create_user);

        btnLogout = findViewById(R.id.btnLogout);
        tvTitle = findViewById(R.id.tvTitle);
        edtFullName = findViewById(R.id.edtFullName);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnXemDanhSach = findViewById(R.id.btnXemDanhSach);

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

        checkEditMode();
    }

    private void checkEditMode() {
        editingUser = (User) getIntent().getSerializableExtra("user_data");
        if (editingUser != null) {
            if (tvTitle != null) tvTitle.setText("Cập Nhật Tài Khoản");
            btnCreateAccount.setText("CẬP NHẬT TÀI KHOẢN");
            
            edtFullName.setText(editingUser.getHoTen());
            edtUsername.setText(editingUser.getTenDangNhap());
            edtUsername.setEnabled(false); // Không cho sửa username
            edtPassword.setText(editingUser.getMatKhau());
            edtEmail.setText(editingUser.getEmail());
            edtPhone.setText(editingUser.getSoDienThoai());
            
            setPermissions(editingUser.getDanhSachQuyen());
        }
    }

    private void setPermissions(List<String> permissions) {
        if (permissions == null) return;
        ((CheckBox)findViewById(R.id.cbTiepNhanHS)).setChecked(permissions.contains("CNTNHS"));
        ((CheckBox)findViewById(R.id.cbLapDanhSachLop)).setChecked(permissions.contains("CNLDSL"));
        ((CheckBox)findViewById(R.id.cbLapDanhSachHSChoLop)).setChecked(permissions.contains("CNLDSHSCL"));
        ((CheckBox)findViewById(R.id.cbLapDanhSachNamHoc)).setChecked(permissions.contains("CNLDSNH"));
        ((CheckBox)findViewById(R.id.cbLapDanhSachKhoiLop)).setChecked(permissions.contains("CNLDSKL"));
        ((CheckBox)findViewById(R.id.cbLapDanhSachMonHoc)).setChecked(permissions.contains("CNLDSMH"));
        ((CheckBox)findViewById(R.id.cbTraCuuHS)).setChecked(permissions.contains("CNTCHS"));
        ((CheckBox)findViewById(R.id.cbNhapDiem)).setChecked(permissions.contains("CNNBD"));
        ((CheckBox)findViewById(R.id.cbLoaiKiemTra)).setChecked(permissions.contains("CNNDSCLKT"));
        ((CheckBox)findViewById(R.id.cbBaoCaoMon)).setChecked(permissions.contains("CNLBCTKM"));
        ((CheckBox)findViewById(R.id.cbBaoCaoHocKy)).setChecked(permissions.contains("CNLBCTKHK"));
        ((CheckBox)findViewById(R.id.cbCaiDatThamSo)).setChecked(permissions.contains("CNCDTSHT"));
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
            Toast.makeText(this, "Vui lòng chọn ít nhất một quyền hệ thống", Toast.LENGTH_SHORT).show();
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

        Call<Map<String, String>> call;
        if (editingUser != null) {
            call = ApiClient.getApiService().updateAccount(editingUser.getMaSo(), user);
        } else {
            call = ApiClient.getApiService().createAccount(user);
        }

        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                btnCreateAccount.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().get("message");
                    Toast.makeText(AdminCreateUserActivity.this, message, Toast.LENGTH_LONG).show();
                    if (editingUser != null) {
                        finish(); // Quay lại sau khi cập nhật
                    } else {
                        resetFields();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        JSONObject jObjError = new JSONObject(errorBody);
                        String errorMsg = jObjError.has("error") ? jObjError.getString("error") : "Lỗi xử lý dữ liệu";
                        Toast.makeText(AdminCreateUserActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(AdminCreateUserActivity.this, "Lỗi Server", Toast.LENGTH_SHORT).show();
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
        if (((CheckBox)findViewById(R.id.cbTiepNhanHS)).isChecked()) permissions.add("CNTNHS");
        if (((CheckBox)findViewById(R.id.cbLapDanhSachLop)).isChecked()) permissions.add("CNLDSL");
        if (((CheckBox)findViewById(R.id.cbLapDanhSachHSChoLop)).isChecked()) permissions.add("CNLDSHSCL");
        if (((CheckBox)findViewById(R.id.cbLapDanhSachNamHoc)).isChecked()) permissions.add("CNLDSNH");
        if (((CheckBox)findViewById(R.id.cbLapDanhSachKhoiLop)).isChecked()) permissions.add("CNLDSKL");
        if (((CheckBox)findViewById(R.id.cbLapDanhSachMonHoc)).isChecked()) permissions.add("CNLDSMH");
        if (((CheckBox)findViewById(R.id.cbTraCuuHS)).isChecked()) permissions.add("CNTCHS");
        if (((CheckBox)findViewById(R.id.cbNhapDiem)).isChecked()) permissions.add("CNNBD");
        if (((CheckBox)findViewById(R.id.cbLoaiKiemTra)).isChecked()) permissions.add("CNNDSCLKT");
        if (((CheckBox)findViewById(R.id.cbBaoCaoMon)).isChecked()) permissions.add("CNLBCTKM");
        if (((CheckBox)findViewById(R.id.cbBaoCaoHocKy)).isChecked()) permissions.add("CNLBCTKHK");
        if (((CheckBox)findViewById(R.id.cbCaiDatThamSo)).isChecked()) permissions.add("CNCDTSHT");
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
            if (view instanceof CheckBox) {
                ((CheckBox) view).setChecked(false);
            } else if (view instanceof ViewGroup) {
                clearCheckBoxes((ViewGroup) view);
            }
        }
    }
}
