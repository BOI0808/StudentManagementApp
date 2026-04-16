package com.example.studentmanagementapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.ClassModel;
import com.example.studentmanagementapp.model.Subject;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GradeEntryActivity extends AppCompatActivity {

    private AutoCompleteTextView autoLop, autoMon, autoNamHoc, autoHocKy, autoLoaiKT;
    private MaterialButton btnXem, btnLuu;
    private RecyclerView rvDiem;
    private ImageButton btnBack;

    private List<Map<String, Object>> listDiem = new ArrayList<>();
    private List<Map<String, String>> semesterList = new ArrayList<>();
    private List<ClassModel> allClassList = new ArrayList<>();
    
    private String selectedMaLop = "", selectedMaMon = "", selectedMaLoaiKT = "", selectedMaHK = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grade_entry);

        initViews();
        setupFilters();

        btnBack.setOnClickListener(v -> finish());
        btnXem.setOnClickListener(v -> loadGradeList());
        btnLuu.setOnClickListener(v -> saveGrades());
    }

    private void initViews() {
        autoNamHoc = findViewById(R.id.autoCompleteNamHoc);
        autoHocKy = findViewById(R.id.autoCompleteHocKy);
        autoLop = findViewById(R.id.autoCompleteMaLopGrade);
        autoMon = findViewById(R.id.autoCompleteMonHoc);
        autoLoaiKT = findViewById(R.id.autoCompleteLoaiKT);
        btnXem = findViewById(R.id.btnXemDanhSachDiem);
        btnLuu = findViewById(R.id.btnLuuBangDiem);
        rvDiem = findViewById(R.id.rvBangDiem);
        btnBack = findViewById(R.id.btnBack);

        rvDiem.setLayoutManager(new LinearLayoutManager(this));
        
        autoLoaiKT.setOnClickListener(v -> autoLoaiKT.showDropDown());
        autoMon.setOnClickListener(v -> autoMon.showDropDown());
        autoHocKy.setOnClickListener(v -> autoHocKy.showDropDown());
        autoNamHoc.setOnClickListener(v -> autoNamHoc.showDropDown());
        autoLop.setOnClickListener(v -> autoLop.showDropDown());
    }

    private void setupFilters() {
        // 1. Tải danh mục Học kỳ
        ApiClient.getApiService().getSemesterList().enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    semesterList = response.body();
                    List<String> years = new ArrayList<>();
                    for (Map<String, String> m : semesterList) {
                        String y = m.get("namhoc");
                        if (y != null && !years.contains(y)) years.add(y);
                    }
                    autoNamHoc.setAdapter(new ArrayAdapter<>(GradeEntryActivity.this, android.R.layout.simple_list_item_1, years));
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {}
        });

        autoNamHoc.setOnItemClickListener((parent, view, position, id) -> {
            String year = (String) parent.getItemAtPosition(position);
            List<String> terms = new ArrayList<>();
            for (Map<String, String> m : semesterList) {
                if (year.equals(m.get("namhoc"))) terms.add(m.get("hocky"));
            }
            autoHocKy.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, terms));
            autoHocKy.setText("");
            autoLop.setText("");
            selectedMaHK = "";
        });

        autoHocKy.setOnItemClickListener((parent, view, position, id) -> {
            String year = autoNamHoc.getText().toString();
            String term = (String) parent.getItemAtPosition(position);
            for (Map<String, String> m : semesterList) {
                if (year.equals(m.get("namhoc")) && term.equals(m.get("hocky"))) {
                    selectedMaHK = m.get("ma");
                    break;
                }
            }
            autoLop.setText("");
            filterClasses();
        });

        // 2. Tải danh sách lớp một lần duy nhất
        ApiClient.getApiService().getClassList().enqueue(new Callback<List<ClassModel>>() {
            @Override
            public void onResponse(@NonNull Call<List<ClassModel>> call, @NonNull Response<List<ClassModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allClassList = response.body();
                }
            }
            @Override public void onFailure(@NonNull Call<List<ClassModel>> call, @NonNull Throwable t) {}
        });

        autoLop.setOnItemClickListener((p, v, pos, id) -> {
            ClassModel sel = (ClassModel) p.getItemAtPosition(pos);
            selectedMaLop = sel.getMaLop();
        });

        // 3. Tải môn học và loại kiểm tra (giữ nguyên các bước trước)
        ApiClient.getApiService().getSubjectList().enqueue(new Callback<List<Subject>>() {
            @Override
            public void onResponse(@NonNull Call<List<Subject>> call, @NonNull Response<List<Subject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Subject> subjs = response.body();
                    List<String> names = new ArrayList<>();
                    for(Subject s : subjs) names.add(s.getTenMonHoc());
                    autoMon.setAdapter(new ArrayAdapter<>(GradeEntryActivity.this, android.R.layout.simple_list_item_1, names));
                    autoMon.setOnItemClickListener((p, v, pos, id) -> selectedMaMon = subjs.get(pos).getMaMonHoc());
                }
            }
            @Override public void onFailure(@NonNull Call<List<Subject>> call, @NonNull Throwable t) {}
        });

        ApiClient.getApiService().getTestTypeList().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> types = response.body();
                    List<String> names = new ArrayList<>();
                    for(Map<String, Object> m : types) names.add(String.valueOf(m.get("TenLoaiKiemTra")));
                    autoLoaiKT.setAdapter(new ArrayAdapter<>(GradeEntryActivity.this, android.R.layout.simple_list_item_1, names));
                    autoLoaiKT.setOnItemClickListener((p, v, pos, id) -> selectedMaLoaiKT = String.valueOf(types.get(pos).get("MaLoaiKiemTra")));
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void filterClasses() {
        if (selectedMaHK.isEmpty()) return;
        List<ClassModel> filtered = new ArrayList<>();
        for (ClassModel c : allClassList) {
            // So sánh mã HK chính xác
            if (selectedMaHK.equalsIgnoreCase(c.getMaHocKyNamHoc())) filtered.add(c);
        }
        autoLop.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filtered));
        if (!filtered.isEmpty()) autoLop.showDropDown();
    }

    private void loadGradeList() {
        if(selectedMaLop.isEmpty() || selectedMaMon.isEmpty() || selectedMaLoaiKT.isEmpty() || selectedMaHK.isEmpty()){
            Toast.makeText(this, "Vui lòng chọn đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        ApiClient.getApiService().getHocSinhNhapDiem(selectedMaLop, selectedMaMon, selectedMaLoaiKT, selectedMaHK).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listDiem = response.body();
                    setupGradeAdapter();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void setupGradeAdapter() {
        GenericAdapter<Map<String, Object>> adapter = new GenericAdapter<>(listDiem, R.layout.item_grade_entry, (item, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvMaHS)).setText(String.valueOf(item.get("maHocSinh")));
            ((TextView) itemView.findViewById(R.id.tvHoTen)).setText(String.valueOf(item.get("hoTen")));
            EditText edt = itemView.findViewById(R.id.edtDiem);
            edt.setText(String.valueOf(item.get("diem")));
            edt.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    item.put("diem", s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        });
        rvDiem.setAdapter(adapter);
    }

    private void saveGrades() {
        if (selectedMaLop.isEmpty() || selectedMaMon.isEmpty() || selectedMaLoaiKT.isEmpty()) return;
        
        Map<String, Object> body = new HashMap<>();
        body.put("MaLop", selectedMaLop);
        body.put("MaMonHoc", selectedMaMon);
        body.put("MaLoaiKiemTra", selectedMaLoaiKT);
        
        List<Map<String, Object>> danhSach = new ArrayList<>();
        for (Map<String, Object> m : listDiem) {
            Map<String, Object> d = new HashMap<>();
            d.put("maHocSinh", m.get("maHocSinh"));
            d.put("diem", m.get("diem"));
            danhSach.add(d);
        }
        body.put("DanhSachDiem", danhSach);

        btnLuu.setEnabled(false);
        // Endpoint: api/grades/nhap-diem
        ApiClient.getApiService().saveGrades(body).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                btnLuu.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(GradeEntryActivity.this, "Lưu điểm thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GradeEntryActivity.this, "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                btnLuu.setEnabled(true);
                Toast.makeText(GradeEntryActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
