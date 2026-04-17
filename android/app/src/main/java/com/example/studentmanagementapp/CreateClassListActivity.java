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
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateClassListActivity extends AppCompatActivity {

    private AutoCompleteTextView autoLop, autoHocSinh;
    private MaterialButton btnThem, btnLuu;
    private RecyclerView rvHocSinh;
    private ImageButton btnBack;
    
    private final List<Student> listHocSinhSelected = new ArrayList<>();
    private GenericAdapter<Student> adapter;
    private String selectedMaLop = "";
    private Student selectedStudentToAdd = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_class_list);

        initViews();
        setupClassAutocomplete();
        setupStudentAutocomplete();

        btnBack.setOnClickListener(v -> finish());
        btnThem.setOnClickListener(v -> addStudentToList());
        btnLuu.setOnClickListener(v -> saveClassList());
    }

    private void initViews() {
        autoLop = findViewById(R.id.autoCompleteMaLop);
        autoHocSinh = findViewById(R.id.autoCompleteHocSinh);
        btnThem = findViewById(R.id.btnThemVaoLop);
        btnLuu = findViewById(R.id.btnLuuDanhSachLop);
        rvHocSinh = findViewById(R.id.rvDanhSachHocSinhMoi);
        btnBack = findViewById(R.id.btnBack);

        rvHocSinh.setLayoutManager(new LinearLayoutManager(this));
        setupAdapter();
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
                                List<ClassModel> list = response.body();
                                ArrayAdapter<ClassModel> adapterLop = new ArrayAdapter<>(CreateClassListActivity.this,
                                        android.R.layout.simple_dropdown_item_1line, list);
                                autoLop.setAdapter(adapterLop);
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
                if (s.length() >= 2) {
                    ApiClient.getApiService().searchStudent(s.toString()).enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<List<Student>> call, @NonNull Response<List<Student>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<Student> list = response.body();
                                ArrayAdapter<String> adapterStudent = new ArrayAdapter<>(CreateClassListActivity.this,
                                        android.R.layout.simple_dropdown_item_1line);
                                for(Student st : list) adapterStudent.add(st.getHoTen() + " (" + st.getMaHocSinh() + ")");
                                
                                autoHocSinh.setAdapter(adapterStudent);
                                autoHocSinh.showDropDown();
                                
                                autoHocSinh.setOnItemClickListener((p, v, pos, i) -> {
                                    selectedStudentToAdd = list.get(pos);
                                    autoHocSinh.setText(selectedStudentToAdd.getHoTen());
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
        ApiClient.getApiService().getStudentsByClass(maLop).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listHocSinhSelected.clear();
                    for (Map<String, String> m : response.body()) {
                        Student s = new Student();
                        s.setMaHocSinh(m.get("MaHocSinh"));
                        s.setHoTen(m.get("HoTen"));
                        s.setNgaySinh(m.get("NgaySinh"));
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
            ((TextView) itemView.findViewById(R.id.tvNgaySinh)).setText(student.getNgaySinh());
            
            itemView.findViewById(R.id.btnXoaHocSinh).setOnClickListener(v -> {
                listHocSinhSelected.remove(position);
                adapter.notifyDataSetChanged();
            });
        });
        rvHocSinh.setAdapter(adapter);
    }

    private void addStudentToList() {
        if (selectedStudentToAdd == null) {
            Toast.makeText(this, "Vui lòng chọn học sinh từ danh sách gợi ý", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Kiểm tra trùng trong danh sách
        for (Student s : listHocSinhSelected) {
            if (s.getMaHocSinh() != null && s.getMaHocSinh().equals(selectedStudentToAdd.getMaHocSinh())) {
                Toast.makeText(this, "Học sinh này đã có trong danh sách", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        listHocSinhSelected.add(selectedStudentToAdd);
        adapter.notifyDataSetChanged();
        autoHocSinh.setText("");
        selectedStudentToAdd = null;
    }

    private void saveClassList() {
        if (selectedMaLop.isEmpty() || listHocSinhSelected.isEmpty()) {
            Toast.makeText(this, "Chưa chọn lớp hoặc danh sách trống", Toast.LENGTH_SHORT).show();
            return;
        }

        ClassModel data = new ClassModel();
        data.setMaLop(selectedMaLop);
        List<String> maHSList = new ArrayList<>();
        for (Student s : listHocSinhSelected) maHSList.add(s.getMaHocSinh());
        data.setDanhSachMaHS(maHSList);

        btnLuu.setEnabled(false);
        ApiClient.getApiService().saveClassList(data).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                btnLuu.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(CreateClassListActivity.this, "Lưu danh sách thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreateClassListActivity.this, "Lỗi: Một số học sinh đã có lớp hoặc vượt sĩ số", Toast.LENGTH_LONG).show();
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
