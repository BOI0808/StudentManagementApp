package com.example.studentmanagementapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryTermActivity extends AppCompatActivity {

    private TextInputLayout tilNamBatDau, tilNamKetThuc, tilSearchTerm;
    private TextInputEditText edtNamBatDau, edtNamKetThuc, edtSearchTerm;
    private RadioGroup rgHocKy;
    private RecyclerView rvNamHoc;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;
    private ProgressBar pbSearchLoading;
    private List<Map<String, String>> termList = new ArrayList<>();
    private List<Map<String, String>> filteredTermList = new ArrayList<>();
    private GenericAdapter<Map<String, String>> adapter;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_term);

        initViews();
        setupListeners();
        loadTermList();
    }

    private void initViews() {
        tilNamBatDau = findViewById(R.id.tilNamBatDau);
        tilNamKetThuc = findViewById(R.id.tilNamKetThuc);
        edtNamBatDau = findViewById(R.id.edtNamBatDau);
        edtNamKetThuc = findViewById(R.id.edtNamKetThuc);
        
        tilSearchTerm = findViewById(R.id.tilSearchTerm);
        edtSearchTerm = findViewById(R.id.edtSearchTerm);
        pbSearchLoading = findViewById(R.id.pbSearchLoading);
        
        rgHocKy = findViewById(R.id.rgHocKy);
        rvNamHoc = findViewById(R.id.rvNamHoc);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);

        rvNamHoc.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.btnThemNamHoc).setOnClickListener(v -> performAddTerm());

        // Auto-fill: Năm kết thúc = Năm bắt đầu + 1
        edtNamBatDau.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                tilNamBatDau.setError(null);
                if (s.length() == 4) {
                    try {
                        int namBD = Integer.parseInt(s.toString());
                        edtNamKetThuc.setText(String.valueOf(namBD + 1));
                        tilNamKetThuc.setError(null);
                    } catch (NumberFormatException ignored) {}
                }
            }
        });

        edtNamKetThuc.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                tilNamKetThuc.setError(null);
            }
        });

        edtSearchTerm.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    pbSearchLoading.setVisibility(View.VISIBLE);
                } else {
                    pbSearchLoading.setVisibility(View.GONE);
                }

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> {
                    filterTerms(s.toString());
                    pbSearchLoading.setVisibility(View.GONE);
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadTermList() {
        progressIndicator.setVisibility(View.VISIBLE);
        ApiClient.getApiService().getSemesterList().enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call, @NonNull Response<List<Map<String, String>>> response) {
                progressIndicator.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    termList.clear();
                    termList.addAll(response.body());

                    // Sắp xếp danh sách giảm dần (Năm mới nhất, Học kỳ mới nhất lên đầu)
                    Collections.sort(termList, (o1, o2) -> {
                        int yearCompare = o2.get("namhoc").compareTo(o1.get("namhoc"));
                        if (yearCompare != 0) return yearCompare;
                        return o2.get("hocky").compareTo(o1.get("hocky"));
                    });

                    filterTerms(edtSearchTerm.getText().toString());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(CategoryTermActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterTerms(String query) {
        filteredTermList.clear();
        String lowerQuery = query.toLowerCase().trim();
        
        if (lowerQuery.isEmpty()) {
            filteredTermList.addAll(termList);
        } else {
            for (Map<String, String> term : termList) {
                String namHoc = term.get("namhoc").toLowerCase();
                String hocKy = term.get("hocky").toLowerCase();
                if (namHoc.contains(lowerQuery) || hocKy.contains(lowerQuery)) {
                    filteredTermList.add(term);
                }
            }
        }
        updateRecyclerView();
    }

    private void updateRecyclerView() {
        adapter = new GenericAdapter<>(filteredTermList, R.layout.item_term_row, (term, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvNamHoc)).setText(term.get("namhoc"));
            ((TextView) itemView.findViewById(R.id.tvHocKy)).setText(term.get("hocky"));
            
            itemView.findViewById(R.id.btnDeleteTerm).setOnClickListener(v -> {
                String ma = term.get("ma");
                if (ma != null) {
                    showDeleteConfirmation(ma);
                }
            });
        });
        rvNamHoc.setAdapter(adapter);
    }

    private void showDeleteConfirmation(String ma) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa học kỳ này không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> deleteTerm(ma))
                .show();
    }

    private void deleteTerm(String ma) {
        progressIndicator.setVisibility(View.VISIBLE);
        ApiClient.getApiService().deleteSemester(ma).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                progressIndicator.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    loadTermList();
                } else {
                    Toast.makeText(CategoryTermActivity.this, "Không thể xóa học kỳ này", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(CategoryTermActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performAddTerm() {
        String strNamBD = edtNamBatDau.getText().toString().trim();
        String strNamKT = edtNamKetThuc.getText().toString().trim();

        boolean isValid = true;
        if (strNamBD.isEmpty()) { tilNamBatDau.setError("Không được để trống"); isValid = false; }
        if (strNamKT.isEmpty()) { tilNamKetThuc.setError("Không được để trống"); isValid = false; }
        if (!isValid) return;

        try {
            int namBD = Integer.parseInt(strNamBD);
            int namKT = Integer.parseInt(strNamKT);
            if (namBD >= namKT) {
                tilNamBatDau.setError("Năm bắt đầu phải nhỏ hơn năm kết thúc");
                isValid = false;
            } else if (namKT - namBD != 1) {
                tilNamKetThuc.setError("Khoảng cách phải đúng bằng 1 năm");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            tilNamBatDau.setError("Năm không hợp lệ");
            isValid = false;
        }
        if (!isValid) return;

        String targetYear = strNamBD + "-" + strNamKT;
        boolean hasHK1 = false;
        boolean hasHK2 = false;
        for (Map<String, String> term : termList) {
            if (targetYear.equals(term.get("namhoc"))) {
                String hk = term.get("hocky");
                if ("Học kỳ 1".equals(hk)) hasHK1 = true;
                else if ("Học kỳ 2".equals(hk)) hasHK2 = true;
            }
        }

        int hockyValueToSend;
        String hockySuccessText;
        int checkedId = rgHocKy.getCheckedRadioButtonId();
        if (checkedId == R.id.rbHocKy1) {
            if (hasHK1) { showSimpleErrorDialog("Học kỳ 1 đã tồn tại trong năm học " + targetYear); return; }
            hockyValueToSend = 1; hockySuccessText = "học kỳ 1";
        } else if (checkedId == R.id.rbHocKy2) {
            if (hasHK2) { showSimpleErrorDialog("Học kỳ 2 đã tồn tại trong năm học " + targetYear); return; }
            hockyValueToSend = 2; hockySuccessText = "học kỳ 2";
        } else {
            if (hasHK1 && hasHK2) { showSimpleErrorDialog("Năm học " + targetYear + " đã có cả 2 học kỳ!"); return; }
            else if (hasHK1) { hockyValueToSend = 2; hockySuccessText = "bổ sung học kỳ 2"; }
            else if (hasHK2) { hockyValueToSend = 1; hockySuccessText = "bổ sung học kỳ 1"; }
            else { hockyValueToSend = 3; hockySuccessText = "học kỳ 1 và học kỳ 2"; }
        }

        final String finalSuccessText = hockySuccessText;
        Map<String, Object> data = new HashMap<>();
        data.put("NamHocBatDau", Integer.parseInt(strNamBD));
        data.put("NamHocKetThuc", Integer.parseInt(strNamKT));
        data.put("HocKy", hockyValueToSend);

        progressIndicator.setVisibility(View.VISIBLE);
        ApiClient.getApiService().createSemester(data).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                progressIndicator.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    String message = String.format("Đã tạo %s năm học %s thành công!", finalSuccessText, targetYear);
                    new MaterialAlertDialogBuilder(CategoryTermActivity.this)
                            .setTitle("Thành công")
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton("Tạo tiếp", (dialog, which) -> {
                                edtNamBatDau.setText("");
                                edtNamKetThuc.setText("");
                                loadTermList();
                                dialog.dismiss();
                            })
                            .setNegativeButton("Đóng", (dialog, which) -> finish())
                            .show();
                } else {
                    showSimpleErrorDialog("Học kỳ này đã tồn tại trên hệ thống!");
                }
            }
            @Override public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(CategoryTermActivity.this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSimpleErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Thất bại")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
