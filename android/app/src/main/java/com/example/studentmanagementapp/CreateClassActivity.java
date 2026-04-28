package com.example.studentmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.Block;
import com.example.studentmanagementapp.model.ClassModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateClassActivity extends AppCompatActivity {

    private TextInputEditText edtTenLop;
    private AutoCompleteTextView autoCompleteKhoiLop, autoCompleteNamHoc, autoCompleteHocKy;
    private TextInputLayout tilTenLop, tilKhoiLop, tilNamHoc, tilHocKy;
    private MaterialButton btnTaoLop;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;
    
    private List<Block> blockList = new ArrayList<>();
    private List<Map<String, String>> termList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_class);

        initViews();
        loadData();
        setupErrorClearing();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        btnTaoLop.setOnClickListener(v -> performCreateClass());
    }

    private void initViews() {
        edtTenLop = findViewById(R.id.edtTenLop);
        autoCompleteKhoiLop = findViewById(R.id.autoCompleteKhoiLop);
        autoCompleteNamHoc = findViewById(R.id.autoCompleteNamHoc);
        autoCompleteHocKy = findViewById(R.id.autoCompleteHocKy);
        
        tilTenLop = findViewById(R.id.tilTenLop);
        tilKhoiLop = findViewById(R.id.tilKhoiLop);
        tilNamHoc = findViewById(R.id.tilNamHoc);
        tilHocKy = findViewById(R.id.tilHocKy);
        
        btnTaoLop = findViewById(R.id.btnTaoLop);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);
    }

    private void setupErrorClearing() {
        edtTenLop.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilTenLop != null) {
                    tilTenLop.setError(null);
                    tilTenLop.setErrorEnabled(false);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        autoCompleteKhoiLop.setOnItemClickListener((parent, view, position, id) -> {
            if (tilKhoiLop != null) {
                tilKhoiLop.setError(null);
                tilKhoiLop.setErrorEnabled(false);
            }
        });

        autoCompleteNamHoc.setOnItemClickListener((parent, view, position, id) -> {
            if (tilNamHoc != null) {
                tilNamHoc.setError(null);
                tilNamHoc.setErrorEnabled(false);
            }
        });

        autoCompleteHocKy.setOnItemClickListener((parent, view, position, id) -> {
            if (tilHocKy != null) {
                tilHocKy.setError(null);
                tilHocKy.setErrorEnabled(false);
            }
        });
    }

    private void showLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
        if (btnTaoLop != null) btnTaoLop.setEnabled(false);
    }

    private void hideLoading() {
        if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
        if (btnTaoLop != null) btnTaoLop.setEnabled(true);
    }

    private void loadData() {
        showLoading();
        ApiClient.getApiService().getBlockList().enqueue(new Callback<List<Block>>() {
            @Override
            public void onResponse(@NonNull Call<List<Block>> call, @NonNull Response<List<Block>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    blockList = response.body();
                    List<String> names = new ArrayList<>();
                    for (Block b : blockList) {
                        if (b.getTenKhoiLop() != null) names.add(b.getTenKhoiLop());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(CreateClassActivity.this,
                            android.R.layout.simple_dropdown_item_1line, names);
                    autoCompleteKhoiLop.setAdapter(adapter);
                }
                checkAllDataLoaded();
            }
            @Override
            public void onFailure(@NonNull Call<List<Block>> call, @NonNull Throwable t) {
                checkAllDataLoaded();
            }
        });

        ApiClient.getApiService().getSemesterList().enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    termList = response.body();
                    setupTermSpinners();
                }
                checkAllDataLoaded();
            }
            @Override
            public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {
                checkAllDataLoaded();
            }
        });
    }
    
    private int loadCount = 0;
    private void checkAllDataLoaded() {
        loadCount++;
        if (loadCount >= 2) {
            hideLoading();
            loadCount = 0;
        }
    }

    private void setupTermSpinners() {
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH);
        int effectiveSchoolStartYear = (currentMonth >= Calendar.SEPTEMBER) ? currentYear : currentYear - 1;

        List<String> years = new ArrayList<>();
        for (Map<String, String> m : termList) {
            String namHoc = m.get("namhoc");
            if (namHoc != null && !years.contains(namHoc)) {
                try {
                    String startYearStr = namHoc.contains("-") ? namHoc.split("-")[0].trim() : namHoc.trim();
                    int startYear = Integer.parseInt(startYearStr);
                    if (startYear >= effectiveSchoolStartYear) {
                        years.add(namHoc);
                    }
                } catch (Exception ignored) {}
            }
        }

        autoCompleteNamHoc.setAdapter(new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, years));

        String[] types = {"Học kỳ 1", "Học kỳ 2", "Cả năm"};
        autoCompleteHocKy.setAdapter(new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, types));
    }

    private void resetFields() {
        if (edtTenLop != null) edtTenLop.setText("");
        if (autoCompleteKhoiLop != null) autoCompleteKhoiLop.setText("", false);
        
        if (tilTenLop != null) {
            tilTenLop.setError(null);
            tilTenLop.setErrorEnabled(false);
        }
        if (tilKhoiLop != null) {
            tilKhoiLop.setError(null);
            tilKhoiLop.setErrorEnabled(false);
        }
    }

    private void performCreateClass() {
        final String tenLop = (edtTenLop.getText() != null) ? edtTenLop.getText().toString().trim() : "";
        String tenKhoi = autoCompleteKhoiLop.getText().toString();
        final String namHoc = autoCompleteNamHoc.getText().toString();
        final String hocKyStr = autoCompleteHocKy.getText().toString();

        boolean hasError = false;

        if (tenLop.isEmpty()) {
            if (tilTenLop != null) {
                tilTenLop.setErrorEnabled(true);
                tilTenLop.setError("Tên lớp không được để trống (VD: 10A1)");
            }
            hasError = true;
        }
        if (tenKhoi.isEmpty()) {
            if (tilKhoiLop != null) {
                tilKhoiLop.setErrorEnabled(true);
                tilKhoiLop.setError("Vui lòng chọn khối lớp");
            }
            hasError = true;
        }
        if (namHoc.isEmpty()) {
            if (tilNamHoc != null) {
                tilNamHoc.setErrorEnabled(true);
                tilNamHoc.setError("Vui lòng chọn năm học");
            }
            hasError = true;
        }
        if (hocKyStr.isEmpty()) {
            if (tilHocKy != null) {
                tilHocKy.setErrorEnabled(true);
                tilHocKy.setError("Vui lòng chọn học kỳ");
            }
            hasError = true;
        }

        if (hasError) return;

        ClassModel classModel = new ClassModel();
        classModel.setTenLop(tenLop);
        
        for (Block b : blockList) {
            if (tenKhoi.equals(b.getTenKhoiLop())) {
                classModel.setMaKhoiLop(b.getMaKhoiLop());
                break;
            }
        }

        String selectedHK = "Cả năm".equals(hocKyStr) ? "Học kỳ 1" : hocKyStr;
        for (Map<String, String> m : termList) {
            if (namHoc.equals(m.get("namhoc")) && selectedHK.equals(m.get("hocky"))) {
                classModel.setMaHocKyNamHoc(m.get("ma"));
                break;
            }
        }

        classModel.setLoaiHocKy("Cả năm".equals(hocKyStr) ? 3 : 1);

        showLoading();
        ApiClient.getApiService().createClass(classModel).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    new MaterialAlertDialogBuilder(CreateClassActivity.this)
                            .setTitle("Thành công")
                            .setMessage("Lớp " + tenLop + " đã được tạo thành công.")
                            .setPositiveButton("Tạo lớp tiếp", (dialog, which) -> resetFields())
                            .setNegativeButton("Đóng", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                } else {
                    Toast.makeText(CreateClassActivity.this, "Lớp đã tồn tại hoặc dữ liệu lỗi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                hideLoading();
                Toast.makeText(CreateClassActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
