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
import android.widget.EditText;
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
import com.example.studentmanagementapp.model.Subject;
import com.google.android.material.button.MaterialButton;
import org.apache.poi.ss.usermodel.*;
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

public class GradeEntryActivity extends AppCompatActivity {

    private AutoCompleteTextView autoLop, autoMon, autoNamHoc, autoHocKy, autoLoaiKT;
    private MaterialButton btnXem, btnLuu, btnImportExcel;
    private RecyclerView rvDiem;
    private ImageButton btnBack;

    private List<Map<String, Object>> listDiem = new ArrayList<>();
    private List<Map<String, String>> semesterList = new ArrayList<>();
    private List<ClassModel> allClassList = new ArrayList<>();
    
    private String selectedMaLop = "", selectedMaMon = "", selectedMaLoaiKT = "", selectedMaHK = "";
    private GenericAdapter<Map<String, Object>> adapter;

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    new Thread(() -> processExcelFile(uri)).start();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grade_entry);

        System.setProperty("java.io.tmpdir", getCacheDir().getAbsolutePath());

        initViews();
        setupFilters();

        btnBack.setOnClickListener(v -> finish());
        btnXem.setOnClickListener(v -> loadGradeList());
        btnLuu.setOnClickListener(v -> saveGrades());
        btnImportExcel.setOnClickListener(v -> {
            if (validateFilters()) {
                filePickerLauncher.launch("*/*");
            }
        });
    }

    private void initViews() {
        autoNamHoc = findViewById(R.id.autoCompleteNamHoc);
        autoHocKy = findViewById(R.id.autoCompleteHocKy);
        autoLop = findViewById(R.id.autoCompleteMaLopGrade);
        autoMon = findViewById(R.id.autoCompleteMonHoc);
        autoLoaiKT = findViewById(R.id.autoCompleteLoaiKT);
        btnXem = findViewById(R.id.btnXemDanhSachDiem);
        btnLuu = findViewById(R.id.btnLuuBangDiem);
        btnImportExcel = findViewById(R.id.btnImportExcel);
        rvDiem = findViewById(R.id.rvBangDiem);
        btnBack = findViewById(R.id.btnBack);

        rvDiem.setLayoutManager(new LinearLayoutManager(this));
    }

    private boolean validateFilters() {
        if (selectedMaLop.isEmpty() || selectedMaMon.isEmpty() || selectedMaLoaiKT.isEmpty() || selectedMaHK.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn đầy đủ thông tin bộ lọc trước khi Import", Toast.LENGTH_LONG).show();
            return false;
        }
        if (listDiem.isEmpty()) {
            Toast.makeText(this, "Hãy nhấn 'Lấy danh sách học sinh' trước khi Import", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void setupFilters() {
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
            if (selectedMaHK.equalsIgnoreCase(c.getMaHocKyNamHoc())) filtered.add(c);
        }
        autoLop.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filtered));
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

    private void processExcelFile(Uri uri) {
        try {
            File tempFile = copyUriToInternalStorage(uri);
            try (Workbook workbook = WorkbookFactory.create(tempFile)) {
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) throw new Exception("File không có dữ liệu tiêu đề.");

                int idxMaHS = -1, idxDiem = -1, idxGhiChu = -1;
                for (Cell cell : headerRow) {
                    String h = formatter.formatCellValue(cell).trim();
                    String lowH = h.toLowerCase();
                    
                    if (h.contains("Mã học sinh") || lowH.equals("mã học sinh") || lowH.equals("mã hs")) {
                        idxMaHS = cell.getColumnIndex();
                    } else if (h.contains("Điểm") || lowH.equals("điểm") || lowH.equals("grade")) {
                        idxDiem = cell.getColumnIndex();
                    } else if (h.contains("Ghi chú") || lowH.equals("ghi chú") || lowH.equals("note")) {
                        idxGhiChu = cell.getColumnIndex();
                    }
                }

                if (idxMaHS == -1 || idxDiem == -1) {
                    throw new Exception("Không tìm thấy cột 'Mã học sinh' hoặc 'Điểm' trong file Excel.");
                }

                int count = 0;
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String maHS = formatter.formatCellValue(row.getCell(idxMaHS)).trim();
                    String diem = formatter.formatCellValue(row.getCell(idxDiem)).trim().replace(",", ".");
                    String ghiChu = idxGhiChu != -1 ? formatter.formatCellValue(row.getCell(idxGhiChu)).trim() : "";

                    for (Map<String, Object> m : listDiem) {
                        if (maHS.equalsIgnoreCase(String.valueOf(m.get("maHocSinh")))) {
                            m.put("diem", diem);
                            m.put("ghiChu", ghiChu);
                            count++;
                            break;
                        }
                    }
                }
                int finalCount = count;
                runOnUiThread(() -> {
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    Toast.makeText(this, "Đã khớp " + finalCount + " học sinh từ Excel", Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("Lỗi Import").setMessage(e.getMessage()).show());
        }
    }

    private File copyUriToInternalStorage(Uri uri) throws Exception {
        File destinationFile = new File(getCacheDir(), "import_temp_grade.xlsx");
        try (InputStream is = getContentResolver().openInputStream(uri);
             FileOutputStream os = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) != -1) os.write(buffer, 0, length);
            os.flush();
        }
        return destinationFile;
    }

    private void setupGradeAdapter() {
        adapter = new GenericAdapter<>(listDiem, R.layout.item_grade_entry, (item, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvMaHS)).setText(String.valueOf(item.get("maHocSinh")));
            ((TextView) itemView.findViewById(R.id.tvHoTen)).setText(String.valueOf(item.get("hoTen")));
            
            EditText edtDiem = itemView.findViewById(R.id.edtDiem);
            edtDiem.setText(String.valueOf(item.get("diem")));
            
            EditText edtGhiChu = itemView.findViewById(R.id.edtGhiChu);
            edtGhiChu.setText(String.valueOf(item.get("ghiChu")));

            edtDiem.setTag(position);
            edtDiem.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (edtDiem.getTag() != null && (int)edtDiem.getTag() == position) {
                        item.put("diem", s.toString());
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
            
            edtGhiChu.setTag(position);
            edtGhiChu.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (edtGhiChu.getTag() != null && (int)edtGhiChu.getTag() == position) {
                        item.put("ghiChu", s.toString());
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        });
        rvDiem.setAdapter(adapter);
    }

    private void saveGrades() {
        if (selectedMaLop.isEmpty() || selectedMaMon.isEmpty() || selectedMaLoaiKT.isEmpty() || selectedMaHK.isEmpty()) {
            Toast.makeText(this, "Thiếu thông tin để lưu", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Map<String, Object> body = new HashMap<>();
        body.put("MaLop", selectedMaLop);
        body.put("MaMonHoc", selectedMaMon);
        body.put("MaLoaiKiemTra", selectedMaLoaiKT);
        body.put("MaHocKyNamHoc", selectedMaHK);
        
        List<Map<String, Object>> danhSach = new ArrayList<>();
        for (Map<String, Object> m : listDiem) {
            Map<String, Object> d = new HashMap<>();
            d.put("maHocSinh", m.get("maHocSinh"));
            d.put("diem", m.get("diem"));
            d.put("ghiChu", m.get("ghiChu"));
            danhSach.add(d);
        }
        body.put("DanhSachDiem", danhSach);

        btnLuu.setEnabled(false);
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
