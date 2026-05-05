package com.example.studentmanagementapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.ClassModel;
import com.example.studentmanagementapp.model.Student;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchStudentsActivity extends AppCompatActivity {

    private AutoCompleteTextView autoLop, autoMaHS, autoTenHS, autoNamHoc;
    private TextInputLayout tilNamHoc, tilMaLop, tilSearchMaHS, tilSearchTen;
    private ProgressBar pbMaHSLoading, pbTenHSLoading;
    private MaterialButton btnTimKiem;
    private RecyclerView rvKetQua;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;
    private LinearLayout layoutEmpty;

    private List<Map<String, Object>> searchResults = new ArrayList<>();
    private List<Student> studentListInClass = new ArrayList<>();
    private List<Map<String, String>> semesterList = new ArrayList<>();
    private List<ClassModel> allClassList = new ArrayList<>();
    
    private String selectedMaLop = "";
    private boolean isProgrammaticChange = false;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchMaHSRunnable, searchTenHSRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_students);

        initViews();
        setupFilters();
        setupStudentAutocomplete();
        setupErrorWatchers();

        btnBack.setOnClickListener(v -> finish());
        btnTimKiem.setOnClickListener(v -> performSearch());
    }

    private void initViews() {
        tilNamHoc = findViewById(R.id.tilNamHoc);
        tilMaLop = findViewById(R.id.tilMaLop);
        tilSearchMaHS = findViewById(R.id.tilSearchMaHS);
        tilSearchTen = findViewById(R.id.tilSearchTen);

        pbMaHSLoading = findViewById(R.id.pbMaHSLoading);
        pbTenHSLoading = findViewById(R.id.pbTenHSLoading);

        // Khóa hai ô tìm kiếm học sinh ban đầu
        tilSearchMaHS.setEnabled(false);
        tilSearchTen.setEnabled(false);

        autoNamHoc = findViewById(R.id.autoCompleteNamHoc);
        autoLop = findViewById(R.id.autoCompleteMaLop);
        
        autoMaHS = findViewById(R.id.edtSearchMaHS);
        autoTenHS = findViewById(R.id.edtSearchTen);
        
        btnTimKiem = findViewById(R.id.btnTimKiem);
        rvKetQua = findViewById(R.id.rvKetQuaTraCuu);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        rvKetQua.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupFilters() {
        // 1. Tải danh sách Học kỳ/Năm học
        ApiClient.getApiService().getSemesterList().enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    semesterList = response.body();
                    loadNamHoc(semesterList);
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {}
        });

        // 2. Tải danh sách lớp
        if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
        ApiClient.getApiService().getClassList().enqueue(new Callback<List<ClassModel>>() {
            @Override
            public void onResponse(@NonNull Call<List<ClassModel>> call, @NonNull Response<List<ClassModel>> response) {
                if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allClassList = response.body();

                    // Nếu người dùng đã chọn Năm học trước khi lớp tải xong, lọc ngay
                    if (!autoNamHoc.getText().toString().isEmpty()) {
                        filterClasses();
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<List<ClassModel>> call, @NonNull Throwable t) {
                if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
            }
        });

        autoLop.setOnItemClickListener((p, v, pos, id) -> {
            ClassModel sel = (ClassModel) p.getItemAtPosition(pos);
            selectedMaLop = sel.getMaLop();

            tilMaLop.setError(null);
            tilMaLop.setErrorEnabled(false);

            // Mở khóa các ô tìm kiếm khi đã chọn lớp
            tilSearchMaHS.setEnabled(true);
            tilSearchTen.setEnabled(true);

            hideKeyboard();
            loadStudentsInClass(selectedMaLop);
        });
    }

    private void loadNamHoc(List<Map<String, String>> fullList) {
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH);
        int effectiveSchoolStartYear = (currentMonth >= Calendar.SEPTEMBER) ? currentYear : currentYear - 1;

        List<String> distinctYears = new ArrayList<>();

        for (Map<String, String> m : fullList) {
            String y = m.get("namhoc");
            if (y == null || distinctYears.contains(y)) continue;
            try {
                String startYearStr = y.contains("-") ? y.split("-")[0].trim() : y.trim();
                int startYear = Integer.parseInt(startYearStr);
                if (startYear >= effectiveSchoolStartYear) {
                    distinctYears.add(y);
                }
            } catch (Exception ignored) {}
        }

        autoNamHoc.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, distinctYears));
        autoNamHoc.setOnItemClickListener((parent, view, position, id) -> {
            autoLop.setText("");
            selectedMaLop = "";

            tilNamHoc.setError(null);
            tilNamHoc.setErrorEnabled(false);
            tilMaLop.setError(null);
            tilMaLop.setErrorEnabled(false);

            // Khóa lại các ô tìm kiếm khi đổi năm học (vì chưa chọn lớp mới)
            tilSearchMaHS.setEnabled(false);
            tilSearchTen.setEnabled(false);

            filterClasses();
        });
    }

    private void filterClasses() {
        String year = autoNamHoc.getText().toString().trim();
        if (year.isEmpty()) return;

        List<ClassModel> filtered = new ArrayList<>();
        List<String> addedClassNames = new ArrayList<>();

        for (ClassModel c : allClassList) {
            String classYear = c.getNamHoc();

            // Đối chiếu năm học dựa trên mã nếu cần
            if (classYear == null || classYear.equals("Chưa có")) {
                for (Map<String, String> s : semesterList) {
                    if (s.get("ma") != null && s.get("ma").equalsIgnoreCase(c.getMaHocKyNamHoc())) {
                        classYear = s.get("namhoc");
                        break;
                    }
                }
            }

            if (classYear != null && year.equalsIgnoreCase(classYear.trim())) {
                String tenLop = c.getTenLop();
                if (!addedClassNames.contains(tenLop)) {
                    filtered.add(c);
                    addedClassNames.add(tenLop);
                }
            }
        }

        ArrayAdapter<ClassModel> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, filtered);
        autoLop.setAdapter(adapter);
        autoLop.setThreshold(0);

        if (!filtered.isEmpty()) {
            tilMaLop.setEnabled(true);
        } else {
            tilMaLop.setEnabled(false);
            if (!allClassList.isEmpty()) {
                Toast.makeText(this, "Năm học " + year + " chưa có lớp nào.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupErrorWatchers() {
        autoMaHS.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isProgrammaticChange) return; // Chặn hoàn toàn logic khi code tự điền
                
                tilSearchMaHS.setError(null);
                tilSearchMaHS.setErrorEnabled(false);

                searchHandler.removeCallbacksAndMessages(null); // Hủy sạch các Runnable đang chờ
                
                if (s.length() > 0) {
                    pbMaHSLoading.setVisibility(View.VISIBLE);
                    searchMaHSRunnable = () -> {
                        autoMaHS.showDropDown(); 
                        pbMaHSLoading.setVisibility(View.GONE);
                    };
                    searchHandler.postDelayed(searchMaHSRunnable, 300);
                } else {
                    pbMaHSLoading.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        autoTenHS.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isProgrammaticChange) return; // Chặn hoàn toàn logic khi code tự điền
                
                tilSearchTen.setError(null);
                tilSearchTen.setErrorEnabled(false);

                searchHandler.removeCallbacksAndMessages(null); // Hủy sạch các Runnable đang chờ
                
                if (s.length() > 0) {
                    pbTenHSLoading.setVisibility(View.VISIBLE);
                    searchTenHSRunnable = () -> {
                        autoTenHS.showDropDown();
                        pbTenHSLoading.setVisibility(View.GONE);
                    };
                    searchHandler.postDelayed(searchTenHSRunnable, 300);
                } else {
                    pbTenHSLoading.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showLoading() {
        progressIndicator.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        progressIndicator.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void loadStudentsInClass(String maLop) {
        if (maLop == null || maLop.isEmpty()) return;

        showLoading();
        ApiClient.getApiService().getStudentsByClass(maLop).enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    studentListInClass.clear();
                    List<Map<String, String>> data = response.body();
                    for (Map<String, String> m : data) {
                        Student s = new Student();
                        String ma = m.get("MaHocSinh") != null ? m.get("MaHocSinh") : m.get("maHocSinh");
                        String ten = m.get("HoTen") != null ? m.get("HoTen") : m.get("hoTen");
                        if (ma != null && ten != null) {
                            s.setMaHocSinh(ma);
                            s.setHoTen(ten);
                            studentListInClass.add(s);
                        }
                    }
                    updateStudentAdapters();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {
                hideLoading();
            }
        });
    }

    private void updateStudentAdapters() {
        if (studentListInClass.isEmpty()) {
            autoMaHS.setAdapter(null);
            autoTenHS.setAdapter(null);
            return;
        }

        // Adapter cho ô Mã Học Sinh - Lọc theo Mã
        ArrayAdapter<Student> adapterMa = new ArrayAdapter<Student>(this, R.layout.item_dropdown_2line, new ArrayList<>(studentListInClass)) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) convertView = getLayoutInflater().inflate(R.layout.item_dropdown_2line, parent, false);
                Student s = getItem(position);
                if (s != null) {
                    ((TextView) convertView.findViewById(R.id.text1)).setText(s.getMaHocSinh());
                    ((TextView) convertView.findViewById(R.id.text2)).setText(s.getHoTen());
                }
                return convertView;
            }

            @NonNull
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        List<Student> suggestions = new ArrayList<>();
                        if (constraint != null) {
                            String filterPattern = constraint.toString().toLowerCase().trim();
                            for (Student s : studentListInClass) {
                                if (s.getMaHocSinh().toLowerCase().contains(filterPattern)) suggestions.add(s);
                            }
                        }
                        results.values = suggestions;
                        results.count = suggestions.size();
                        return results;
                    }
                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        clear();
                        if (results != null && results.count > 0) addAll((List<Student>) results.values);
                        notifyDataSetChanged();
                    }
                    @Override
                    public CharSequence convertResultToString(Object resultValue) {
                        return ((Student) resultValue).getMaHocSinh();
                    }
                };
            }
        };

        // Adapter cho ô Tên Học Sinh - Lọc theo Tên
        ArrayAdapter<Student> adapterTen = new ArrayAdapter<Student>(this, R.layout.item_dropdown_2line, new ArrayList<>(studentListInClass)) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) convertView = getLayoutInflater().inflate(R.layout.item_dropdown_2line, parent, false);
                Student s = getItem(position);
                if (s != null) {
                    ((TextView) convertView.findViewById(R.id.text1)).setText(s.getHoTen());
                    ((TextView) convertView.findViewById(R.id.text2)).setText(s.getMaHocSinh());
                }
                return convertView;
            }

            @NonNull
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        List<Student> suggestions = new ArrayList<>();
                        if (constraint != null) {
                            String filterPattern = constraint.toString().toLowerCase().trim();
                            for (Student s : studentListInClass) {
                                if (s.getHoTen().toLowerCase().contains(filterPattern)) suggestions.add(s);
                            }
                        }
                        results.values = suggestions;
                        results.count = suggestions.size();
                        return results;
                    }
                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        clear();
                        if (results != null && results.count > 0) addAll((List<Student>) results.values);
                        notifyDataSetChanged();
                    }
                    @Override
                    public CharSequence convertResultToString(Object resultValue) {
                        return ((Student) resultValue).getHoTen();
                    }
                };
            }
        };

        autoMaHS.setAdapter(adapterMa);
        autoTenHS.setAdapter(adapterTen);
        autoMaHS.setThreshold(1);
        autoTenHS.setThreshold(1);
    }

    private void setupStudentAutocomplete() {
        autoMaHS.setOnItemClickListener((parent, view, position, id) -> {
            searchHandler.removeCallbacksAndMessages(null); // Hủy mọi lệnh hiện dropdown đang chờ
            pbMaHSLoading.setVisibility(View.GONE); // Ẩn Spinner trực tiếp
            pbTenHSLoading.setVisibility(View.GONE);

            Student selected = (Student) parent.getItemAtPosition(position);
            if (selected != null) {
                isProgrammaticChange = true;
                autoMaHS.setText(selected.getMaHocSinh(), false); // false để không kích hoạt filter
                autoMaHS.dismissDropDown();
                
                autoTenHS.setText(selected.getHoTen(), false); // false để không kích hoạt filter
                autoTenHS.dismissDropDown();
                
                autoMaHS.clearFocus();
                hideKeyboard();
                isProgrammaticChange = false;
            }
        });

        autoTenHS.setOnItemClickListener((parent, view, position, id) -> {
            searchHandler.removeCallbacksAndMessages(null); // Hủy mọi lệnh hiện dropdown đang chờ
            pbMaHSLoading.setVisibility(View.GONE); // Ẩn Spinner trực tiếp
            pbTenHSLoading.setVisibility(View.GONE);

            Student selected = (Student) parent.getItemAtPosition(position);
            if (selected != null) {
                isProgrammaticChange = true;
                autoMaHS.setText(selected.getMaHocSinh(), false); // false để không kích hoạt filter
                autoMaHS.dismissDropDown();
                
                autoTenHS.setText(selected.getHoTen(), false); // false để không kích hoạt filter
                autoTenHS.dismissDropDown();

                autoTenHS.clearFocus();
                hideKeyboard();
                isProgrammaticChange = false;
            }
        });
    }

    private void performSearch() {
        String year = autoNamHoc.getText().toString().trim();
        if (year.isEmpty()) {
            tilNamHoc.setErrorEnabled(true);
            tilNamHoc.setError("Vui lòng chọn năm học");
            return;
        }

        if (selectedMaLop == null || selectedMaLop.isEmpty()) {
            tilMaLop.setErrorEnabled(true);
            tilMaLop.setError("Vui lòng chọn lớp");
            return;
        }

        String maHS = autoMaHS.getText().toString().trim();
        String tenHS = autoTenHS.getText().toString().trim();

        hideKeyboard();
        showLoading();
        btnTimKiem.setEnabled(false);
        ApiClient.getApiService().getSearchResult(selectedMaLop, tenHS, maHS).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                hideLoading();
                btnTimKiem.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    searchResults = response.body();
                    if (searchResults.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvKetQua.setVisibility(View.GONE);
                    } else {
                        Collator collator = Collator.getInstance(new Locale("vi", "VN"));
                        Collections.sort(searchResults, (map1, map2) -> {
                            String fullName1 = getStringValue(findValue(map1, "HoTen", "hoTen", "HOTEN"));
                            String fullName2 = getStringValue(findValue(map2, "HoTen", "hoTen", "HOTEN"));
                            
                            String lastName1 = getLastName(fullName1);
                            String lastName2 = getLastName(fullName2);
                            
                            int res = collator.compare(lastName1, lastName2);
                            if (res == 0) {
                                return collator.compare(fullName1, fullName2);
                            }
                            return res;
                        });

                        layoutEmpty.setVisibility(View.GONE);
                        rvKetQua.setVisibility(View.VISIBLE);
                        setupAdapter();
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                hideLoading();
                btnTimKiem.setEnabled(true);
                Toast.makeText(SearchStudentsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getLastName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty() || fullName.equals("N/A")) return "";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return "";
    }

    private void setupAdapter() {
        GenericAdapter<Map<String, Object>> adapter = new GenericAdapter<>(searchResults, R.layout.item_search_students, (item, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            String maHS = getStringValue(findValue(item, "MaHocSinh", "maHocSinh", "MAHOCSINH"));
            String hoTen = getStringValue(findValue(item, "HoTen", "hoTen", "HOTEN"));
            ((TextView) itemView.findViewById(R.id.tvMaHS)).setText(maHS);
            ((TextView) itemView.findViewById(R.id.tvTenHS)).setText(hoTen);

            String lop = getStringValue(findValue(item, "TenLop", "lop"));
            String namHoc = getStringValue(findValue(item, "NamHoc", "namHoc", "NamHocBatDau"));
            ((TextView) itemView.findViewById(R.id.tvLopNamHoc)).setText(String.format("Lớp: %s  |  Năm học: %s", lop, namHoc));

            itemView.setOnClickListener(v -> showChoiceDialog(item));
        });
        rvKetQua.setAdapter(adapter);
    }

    private void showChoiceDialog(Map<String, Object> item) {
        String hoTen = getStringValue(findValue(item, "HoTen", "hoTen", "HOTEN"));
        String[] options = {"Tra cứu thông tin", "Tra cứu điểm"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Lựa chọn cho " + hoTen)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(this, StudentInfoActivity.class);
                        intent.putExtra("student_data", (Serializable) new HashMap<>(item));
                        startActivity(intent);
                    } else if (which == 1) {
                        Intent intent = new Intent(this, StudentScoreActivity.class);
                        intent.putExtra("student_data", (Serializable) new HashMap<>(item));
                        startActivity(intent);
                    }
                })
                .show();
    }

    private Object findValue(Map<String, Object> map, String... keys) {
        if (map == null) return null;
        for (String key : keys) {
            if (map.containsKey(key)) return map.get(key);
            for (String actualKey : map.keySet()) {
                if (actualKey.equalsIgnoreCase(key)) return map.get(actualKey);
            }
        }
        return null;
    }

    private String formatScore(Object obj) {
        if (obj == null || obj.toString().isEmpty() || obj.toString().equalsIgnoreCase("null")) return "-";
        try {
            double score = Double.parseDouble(obj.toString());
            if (score == (long) score) return String.format(Locale.getDefault(), "%d", (long) score);
            return String.format(Locale.getDefault(), "%.1f", score);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String getStringValue(Object obj) {
        if (obj == null || obj.toString().isEmpty() || obj.toString().equalsIgnoreCase("null")) return "N/A";
        return obj.toString();
    }
}
