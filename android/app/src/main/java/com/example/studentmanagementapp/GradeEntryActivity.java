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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.ClassModel;
import com.example.studentmanagementapp.model.Subject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GradeEntryActivity extends AppCompatActivity {

    private AutoCompleteTextView autoLop, autoMon, autoNamHoc, autoHocKy, autoLoaiKT;
    private TextInputLayout tilNamHoc, tilHocKy, tilLop, tilMon, tilLoaiKT;
    private MaterialButton btnLuu;
    private ImageButton btnImportExcel;
    private RecyclerView rvDiem;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;

    private List<Map<String, Object>> listDiem = new ArrayList<>();
    private List<Map<String, String>> semesterList = new ArrayList<>();
    private List<ClassModel> allClassList = new ArrayList<>();
    
    private String selectedMaLop = "", selectedMaMon = "", selectedMaLoaiKT = "", selectedMaHK = "";
    private GenericAdapter<Map<String, Object>> adapter;

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    showLoading();
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
        btnLuu.setOnClickListener(v -> saveGrades());
        
        btnImportExcel.setOnClickListener(v -> {
            if (validateFilters()) {
                if (listDiem.isEmpty()) {
                    showErrorDialog("Thông báo", "Hãy đảm bảo danh sách học sinh đã được tải trước khi Import");
                    return;
                }
                filePickerLauncher.launch("*/*");
            }
        });
    }

    private void initViews() {
        tilNamHoc = findViewById(R.id.tilNamHoc);
        tilHocKy = findViewById(R.id.tilHocKy);
        tilLop = findViewById(R.id.tilLop);
        tilMon = findViewById(R.id.tilMon);
        tilLoaiKT = findViewById(R.id.tilLoaiKT);

        autoNamHoc = findViewById(R.id.autoCompleteNamHoc);
        autoHocKy = findViewById(R.id.autoCompleteHocKy);
        autoLop = findViewById(R.id.autoCompleteMaLopGrade);
        autoMon = findViewById(R.id.autoCompleteMonHoc);
        autoLoaiKT = findViewById(R.id.autoCompleteLoaiKT);
        
        btnLuu = findViewById(R.id.btnLuuBangDiem);
        btnImportExcel = findViewById(R.id.btnImportExcel);

        rvDiem = findViewById(R.id.rvBangDiem);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);

        rvDiem.setLayoutManager(new LinearLayoutManager(this));
    }

    private void showLoading() {
        runOnUiThread(() -> {
            if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> {
            if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
        });
    }

    private void showErrorDialog(String title, String message) {
        runOnUiThread(() -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    private void showRetrySnackbar(String message, Runnable retryAction) {
        runOnUiThread(() -> {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                    .setAction("Thử lại", v -> retryAction.run())
                    .show();
        });
    }

    private void checkAndAutoLoad() {
        if (!autoNamHoc.getText().toString().isEmpty() &&
            !selectedMaHK.isEmpty() &&
            !selectedMaLop.isEmpty() &&
            !selectedMaMon.isEmpty() &&
            !selectedMaLoaiKT.isEmpty()) {
            loadGradeList();
        }
    }

    private boolean validateFilters() {
        boolean isValid = true;
        if (autoNamHoc.getText().toString().isEmpty()) {
            tilNamHoc.setErrorEnabled(true);
            tilNamHoc.setError("Vui lòng chọn năm học");
            isValid = false;
        }
        if (selectedMaHK.isEmpty()) {
            tilHocKy.setErrorEnabled(true);
            tilHocKy.setError("Vui lòng chọn học kỳ");
            isValid = false;
        }
        if (selectedMaLop.isEmpty()) {
            tilLop.setErrorEnabled(true);
            tilLop.setError("Vui lòng chọn lớp");
            isValid = false;
        }
        if (selectedMaMon.isEmpty()) {
            tilMon.setErrorEnabled(true);
            tilMon.setError("Vui lòng chọn môn học");
            isValid = false;
        }
        if (selectedMaLoaiKT.isEmpty()) {
            tilLoaiKT.setErrorEnabled(true);
            tilLoaiKT.setError("Vui lòng chọn loại kiểm tra");
            isValid = false;
        }
        return isValid;
    }

    private void setupFilters() {
        loadSemesters();
        loadClasses();
        loadSubjects();
        loadTestTypes();
    }

    private void loadSemesters() {
        ApiClient.getApiService().getSemesterList().enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    semesterList = response.body();
                    setupNamHocAdapter();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {
                showRetrySnackbar("Lỗi tải danh sách niên khóa", () -> loadSemesters());
            }
        });
    }

    private void setupNamHocAdapter() {
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH);
        int thresholdYear = (currentMonth >= Calendar.SEPTEMBER) ? currentYear : currentYear - 1;

        List<String> years = new ArrayList<>();
        for (Map<String, String> m : semesterList) {
            String y = m.get("namhoc");
            if (y == null || years.contains(y)) continue;
            try {
                String startYearStr = y.contains("-") ? y.split("-")[0].trim() : y.trim();
                int startYear = Integer.parseInt(startYearStr);
                if (startYear >= thresholdYear) {
                    years.add(y);
                }
            } catch (Exception ignored) {}
        }
        Collections.sort(years, Collections.reverseOrder());
        autoNamHoc.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));
        autoNamHoc.setOnItemClickListener((parent, view, position, id) -> {
            tilNamHoc.setError(null);
            tilNamHoc.setErrorEnabled(false);
            autoHocKy.setText("");
            autoLop.setText("");
            selectedMaHK = "";
            selectedMaLop = "";
            tilHocKy.setEnabled(true);
            tilLop.setEnabled(false);
            listDiem.clear();
            if (adapter != null) adapter.notifyDataSetChanged();
            String year = (String) parent.getItemAtPosition(position);
            List<String> terms = new ArrayList<>();
            for (Map<String, String> m : semesterList) {
                if (year.equals(m.get("namhoc"))) terms.add(m.get("hocky"));
            }
            autoHocKy.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, terms));
            checkAndAutoLoad();
        });

        autoHocKy.setOnItemClickListener((parent, view, position, id) -> {
            tilHocKy.setError(null);
            tilHocKy.setErrorEnabled(false);
            autoLop.setText("");
            selectedMaLop = "";
            tilLop.setEnabled(true);
            listDiem.clear();
            if (adapter != null) adapter.notifyDataSetChanged();
            String year = autoNamHoc.getText().toString();
            String term = (String) parent.getItemAtPosition(position);
            for (Map<String, String> m : semesterList) {
                if (year.equals(m.get("namhoc")) && term.equals(m.get("hocky"))) {
                    selectedMaHK = m.get("ma");
                    break;
                }
            }
            filterClasses();
            checkAndAutoLoad();
        });
    }

    private void loadClasses() {
        ApiClient.getApiService().getClassList().enqueue(new Callback<List<ClassModel>>() {
            @Override
            public void onResponse(@NonNull Call<List<ClassModel>> call, @NonNull Response<List<ClassModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allClassList = response.body();
                    if (!autoNamHoc.getText().toString().isEmpty()) filterClasses();
                }
            }
            @Override public void onFailure(@NonNull Call<List<ClassModel>> call, @NonNull Throwable t) {
                showRetrySnackbar("Lỗi tải danh sách lớp", () -> loadClasses());
            }
        });

        autoLop.setOnItemClickListener((p, v, pos, id) -> {
            tilLop.setError(null);
            tilLop.setErrorEnabled(false);
            ClassModel sel = (ClassModel) p.getItemAtPosition(pos);
            selectedMaLop = sel.getMaLop();
            checkAndAutoLoad();
        });
    }

    private void loadSubjects() {
        ApiClient.getApiService().getSubjectList().enqueue(new Callback<List<Subject>>() {
            @Override
            public void onResponse(@NonNull Call<List<Subject>> call, @NonNull Response<List<Subject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Subject> subjs = response.body();
                    List<String> names = new ArrayList<>();
                    for(Subject s : subjs) names.add(s.getTenMonHoc());
                    autoMon.setAdapter(new ArrayAdapter<>(GradeEntryActivity.this, android.R.layout.simple_list_item_1, names));
                    autoMon.setOnItemClickListener((p, v, pos, id) -> {
                        tilMon.setError(null);
                        tilMon.setErrorEnabled(false);
                        selectedMaMon = subjs.get(pos).getMaMonHoc();
                        checkAndAutoLoad();
                    });
                }
            }
            @Override public void onFailure(@NonNull Call<List<Subject>> call, @NonNull Throwable t) {
                showRetrySnackbar("Lỗi tải danh sách môn học", () -> loadSubjects());
            }
        });
    }

    private void loadTestTypes() {
        ApiClient.getApiService().getTestTypeList().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> types = response.body();
                    List<String> names = new ArrayList<>();
                    for(Map<String, Object> m : types) names.add(String.valueOf(m.get("TenLoaiKiemTra")));
                    autoLoaiKT.setAdapter(new ArrayAdapter<>(GradeEntryActivity.this, android.R.layout.simple_list_item_1, names));
                    autoLoaiKT.setOnItemClickListener((p, v, pos, id) -> {
                        tilLoaiKT.setError(null);
                        tilLoaiKT.setErrorEnabled(false);
                        selectedMaLoaiKT = String.valueOf(types.get(pos).get("MaLoaiKiemTra"));
                        checkAndAutoLoad();
                    });
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                showRetrySnackbar("Lỗi tải các loại kiểm tra", () -> loadTestTypes());
            }
        });
    }

    private void filterClasses() {
        if (selectedMaHK.isEmpty()) return;
        List<ClassModel> filtered = new ArrayList<>();
        for (ClassModel c : allClassList) {
            if (selectedMaHK.equalsIgnoreCase(c.getMaHocKyNamHoc())) filtered.add(c);
        }
        autoLop.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filtered));
        
        if (filtered.isEmpty() && !allClassList.isEmpty()) {
            showErrorDialog("Thông báo", "Năm học hiện tại chưa có lớp nào được tạo.");
        }
    }

    private void loadGradeList() {
        showLoading();
        ApiClient.getApiService().getHocSinhNhapDiem(selectedMaLop, selectedMaMon, selectedMaLoaiKT, selectedMaHK).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    listDiem = response.body();
                    setupGradeAdapter();
                } else {
                    listDiem.clear();
                    if (adapter != null) adapter.notifyDataSetChanged();
                    showErrorDialog("Thông báo", "Học sinh trong lớp này đã được nhập điểm hoặc lớp trống.");
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                hideLoading();
                showRetrySnackbar("Lỗi tải danh sách học sinh", () -> loadGradeList());
            }
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
                    String h = formatter.formatCellValue(cell).trim().toLowerCase();
                    if (h.contains("mã học sinh") || h.equals("mahs") || h.equals("mã hs")) idxMaHS = cell.getColumnIndex();
                    else if (h.contains("điểm") || h.equals("grade")) idxDiem = cell.getColumnIndex();
                    else if (h.contains("ghi chú") || h.equals("note")) idxGhiChu = cell.getColumnIndex();
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
                    hideLoading();
                    if (adapter != null) adapter.notifyDataSetChanged();
                    showErrorDialog("Import thành công", "Đã import thành công " + finalCount + " học sinh từ file Excel. Vui lòng kiểm tra lại trước khi nhấn Lưu.");
                });
            }
        } catch (Exception e) {
            runOnUiThread(() -> {
                hideLoading();
                showErrorDialog("Import thất bại", e.getMessage());
            });
        }
    }

    private File copyUriToInternalStorage(Uri uri) throws Exception {
        File destinationFile = new File(getCacheDir(), "import_temp_grade.xlsx");
        try (InputStream is = getContentResolver().openInputStream(uri);
             FileOutputStream os = new FileOutputStream(destinationFile)) {
            if (is == null) throw new Exception("Không thể mở tệp.");
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
            EditText edtGhiChu = itemView.findViewById(R.id.edtGhiChu);

            if (edtDiem.getTag() instanceof TextWatcher) {
                edtDiem.removeTextChangedListener((TextWatcher) edtDiem.getTag());
            }
            if (edtGhiChu.getTag() instanceof TextWatcher) {
                edtGhiChu.removeTextChangedListener((TextWatcher) edtGhiChu.getTag());
            }

            edtDiem.setText(String.valueOf(item.get("diem") != null ? item.get("diem") : ""));
            edtGhiChu.setText(String.valueOf(item.get("ghiChu") != null ? item.get("ghiChu") : ""));

            TextWatcher diemWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    item.put("diem", s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            };

            TextWatcher ghiChuWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    item.put("ghiChu", s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            };

            edtDiem.addTextChangedListener(diemWatcher);
            edtDiem.setTag(diemWatcher);

            edtGhiChu.addTextChangedListener(ghiChuWatcher);
            edtGhiChu.setTag(ghiChuWatcher);
        });
        rvDiem.setAdapter(adapter);
    }

    private void saveGrades() {
        if (!validateFilters()) return;
        
        showLoading();
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
                hideLoading();
                btnLuu.setEnabled(true);
                if (response.isSuccessful()) {
                    new MaterialAlertDialogBuilder(GradeEntryActivity.this)
                            .setTitle("Thành công")
                            .setMessage("Đã lưu bảng điểm thành công vào hệ thống.")
                            .setCancelable(false)
                            .setNegativeButton("Đóng", (dialog, which) -> finish())
                            .setPositiveButton("Nhập tiếp", (dialog, which) -> {
                                autoMon.setText("");
                                autoLoaiKT.setText("");
                                selectedMaMon = "";
                                selectedMaLoaiKT = "";
                                listDiem.clear();
                                if (adapter != null) adapter.notifyDataSetChanged();
                                tilMon.setError(null);
                                tilMon.setErrorEnabled(false);
                                tilLoaiKT.setError(null);
                                tilLoaiKT.setErrorEnabled(false);
                                dialog.dismiss();
                            })
                            .show();
                } else {
                    showErrorDialog("Thất bại", "Không thể lưu điểm. Mã lỗi: " + response.code());
                }
            }
            @Override public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                btnLuu.setEnabled(true);
                showRetrySnackbar("Lỗi kết nối máy chủ", () -> saveGrades());
            }
        });
    }
}
