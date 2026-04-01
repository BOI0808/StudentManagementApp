package com.example.studentmanagementapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class AdminCreateUserActivity extends AppCompatActivity {

    private EditText edtFullName, edtStaffId, edtUsername, edtPassword, edtEmail, edtPhone;
    private CheckBox cbTiepNhanHocSinh, cbLapDanhSachLop, cbLapDanhMuc, cbTraCuuHocSinh;
    private CheckBox cbNhapBangDiem, cbNhapLoaiKiemTra, cbBaoCaoMon, cbBaoCaoHocKy;
    private Button btnCreateAccount;
    private TextView tvLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_create_user);

        initViews();
        setEvents();
    }

    private void initViews() {
        edtFullName = findViewById(R.id.edtFullName);
        edtStaffId = findViewById(R.id.edtStaffId);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);

        cbTiepNhanHocSinh = findViewById(R.id.cbTiepNhanHocSinh);
        cbLapDanhSachLop = findViewById(R.id.cbLapDanhSachLop);
        cbLapDanhMuc = findViewById(R.id.cbLapDanhMuc);
        cbTraCuuHocSinh = findViewById(R.id.cbTraCuuHocSinh);
        cbNhapBangDiem = findViewById(R.id.cbNhapBangDiem);
        cbNhapLoaiKiemTra = findViewById(R.id.cbNhapLoaiKiemTra);
        cbBaoCaoMon = findViewById(R.id.cbBaoCaoMon);
        cbBaoCaoHocKy = findViewById(R.id.cbBaoCaoHocKy);

        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvLogout = findViewById(R.id.tvLogout);
    }

    private void setEvents() {
        // Xử lý khi nhấn nút Xác nhận
        btnCreateAccount.setOnClickListener(v -> handleCreateAccount());

        // Xử lý khi nhấn Enter tại các ô nhập liệu quan trọng
        TextView.OnEditorActionListener enterListener = (v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                handleCreateAccount();
                return true;
            }
            return false;
        };

        edtPassword.setOnEditorActionListener(enterListener);
        edtPhone.setOnEditorActionListener(enterListener);

        tvLogout.setOnClickListener(v -> {
            Intent intent = new Intent(AdminCreateUserActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void clearFields() {
        // Xóa sạch nội dung các EditText
        if (edtFullName != null) edtFullName.setText("");
        if (edtStaffId != null) edtStaffId.setText("");
        if (edtUsername != null) edtUsername.setText("");
        if (edtPassword != null) edtPassword.setText("");
        if (edtEmail != null) edtEmail.setText("");
        if (edtPhone != null) edtPhone.setText("");
        
        // Bỏ chọn tất cả các CheckBox
        if (cbTiepNhanHocSinh != null) cbTiepNhanHocSinh.setChecked(false);
        if (cbLapDanhSachLop != null) cbLapDanhSachLop.setChecked(false);
        if (cbLapDanhMuc != null) cbLapDanhMuc.setChecked(false);
        if (cbTraCuuHocSinh != null) cbTraCuuHocSinh.setChecked(false);
        if (cbNhapBangDiem != null) cbNhapBangDiem.setChecked(false);
        if (cbNhapLoaiKiemTra != null) cbNhapLoaiKiemTra.setChecked(false);
        if (cbBaoCaoMon != null) cbBaoCaoMon.setChecked(false);
        if (cbBaoCaoHocKy != null) cbBaoCaoHocKy.setChecked(false);

        // Ẩn bàn phím sau khi reset
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCreateAccount() {
        String fullName = edtFullName.getText().toString().trim();
        String staffId = edtStaffId.getText().toString().trim();
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (fullName.isEmpty() || staffId.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedPermissions = new ArrayList<>();
        if (cbTiepNhanHocSinh.isChecked()) selectedPermissions.add("TIEP_NHAN_HOC_SINH");
        if (cbLapDanhSachLop.isChecked()) selectedPermissions.add("LAP_DAN_SACH_LOP");
        if (cbLapDanhMuc.isChecked()) selectedPermissions.add("LAP_DAN_MUC");
        if (cbTraCuuHocSinh.isChecked()) selectedPermissions.add("TRA_CUU_HOC_SINH");
        if (cbNhapBangDiem.isChecked()) selectedPermissions.add("NHAP_BANG_DIEM");
        if (cbNhapLoaiKiemTra.isChecked()) selectedPermissions.add("NHAP_LOAI_KIEM_TRA");
        if (cbBaoCaoMon.isChecked()) selectedPermissions.add("BAO_CAO_MON");
        if (cbBaoCaoHocKy.isChecked()) selectedPermissions.add("BAO_CAO_HOC_KY");

        showConfirmDialog(fullName, username, selectedPermissions);
    }

    private void showConfirmDialog(String fullName, String username, List<String> permissions) {
        String message = "Bạn có chắc chắn muốn tạo tài khoản cho nhân viên:\n\n" +
                "- Họ tên: " + fullName + "\n" +
                "- Tài khoản: " + username + "\n" +
                "- Số lượng quyền: " + permissions.size() + " quyền";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận tạo tài khoản")
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("XÁC NHẬN", (dialog, which) -> {
                    // Reset ngay khi nhấn XÁC NHẬN
                    clearFields();
                    Toast.makeText(this, "Đã tạo tài khoản thành công!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("HỦY BỎ", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }
}
