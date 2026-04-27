package com.example.studentmanagementapp;

import android.content.Context;
import android.os.Bundle;
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
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchStudentsActivity extends AppCompatActivity {

    private AutoCompleteTextView autoLop, autoMaHS, autoTenHS, autoNamHoc;
    private TextInputLayout tilNamHoc, tilMaLop;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_students);

        initViews();
        setupFilters();
        setupStudentAutocomplete();

        btnBack.setOnClickListener(v -> finish());
        btnTimKiem.setOnClickListener(v -> performSearch());
    }

    private void initViews() {
        tilNamHoc = findViewById(R.id.tilNamHoc);
        tilMaLop = findViewById(R.id.tilMaLop);

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
        distinctYears.add("Tất cả năm học");

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
            tilMaLop.setEnabled(true);
            autoLop.setText("");
            selectedMaLop = "";
            filterClasses();
        });
    }

    private void filterClasses() {
        String year = autoNamHoc.getText().toString();
        if (year.isEmpty()) return;

        List<ClassModel> filtered = new ArrayList<>();
        if (year.equals("Tất cả năm học")) {
            filtered.addAll(allClassList);
        } else {
            for (ClassModel c : allClassList) {
                if (year.equalsIgnoreCase(c.getNamHoc())) {
                    filtered.add(c);
                }
            }
        }
        autoLop.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filtered));
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
            Student selected = (Student) parent.getItemAtPosition(position);
            if (selected != null) {
                isProgrammaticChange = true;
                autoMaHS.setText(selected.getMaHocSinh(), false);
                autoMaHS.dismissDropDown();
                isProgrammaticChange = true;
                autoTenHS.setText(selected.getHoTen(), false);
                autoTenHS.dismissDropDown();
                autoMaHS.clearFocus();
                hideKeyboard();
            }
        });

        autoTenHS.setOnItemClickListener((parent, view, position, id) -> {
            Student selected = (Student) parent.getItemAtPosition(position);
            if (selected != null) {
                isProgrammaticChange = true;
                autoMaHS.setText(selected.getMaHocSinh(), false);
                autoMaHS.dismissDropDown();
                isProgrammaticChange = true;
                autoTenHS.setText(selected.getHoTen(), false);
                autoTenHS.dismissDropDown();
                autoTenHS.clearFocus();
                hideKeyboard();
            }
        });
    }

    private void performSearch() {
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

            ((TextView) itemView.findViewById(R.id.tvTB1)).setText(formatScore(findValue(item, "DiemHK1", "DTB_HK1")));
            ((TextView) itemView.findViewById(R.id.tvTB2)).setText(formatScore(findValue(item, "DiemHK2", "DTB_HK2")));
            ((TextView) itemView.findViewById(R.id.tvTBCN)).setText(formatScore(findValue(item, "DiemCaNam", "DiemTrungBinhMon", "TBCN")));
        });
        rvKetQua.setAdapter(adapter);
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
