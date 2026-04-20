package com.example.studentmanagementapp;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
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
import com.example.studentmanagementapp.model.ClassModel;
import com.example.studentmanagementapp.model.Student;
import com.google.android.material.button.MaterialButton;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateClassListActivity extends AppCompatActivity {

    private AutoCompleteTextView autoLop, autoHocSinh;
    private MaterialButton btnThem, btnLuu, btnImportExcel;
    private RecyclerView rvHocSinh;
    private ImageButton btnBack;
    
    private final List<Student> listHocSinhSelected = new ArrayList<>();
    private GenericAdapter<Student> adapter;
    private String selectedMaLop = "";
    private Student selectedStudentToAdd = null;
    
    // Bản đồ lưu trữ lỗi: Key = MaHocSinh, Value = Thông báo lỗi
    private final Map<String, String> mapErrors = new HashMap<>();
    
    private boolean isProgrammaticChange = false;
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
        setContentView(R.layout.activity_create_class_list);

        System.setProperty("java.io.tmpdir", getCacheDir().getAbsolutePath());

        initViews();
        setupClassAutocomplete();
        setupStudentAutocomplete();

        btnBack.setOnClickListener(v -> finish());
        btnThem.setOnClickListener(v -> addStudentToList());
        btnLuu.setOnClickListener(v -> saveClassList());
        btnImportExcel.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
    }

    private void initViews() {
        autoLop = findViewById(R.id.autoCompleteMaLop);
        autoHocSinh = findViewById(R.id.autoCompleteHocSinh);
        btnThem = findViewById(R.id.btnThemVaoLop);
        btnLuu = findViewById(R.id.btnLuuDanhSachLop);
        btnImportExcel = findViewById(R.id.btnImportExcel);
        rvHocSinh = findViewById(R.id.rvDanhSachHocSinhMoi);
        btnBack = findViewById(R.id.btnBack);

        rvHocSinh.setLayoutManager(new LinearLayoutManager(this));
        setupAdapter();
    }

    private void processExcelFile(Uri uri) {
        try {
            File tempFile = copyUriToInternalStorage(uri);
            try (Workbook workbook = WorkbookFactory.create(tempFile)) {
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();
                
                listHocSinhSelected.clear();
                mapErrors.clear();

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String maHS = formatter.formatCellValue(row.getCell(0)).trim();
                    String hoTen = formatter.formatCellValue(row.getCell(1)).trim();
                    if (maHS.isEmpty() || hoTen.isEmpty()) continue;

                    Student s = new Student();
                    s.setMaHocSinh(maHS);
                    s.setHoTen(hoTen);
                    s.setNgaySinh(formatter.formatCellValue(row.getCell(2)).trim());
                    
                    String gtRaw = formatter.formatCellValue(row.getCell(3)).trim();
                    if (gtRaw.equalsIgnoreCase("Nam")) s.setMaGioiTinh("GT1");
                    else if (gtRaw.equalsIgnoreCase("Nữ") || gtRaw.equalsIgnoreCase("Nu")) s.setMaGioiTinh("GT2");
                    else s.setMaGioiTinh("GT3");

                    listHocSinhSelected.add(s);
                }
            }
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Đã nạp danh sách từ Excel", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đọc file Excel", Toast.LENGTH_SHORT).show();
        }
    }

    private File copyUriToInternalStorage(Uri uri) throws Exception {
        File dest = new File(getCacheDir(), "import_temp_class.xlsx");
        try (InputStream is = getContentResolver().openInputStream(uri);
             FileOutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) os.write(buffer, 0, len);
            os.flush();
        }
        return dest;
    }

    private void setupClassAutocomplete() {
        autoLop.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 1) {
                    ApiClient.getApiService().suggestClass(s.toString()).enqueue(new Callback<List<ClassModel>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<ClassModel>> call, @NonNull Response<List<ClassModel>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                autoLop.setAdapter(new ArrayAdapter<>(CreateClassListActivity.this, android.R.layout.simple_dropdown_item_1line, response.body()));
                                autoLop.showDropDown();
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<List<ClassModel>> call, @NonNull Throwable t) {}
                    });
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        autoLop.setOnItemClickListener((parent, view, position, id) -> {
            ClassModel selected = (ClassModel) parent.getItemAtPosition(position);
            if (selected != null) {
                selectedMaLop = selected.getMaLop();
                loadExistingStudents(selectedMaLop);
            }
        });
    }

    private void setupStudentAutocomplete() {
        autoHocSinh.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isProgrammaticChange) { isProgrammaticChange = false; return; }
                if (s.length() >= 2) {
                    ApiClient.getApiService().searchStudent(s.toString()).enqueue(new Callback<List<Student>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<Student>> call, @NonNull Response<List<Student>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<Student> list = response.body();
                                List<String> displayList = new ArrayList<>();
                                for(Student st : list) displayList.add(st.getHoTen() + " (" + st.getMaHocSinh() + ")");
                                autoHocSinh.setAdapter(new ArrayAdapter<>(CreateClassListActivity.this, android.R.layout.simple_dropdown_item_1line, displayList));
                                autoHocSinh.showDropDown();
                                autoHocSinh.setOnItemClickListener((p, v, pos, i) -> {
                                    selectedStudentToAdd = list.get(pos);
                                    isProgrammaticChange = true;
                                    autoHocSinh.setText(selectedStudentToAdd.getHoTen(), false);
                                });
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
    }

    private void loadExistingStudents(String maLop) {
        ApiClient.getApiService().getStudentsByClass(maLop).enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listHocSinhSelected.clear();
                    mapErrors.clear();
                    for (Map<String, String> m : response.body()) {
                        Student s = new Student();
                        s.setMaHocSinh(m.get("MaHocSinh"));
                        s.setHoTen(m.get("HoTen"));
                        s.setNgaySinh(m.get("NgaySinh"));
                        s.setMaGioiTinh(m.get("MaGioiTinh"));
                        listHocSinhSelected.add(s);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {}
        });
    }

    private void setupAdapter() {
        adapter = new GenericAdapter<>(listHocSinhSelected, R.layout.item_class_student, (student, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvMaHS)).setText(student.getMaHocSinh());
            ((TextView) itemView.findViewById(R.id.tvTenHS)).setText(student.getHoTen());
            
            String gt = "Nam";
            if ("GT2".equals(student.getMaGioiTinh())) gt = "Nữ";
            else if ("GT3".equals(student.getMaGioiTinh())) gt = "Khác";
            ((TextView) itemView.findViewById(R.id.tvGioiTinh)).setText(gt);
            ((TextView) itemView.findViewById(R.id.tvNgaySinh)).setText(student.getNgaySinh());
            
            // LOGIC HIỂN THỊ LỖI CHỮ ĐỎ (THAY THẾ THÔNG BÁO ANDROID)
            TextView tvError = itemView.findViewById(R.id.tvError);
            String maHS = student.getMaHocSinh();
            if (maHS != null && mapErrors.containsKey(maHS)) {
                tvError.setText(mapErrors.get(maHS));
                tvError.setVisibility(View.VISIBLE);
                itemView.setBackgroundColor(android.graphics.Color.parseColor("#FFEBEE")); // Nền đỏ nhạt
            } else {
                tvError.setVisibility(View.GONE);
                itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
            
            itemView.findViewById(R.id.btnXoaHocSinh).setOnClickListener(v -> {
                if (student.getMaHocSinh() != null) mapErrors.remove(student.getMaHocSinh());
                listHocSinhSelected.remove(position);
                adapter.notifyDataSetChanged();
            });
        });
        rvHocSinh.setAdapter(adapter);
    }

    private void addStudentToList() {
        if (selectedStudentToAdd == null) return;
        for (Student s : listHocSinhSelected) {
            if (s.getMaHocSinh() != null && s.getMaHocSinh().equals(selectedStudentToAdd.getMaHocSinh())) return;
        }
        listHocSinhSelected.add(selectedStudentToAdd);
        adapter.notifyDataSetChanged();
        isProgrammaticChange = true;
        autoHocSinh.setText("");
        selectedStudentToAdd = null;
    }

    private void saveClassList() {
        if (selectedMaLop.isEmpty() || listHocSinhSelected.isEmpty()) return;

        // Reset lỗi trước khi lưu
        mapErrors.clear(); 
        adapter.notifyDataSetChanged();

        ClassModel data = new ClassModel();
        data.setMaLop(selectedMaLop);
        List<String> maHSList = new ArrayList<>();
        for (Student s : listHocSinhSelected) {
            if (s.getMaHocSinh() != null) maHSList.add(s.getMaHocSinh());
        }
        data.setDanhSachMaHS(maHSList);

        btnLuu.setEnabled(false);
        ApiClient.getApiService().saveClassList(data).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                btnLuu.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(CreateClassListActivity.this, "Lưu danh sách thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        JSONObject json = new JSONObject(errorBody);
                        if (json.has("errors")) {
                            JSONArray arr = json.getJSONArray("errors");
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                // Lưu lỗi vào Map để Adapter hiển thị dòng chữ đỏ
                                mapErrors.put(obj.getString("maHocSinh"), obj.getString("message"));
                            }
                            // Cập nhật lại UI để hiện dòng chữ đỏ, KHÔNG HIỆN THÔNG BÁO POPUP
                            adapter.notifyDataSetChanged();
                        } else {
                            // Chỉ hiện Toast cho các lỗi hệ thống khác không phải lỗi trùng lớp
                            Toast.makeText(CreateClassListActivity.this, json.optString("error", "Lưu thất bại"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("SaveError", "Parse error", e);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                btnLuu.setEnabled(true);
                Toast.makeText(CreateClassListActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
