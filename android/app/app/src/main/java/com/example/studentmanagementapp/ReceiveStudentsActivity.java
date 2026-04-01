package com.example.studentmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReceiveStudentsActivity extends AppCompatActivity {

    private TextInputEditText edtHoTen, edtDiaChi, edtEmail, edtGioiTinhKhac;
    private AutoCompleteTextView spnNgay, spnThang, spnNam;
    private TextInputLayout tilGioiTinhKhac;
    private RadioGroup rgGioiTinh;
    private Button btnTiepNhan;
    private ImageButton btnBack;

    // QUY ĐỊNH ĐỘ TUỔI (Có thể thay đổi dễ dàng)
    private final int TUOI_TOI_THIEU = 15;
    private final int TUOI_TOI_DA = 20;
    
    private static List<Student> studentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_students);

        initViews();
        setupSpinners();
        setEvents();
    }

    private void initViews() {
        edtHoTen = findViewById(R.id.edtHoTen);
        edtDiaChi = findViewById(R.id.edtDiaChi);
        edtEmail = findViewById(R.id.edtEmail);
        edtGioiTinhKhac = findViewById(R.id.edtGioiTinhKhac);
        
        spnNgay = findViewById(R.id.spnNgay);
        spnThang = findViewById(R.id.spnThang);
        spnNam = findViewById(R.id.spnNam);
        
        tilGioiTinhKhac = findViewById(R.id.tilGioiTinhKhac);
        rgGioiTinh = findViewById(R.id.rgGioiTinh);
        btnTiepNhan = findViewById(R.id.btnTiepNhan);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupSpinners() {
        // 1. Setup Tháng (1-12)
        List<String> listThang = new ArrayList<>();
        for (int i = 1; i <= 12; i++) listThang.add(String.valueOf(i));
        spnThang.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listThang));

        // 2. Setup Năm (Mở rộng từ 1990 đến năm hiện tại để đảm bảo tính tiến hóa)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<String> listNam = new ArrayList<>();
        for (int i = currentYear; i >= 1990; i--) { // Sắp xếp từ năm mới nhất về cũ nhất
            listNam.add(String.valueOf(i));
        }
        spnNam.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listNam));

        // Mặc định chọn năm phù hợp với tuổi tối thiểu để người dùng đỡ phải cuộn
        spnNam.setText(String.valueOf(currentYear - TUOI_TOI_THIEU), false);

        updateDaysSpinner();
    }

    private void updateDaysSpinner() {
        String thangStr = spnThang.getText().toString();
        String namStr = spnNam.getText().toString();

        int month = thangStr.isEmpty() ? 1 : Integer.parseInt(thangStr);
        int year = namStr.isEmpty() ? Calendar.getInstance().get(Calendar.YEAR) - TUOI_TOI_THIEU : Integer.parseInt(namStr);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        List<String> listNgay = new ArrayList<>();
        for (int i = 1; i <= maxDays; i++) listNgay.add(String.valueOf(i));
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listNgay);
        spnNgay.setAdapter(adapter);

        String currentDay = spnNgay.getText().toString();
        if (!currentDay.isEmpty() && Integer.parseInt(currentDay) > maxDays) {
            spnNgay.setText("");
        }
    }

    private void setEvents() {
        btnBack.setOnClickListener(v -> finish());

        rgGioiTinh.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbKhac) {
                tilGioiTinhKhac.setVisibility(View.VISIBLE);
            } else {
                tilGioiTinhKhac.setVisibility(View.GONE);
            }
        });

        spnThang.setOnItemClickListener((parent, view, position, id) -> updateDaysSpinner());
        spnNam.setOnItemClickListener((parent, view, position, id) -> updateDaysSpinner());

        btnTiepNhan.setOnClickListener(v -> handleTiepNhan());

        edtEmail.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                handleTiepNhan();
                return true;
            }
            return false;
        });
    }

    private void handleTiepNhan() {
        String hoTen = edtHoTen.getText().toString().trim();
        String ngay = spnNgay.getText().toString();
        String thang = spnThang.getText().toString();
        String nam = spnNam.getText().toString();
        String email = edtEmail.getText().toString().trim();

        if (hoTen.isEmpty() || ngay.isEmpty() || thang.isEmpty() || nam.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ Họ tên và Ngày sinh!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra logic độ tuổi quy định của trường (15-20 tuổi)
        int selectedYear = Integer.parseInt(nam);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int age = currentYear - selectedYear;

        if (age < TUOI_TOI_THIEU || age > TUOI_TOI_DA) {
            Toast.makeText(this, "Tuổi học sinh không đúng quy định (15-20 tuổi)!", Toast.LENGTH_LONG).show();
            return;
        }

        String ngaySinhStr = ngay + "/" + thang + "/" + nam;

        if (!email.isEmpty() && isEmailTrungLap(email)) {
            Toast.makeText(this, "Email này đã tồn tại trong hệ thống!", Toast.LENGTH_SHORT).show();
            return;
        }

        String maHS = generateStudentId();
        studentList.add(new Student(hoTen, ngaySinhStr, email));

        Toast.makeText(this, "Tiếp nhận thành công! Mã HS: " + maHS, Toast.LENGTH_LONG).show();
        clearFields();
    }

    private String generateStudentId() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int nextNumber = studentList.size() + 1;
        return String.format("%d%05d", currentYear, nextNumber);
    }

    private boolean isEmailTrungLap(String email) {
        for (Student s : studentList) {
            if (s.getEmail().equalsIgnoreCase(email)) return true;
        }
        return false;
    }

    private void clearFields() {
        edtHoTen.setText("");
        spnNgay.setText("");
        spnThang.setText("");
        // Reset về năm mặc định (currentYear - 15)
        spnNam.setText(String.valueOf(Calendar.getInstance().get(Calendar.YEAR) - TUOI_TOI_THIEU), false);
        edtDiaChi.setText("");
        edtEmail.setText("");
        edtGioiTinhKhac.setText("");
        rgGioiTinh.clearCheck();
        tilGioiTinhKhac.setVisibility(View.GONE);
        edtHoTen.requestFocus();
    }
}
