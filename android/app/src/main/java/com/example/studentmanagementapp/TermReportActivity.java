package com.example.studentmanagementapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

public class TermReportActivity extends AppCompatActivity {

    private AutoCompleteTextView autoNamHoc, autoHocKy;
    private TextInputLayout tilNamHoc, tilHocKy;
    private MaterialButton btnXem;
    private RecyclerView rvReport;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;

    private List<Map<String, String>> termList = new ArrayList<>();
    private List<Map<String, Object>> reportData = new ArrayList<>();

    private String selectedMaHK = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_term_report);

        initViews();
        loadFilters();

        btnBack.setOnClickListener(v -> finish());
        btnXem.setOnClickListener(v -> loadReport());
    }

    private void initViews() {
        autoNamHoc = findViewById(R.id.autoCompleteNamHoc);
        autoHocKy = findViewById(R.id.autoCompleteHocKy);
        tilNamHoc = findViewById(R.id.tilNamHoc);
        tilHocKy = findViewById(R.id.tilHocKy);
        btnXem = findViewById(R.id.btnLapBaoCaoHocKy);
        rvReport = findViewById(R.id.rcvReportHocKy);
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
    }

    private void setupSemesterSpinners() {
        // 1. Xác định niên khóa hiện tại
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH); // Tháng 9 là Calendar.SEPTEMBER (8)
        int thresholdYear = (currentMonth >= Calendar.SEPTEMBER) ? currentYear : currentYear - 1;

        List<String> years = new ArrayList<>();
        for (Map<String, String> m : termList) {
            String namhoc = m.get("namhoc");
            if (namhoc != null && !years.contains(namhoc)) {
                try {
                    // 2. Lọc dữ liệu: Lấy năm bắt đầu (VD: "2023-2024" -> 2023)
                    String startYearStr = namhoc.split("-")[0].trim();
                    int startYear = Integer.parseInt(startYearStr);
                    if (startYear >= thresholdYear) {
                        years.add(namhoc);
                    }
                } catch (Exception ignored) {}
            }
        }

        // 3. Sắp xếp giảm dần
        Collections.sort(years, Collections.reverseOrder());

        autoNamHoc.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));

        autoNamHoc.setOnItemClickListener((parent, view, position, id) -> {
            // Xóa lỗi và reset trạng thái
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
            tilNamHoc.setErrorEnabled(true);
            tilNamHoc.setError("Vui lòng chọn năm học");
            isValid = false;
        }
        if (selectedMaHK.isEmpty()) {
            tilHocKy.setErrorEnabled(true);
            tilHocKy.setError("Vui lòng chọn học kỳ");
            isValid = false;
        }

        if (!isValid) return;

        showLoading();

        ApiClient.getApiService().getTermReport(selectedMaHK).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    reportData = response.body();
                    if (reportData.isEmpty()) {
                        rvReport.setVisibility(View.GONE);
                        new MaterialAlertDialogBuilder(TermReportActivity.this)
                                .setTitle("Thất bại")
                                .setMessage("Hiện tại không có dữ liệu báo cáo cho học kỳ này.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        rvReport.setVisibility(View.VISIBLE);
                        setupAdapter();
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                hideLoading();
                new MaterialAlertDialogBuilder(TermReportActivity.this)
                        .setTitle("Lỗi kết nối")
                        .setMessage("Không thể tải báo cáo. Vui lòng kiểm tra lại mạng hoặc thử lại sau.")
                        .setPositiveButton("OK", null)
                        .show();
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
