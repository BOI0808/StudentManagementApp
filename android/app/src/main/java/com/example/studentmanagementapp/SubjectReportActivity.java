package com.example.studentmanagementapp;

import android.os.Bundle;
import android.view.View;
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
import com.example.studentmanagementapp.model.Subject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubjectReportActivity extends AppCompatActivity {

    private AutoCompleteTextView autoNamHoc, autoHocKy, autoMon;
    private TextInputLayout tilNamHoc, tilHocKy, tilMon;
    private MaterialButton btnXem;
    private RecyclerView rvReport;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;

    private List<Map<String, String>> termList = new ArrayList<>();
    private List<Subject> subjectList = new ArrayList<>();
    private List<Map<String, Object>> reportData = new ArrayList<>();

    private String selectedMaHK = "", selectedMaMon = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_report);

        initViews();
        loadFilters();

        btnBack.setOnClickListener(v -> finish());
        btnXem.setOnClickListener(v -> loadReport());
    }

    private void initViews() {
        autoNamHoc = findViewById(R.id.autoCompleteNamHoc);
        autoHocKy = findViewById(R.id.autoCompleteHocKy);
        autoMon = findViewById(R.id.autoCompleteMonHoc);
        
        tilNamHoc = findViewById(R.id.tilNamHoc);
        tilHocKy = findViewById(R.id.tilHocKy);
        tilMon = findViewById(R.id.tilMon);
        
        btnXem = findViewById(R.id.btnLapBaoCao);
        rvReport = findViewById(R.id.rcvReport);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);

        rvReport.setLayoutManager(new LinearLayoutManager(this));
    }

    private void showLoading() {
        progressIndicator.setVisibility(View.VISIBLE);
        rvReport.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressIndicator.setVisibility(View.GONE);
        rvReport.setVisibility(View.VISIBLE);
    }

    private void loadFilters() {
        // Tải Năm học & Học kỳ
        ApiClient.getApiService().getSemesterList().enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    termList = response.body();
                    setupSemesterSpinners();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {}
        });

        // Tải Môn học
        ApiClient.getApiService().getSubjectList().enqueue(new Callback<List<Subject>>() {
            @Override
            public void onResponse(@NonNull Call<List<Subject>> call, @NonNull Response<List<Subject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    subjectList = response.body();
                    List<String> names = new ArrayList<>();
                    for (Subject s : subjectList) {
                        String tenMon = s.getTenMonHoc();
                        if (tenMon != null) names.add(tenMon);
                    }
                    autoMon.setAdapter(new ArrayAdapter<>(SubjectReportActivity.this, android.R.layout.simple_list_item_1, names));
                    autoMon.setOnItemClickListener((p, v, pos, id) -> {
                        if (pos < subjectList.size()) {
                            selectedMaMon = subjectList.get(pos).getMaMonHoc();
                            tilMon.setError(null);
                            tilMon.setErrorEnabled(false);
                        }
                    });
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Subject>> call, @NonNull Throwable t) {}
        });
    }

    private void setupSemesterSpinners() {
        // 1. Xác định niên khóa hiện tại
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH); // Tháng bắt đầu từ 0 (Tháng 1 là 0)
        // Nếu từ tháng 9 trở đi, năm học bắt đầu từ currentYear. Ngược lại là currentYear - 1
        int thresholdYear = (currentMonth >= Calendar.SEPTEMBER) ? currentYear : currentYear - 1;

        List<String> years = new ArrayList<>();
        for (Map<String, String> m : termList) {
            String namhoc = m.get("namhoc");
            if (namhoc != null && !years.contains(namhoc)) {
                try {
                    // 2. Lọc dữ liệu: Lấy năm bắt đầu từ chuỗi (VD: "2023-2024" -> 2023)
                    String startYearStr = namhoc.split("-")[0].trim();
                    int startYear = Integer.parseInt(startYearStr);
                    
                    if (startYear >= thresholdYear) {
                        years.add(namhoc);
                    }
                } catch (Exception e) {
                    // Tối ưu: Bỏ qua nếu chuỗi namhoc không đúng định dạng để tránh crash
                }
            }
        }
        
        // 3. Sắp xếp danh sách giảm dần (Mới nhất ở trên đầu)
        Collections.sort(years, Collections.reverseOrder());

        autoNamHoc.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));

        autoNamHoc.setOnItemClickListener((parent, view, position, id) -> {
            tilNamHoc.setError(null);
            tilNamHoc.setErrorEnabled(false);

            tilHocKy.setEnabled(true);
            autoHocKy.setText("", false);
            selectedMaHK = "";
            tilHocKy.setError(null);
            tilHocKy.setErrorEnabled(false);

            String selectedYear = years.get(position);
            List<String> hks = new ArrayList<>();
            List<String> mas = new ArrayList<>();
            for (Map<String, String> m : termList) {
                if (selectedYear.equals(m.get("namhoc"))) {
                    String hocky = m.get("hocky");
                    String ma = m.get("ma");
                    if (hocky != null) hks.add(hocky);
                    if (ma != null) mas.add(ma);
                }
            }
            autoHocKy.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, hks));
            autoHocKy.setOnItemClickListener((p, v, pos, i) -> {
                if (pos < mas.size()) {
                    selectedMaHK = mas.get(pos);
                    tilHocKy.setError(null);
                    tilHocKy.setErrorEnabled(false);
                }
            });
        });
    }

    private void loadReport() {
        boolean isValid = true;

        if (autoNamHoc.getText().toString().isEmpty()) {
            tilNamHoc.setError("Vui lòng chọn năm học");
            isValid = false;
        }
        if (selectedMaHK.isEmpty()) {
            tilHocKy.setError("Vui lòng chọn học kỳ");
            isValid = false;
        }
        if (selectedMaMon.isEmpty()) {
            tilMon.setError("Vui lòng chọn môn học");
            isValid = false;
        }

        if (!isValid) return;

        showLoading();

        ApiClient.getApiService().getSubjectReport(selectedMaHK, selectedMaMon).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    reportData = response.body();
                    setupAdapter();
                    if (reportData.isEmpty()) {
                        Toast.makeText(SubjectReportActivity.this, "Không có dữ liệu báo cáo", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                hideLoading();
                Toast.makeText(SubjectReportActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAdapter() {
        GenericAdapter<Map<String, Object>> adapter = new GenericAdapter<>(reportData, R.layout.item_report, (item, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            
            Object lop = item.get("lop");
            ((TextView) itemView.findViewById(R.id.tvLop)).setText(lop != null ? lop.toString() : "");
            
            Object siSo = item.get("siSo");
            if (siSo instanceof Double) {
                ((TextView) itemView.findViewById(R.id.tvSiSo)).setText(String.valueOf(((Double) siSo).intValue()));
            } else if (siSo != null) {
                ((TextView) itemView.findViewById(R.id.tvSiSo)).setText(siSo.toString());
            }

            Object soLuongDat = item.get("soLuongDat");
            if (soLuongDat instanceof Double) {
                ((TextView) itemView.findViewById(R.id.tvSoLuongDat)).setText(String.valueOf(((Double) soLuongDat).intValue()));
            } else if (soLuongDat != null) {
                ((TextView) itemView.findViewById(R.id.tvSoLuongDat)).setText(soLuongDat.toString());
            }

            Object tiLe = item.get("tiLe");
            ((TextView) itemView.findViewById(R.id.tvTiLe)).setText(tiLe != null ? tiLe.toString() : "");
        });
        rvReport.setAdapter(adapter);
    }
}
