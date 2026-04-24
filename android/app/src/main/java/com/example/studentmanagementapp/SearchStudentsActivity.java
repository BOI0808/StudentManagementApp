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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchStudentsActivity extends AppCompatActivity {

    private AutoCompleteTextView autoLop, autoMaHS, autoTenHS;
    private MaterialButton btnTimKiem;
    private RecyclerView rvKetQua;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;
    private LinearLayout layoutEmpty;

    private List<Map<String, Object>> searchResults = new ArrayList<>();
    private List<Student> studentListInClass = new ArrayList<>();
    private String selectedMaLop = "";
    private boolean isProgrammaticChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_students);

        initViews();
        setupClassAutocomplete();
        setupStudentAutocomplete();

        btnBack.setOnClickListener(v -> finish());
        btnTimKiem.setOnClickListener(v -> performSearch());
    }

    private void initViews() {
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

    private void setupClassAutocomplete() {
        autoLop.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isProgrammaticChange) {
                    isProgrammaticChange = false;
                    return;
                }
                if (s.length() == 0) {
                    selectedMaLop = "";
                    studentListInClass.clear();
                    autoMaHS.setAdapter(null);
                    autoTenHS.setAdapter(null);
                    autoMaHS.setText("");
                    autoTenHS.setText("");
                } else if (s.length() >= 1) {
                    ApiClient.getApiService().suggestClass(s.toString()).enqueue(new Callback<List<ClassModel>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<ClassModel>> call, @NonNull Response<List<ClassModel>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                ArrayAdapter<ClassModel> adapter = new ArrayAdapter<ClassModel>(SearchStudentsActivity.this, R.layout.item_dropdown_2line, response.body()) {
                                    @NonNull
                                    @Override
                                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                                        if (convertView == null) {
                                            convertView = getLayoutInflater().inflate(R.layout.item_dropdown_2line, parent, false);
                                        }
                                        ClassModel item = getItem(position);
                                        if (item != null) {
                                            ((TextView) convertView.findViewById(R.id.text1)).setText(item.getTenLop());
                                            String info = "Năm học: " + item.getNamHoc() + " - Học kỳ: " + item.getTenHocKy();
                                            ((TextView) convertView.findViewById(R.id.text2)).setText(info);
                                        }
                                        return convertView;
                                    }
                                };
                                autoLop.setAdapter(adapter);
                                if (autoLop.hasFocus()) autoLop.showDropDown();
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
                
                // Cập nhật văn bản hiển thị theo định dạng: TenLop (MaLop)
                String displayText = String.format("%s (%s)", selected.getTenLop(), selected.getMaLop());
                isProgrammaticChange = true;
                
                // 1. Set văn bản và ẩn ngay Dropbox gợi ý
                autoLop.setText(displayText, false);
                autoLop.dismissDropDown(); 
                
                // 2. Ẩn bàn phím và xóa focus để chắc chắn danh sách biến mất
                autoLop.clearFocus();
                hideKeyboard();
                
                // 3. Tải danh sách học sinh
                loadStudentsInClass(selectedMaLop);
            }
        });
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
