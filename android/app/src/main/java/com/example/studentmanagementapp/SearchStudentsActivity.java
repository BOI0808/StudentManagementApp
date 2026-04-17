package com.example.studentmanagementapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.ClassModel;
import com.example.studentmanagementapp.model.Student;
import com.google.android.material.button.MaterialButton;
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

    private List<Map<String, Object>> searchResults = new ArrayList<>();
    private List<Student> studentListInClass = new ArrayList<>();
    private String selectedMaLop = "";

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

        rvKetQua.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupClassAutocomplete() {
        autoLop.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 1) {
                    ApiClient.getApiService().suggestClass(s.toString()).enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<List<ClassModel>> call, @NonNull Response<List<ClassModel>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                ArrayAdapter<ClassModel> adapter = new ArrayAdapter<>(SearchStudentsActivity.this,
                                        android.R.layout.simple_dropdown_item_1line, response.body());
                                autoLop.setAdapter(adapter);
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
                loadStudentsInClass(selectedMaLop);
            }
        });
    }

    private void loadStudentsInClass(String maLop) {
        ApiClient.getApiService().getStudentsByClass(maLop).enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    studentListInClass.clear();
                    List<String> maHSList = new ArrayList<>();
                    List<String> hoTenList = new ArrayList<>();
                    
                    for (Map<String, String> m : response.body()) {
                        Student s = new Student();
                        s.setMaHocSinh(m.get("MaHocSinh"));
                        s.setHoTen(m.get("HoTen"));
                        studentListInClass.add(s);
                        
                        if (s.getMaHocSinh() != null) maHSList.add(s.getMaHocSinh());
                        if (s.getHoTen() != null) hoTenList.add(s.getHoTen());
                    }
                    
                    autoMaHS.setAdapter(new ArrayAdapter<>(SearchStudentsActivity.this, 
                            android.R.layout.simple_dropdown_item_1line, maHSList));
                    autoTenHS.setAdapter(new ArrayAdapter<>(SearchStudentsActivity.this, 
                            android.R.layout.simple_dropdown_item_1line, hoTenList));
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {}
        });
    }

    private void setupStudentAutocomplete() {
        autoMaHS.setOnItemClickListener((parent, view, position, id) -> {
            String selectedMa = (String) parent.getItemAtPosition(position);
            for (Student s : studentListInClass) {
                if (selectedMa.equals(s.getMaHocSinh())) {
                    autoTenHS.setText(s.getHoTen());
                    break;
                }
            }
        });

        autoTenHS.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTen = (String) parent.getItemAtPosition(position);
            for (Student s : studentListInClass) {
                if (selectedTen.equals(s.getHoTen())) {
                    autoMaHS.setText(s.getMaHocSinh());
                    break;
                }
            }
        });
    }

    private void performSearch() {
        String maHS = autoMaHS.getText().toString().trim();
        String tenHS = autoTenHS.getText().toString().trim();

        btnTimKiem.setEnabled(false);
        ApiClient.getApiService().getSearchResult(selectedMaLop, tenHS, maHS).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                btnTimKiem.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    searchResults = response.body();
                    setupAdapter();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                btnTimKiem.setEnabled(true);
                Toast.makeText(SearchStudentsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAdapter() {
        GenericAdapter<Map<String, Object>> adapter = new GenericAdapter<>(searchResults, R.layout.item_search_students, (item, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvMaHS)).setText(getStringValue(item.get("maHocSinh")));
            ((TextView) itemView.findViewById(R.id.tvTenHS)).setText(getStringValue(item.get("hoTen")));
            
            String lop = getStringValue(item.get("lop"));
            String namHoc = getStringValue(item.get("namHoc"));
            ((TextView) itemView.findViewById(R.id.tvLopNamHoc)).setText(String.format(Locale.getDefault(), "Lớp: %s  |  Năm học: %s", lop, namHoc));
            
            ((TextView) itemView.findViewById(R.id.tvTB1)).setText(getStringValue(item.get("diemHK1")));
            ((TextView) itemView.findViewById(R.id.tvTB2)).setText(getStringValue(item.get("diemHK2")));
            ((TextView) itemView.findViewById(R.id.tvTBCN)).setText(getStringValue(item.get("diemCaNam")));
        });
        rvKetQua.setAdapter(adapter);
    }

    private String getStringValue(Object obj) {
        if (obj == null) return "N/A";
        return obj.toString();
    }
}
