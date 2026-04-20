package com.example.studentmanagementapp;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    
    private final Map<String, String> mapErrors = new HashMap<>();
    
    private boolean isProgrammaticChange = false;
    private Uri selectedFileUri;

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    new Thread(() -> processExcelFile(uri)).start();
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
            List<Student> studentsFromExcel = new ArrayList<>();
            
            try (Workbook workbook = WorkbookFactory.create(tempFile)) {
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) throw new Exception("File Excel không có dữ liệu tiêu đề.");

                int idxMaHS = -1, idxHoTen = -1, idxGioiTinh = -1, idxNgaySinh = -1;

                for (Cell cell : headerRow) {
                    String title = formatter.formatCellValue(cell).trim().toLowerCase();
                    if (title.contains("mã học sinh") || title.contains("mahs") || title.equals("mã hs")) idxMaHS = cell.getColumnIndex();
                    else if (title.contains("họ và tên") || title.contains("hoten") || title.equals("họ tên")) idxHoTen = cell.getColumnIndex();
                    else if (title.contains("giới tính") || title.contains("gioitinh") || title.contains("gender")) idxGioiTinh = cell.getColumnIndex();
                    else if (title.contains("ngày sinh") || title.contains("ngaysinh") || title.contains("birthday")) idxNgaySinh = cell.getColumnIndex();
                }

                if (idxMaHS == -1 || idxHoTen == -1) {
                    throw new Exception("Không tìm thấy các cột bắt buộc trong file (Mã HS, Họ tên). Vui lòng kiểm tra lại tiêu đề cột.");
                }

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String maHS = formatter.formatCellValue(row.getCell(idxMaHS)).trim();
                    String hoTen = formatter.formatCellValue(row.getCell(idxHoTen)).trim();
                    if (maHS.isEmpty() || hoTen.isEmpty()) continue;

                    Student student = new Student();
                    student.setMaHocSinh(maHS);
                    student.setHoTen(hoTen);
                    
                    if (idxNgaySinh != -1) {
                        Cell dateCell = row.getCell(idxNgaySinh);
                        if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            student.setNgaySinh(sdf.format(dateCell.getDateCellValue()));
                        } else {
                            String rawDate = formatter.formatCellValue(dateCell).trim();
                            student.setNgaySinh(formatDateString(rawDate));
                        }
                    }
                    
                    if (idxGioiTinh != -1) {
                        String genderText = formatter.formatCellValue(row.getCell(idxGioiTinh)).trim();
                        if (genderText.equalsIgnoreCase("Nam") || genderText.equalsIgnoreCase("GT1")) student.setMaGioiTinh("GT1");
                        else if (genderText.equalsIgnoreCase("Nữ") || genderText.equalsIgnoreCase("Nu") || genderText.equalsIgnoreCase("GT2")) student.setMaGioiTinh("GT2");
                        else student.setMaGioiTinh("GT3");
                    } else {
                        student.setMaGioiTinh("GT1");
                    }
                    
                    studentsFromExcel.add(student);
                }
            }
            
            if (!studentsFromExcel.isEmpty()) {
                runOnUiThread(() -> {
                    Map<String, Student> uniqueMap = new LinkedHashMap<>();
                    for (Student s : listHocSinhSelected) {
                        if (s.getMaHocSinh() != null) uniqueMap.put(s.getMaHocSinh(), s);
                    }
                    for (Student s : studentsFromExcel) {
                        if (s.getMaHocSinh() != null) uniqueMap.put(s.getMaHocSinh(), s);
                    }
                    
                    listHocSinhSelected.clear();
                    listHocSinhSelected.addAll(uniqueMap.values());
                    adapter.notifyDataSetChanged();
                    Toast.makeText(CreateClassListActivity.this, "Đã import " + studentsFromExcel.size() + " học sinh", Toast.LENGTH_SHORT).show();
                });
            }

        } catch (Exception e) {
            Log.e("ExcelError", "Lỗi xử lý file: ", e);
            runOnUiThread(() -> new AlertDialog.Builder(CreateClassListActivity.this)
                    .setTitle("Lỗi đọc file")
                    .setMessage(e.getMessage())
                    .setPositiveButton("OK", null)
                    .show());
        }
    }

    private String formatDateString(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "";
        try {
            // Thử parse các định dạng phổ biến
            String[] formats = {"dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "MM/dd/yyyy"};
            for (String f : formats) {
                try {
                    SimpleDateFormat inputSdf = new SimpleDateFormat(f, Locale.getDefault());
                    Date date = inputSdf.parse(rawDate);
                    if (date != null) {
                        SimpleDateFormat outputSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        return outputSdf.format(date);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            Log.e("DateFormat", "Error parsing date: " + rawDate);
        }
        return rawDate; // Trả về nguyên bản nếu không parse được
    }

    private File copyUriToInternalStorage(Uri uri) throws Exception {
        File destinationFile = new File(getCacheDir(), "import_temp_class.xlsx");
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
                lastException = e;
                retryCount++;
                Thread.sleep(500);
            }
        }
        throw new Exception("Lỗi truy cập tệp: " + (lastException != null ? lastException.getLocalizedMessage() : "Không rõ"));
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
                        String rawDate = m.get("NgaySinh");
                        s.setNgaySinh(formatDateString(rawDate));
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
            
            TextView tvError = itemView.findViewById(R.id.tvError);
            String maHS = student.getMaHocSinh();
            if (maHS != null && mapErrors.containsKey(maHS)) {
                tvError.setText(mapErrors.get(maHS));
                tvError.setVisibility(View.VISIBLE);
                itemView.setBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"));
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
                                mapErrors.put(obj.getString("maHocSinh"), obj.getString("message"));
                            }
                            adapter.notifyDataSetChanged();
                        } else {
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
