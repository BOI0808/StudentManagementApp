package com.example.studentmanagementapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchStudentsActivity extends AppCompatActivity {

    private AutoCompleteTextView autoMaHS, autoTenHS;
    private TextInputLayout tilSearchMaHS, tilSearchTen;
    private ProgressBar pbMaHSLoading, pbTenHSLoading;
    private MaterialButton btnTimKiem;
    private RecyclerView rvKetQua;
    private ImageButton btnBack;
    private LinearProgressIndicator progressIndicator;
    private LinearLayout layoutEmpty;

    private List<Map<String, Object>> searchResults = new ArrayList<>();
    private boolean isProgrammaticChange = false;
    private volatile String latestMaQuery = "";
    private volatile String latestTenQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_students);

        initViews();
        setupErrorWatchers();
        setupGlobalAutocomplete();

        btnBack.setOnClickListener(v -> finish());
        btnTimKiem.setOnClickListener(v -> performSearch());
    }

    private void initViews() {
        tilSearchMaHS = findViewById(R.id.tilSearchMaHS);
        tilSearchTen = findViewById(R.id.tilSearchTen);

        pbMaHSLoading = findViewById(R.id.pbMaHSLoading);
        pbTenHSLoading = findViewById(R.id.pbTenHSLoading);

        tilSearchMaHS.setEnabled(true);
        tilSearchTen.setEnabled(true);

        autoMaHS = findViewById(R.id.edtSearchMaHS);
        autoTenHS = findViewById(R.id.edtSearchTen);
        
        btnTimKiem = findViewById(R.id.btnTimKiem);
        rvKetQua = findViewById(R.id.rvKetQuaTraCuu);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        rvKetQua.setLayoutManager(new LinearLayoutManager(this));
    }

    private void showRetrySnackbar(String message, Runnable retryAction) {
        runOnUiThread(() -> {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                    .setAction("Thử lại", v -> retryAction.run())
                    .show();
        });
    }

    private void setupErrorWatchers() {
        autoMaHS.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isProgrammaticChange) {
                    pbMaHSLoading.setVisibility(View.GONE);
                    return;
                }
                tilSearchMaHS.setError(null);
                tilSearchMaHS.setErrorEnabled(false);

                if (s.toString().trim().length() > 1) {
                    pbMaHSLoading.setVisibility(View.VISIBLE);
                } else {
                    pbMaHSLoading.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        autoTenHS.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isProgrammaticChange) {
                    pbTenHSLoading.setVisibility(View.GONE);
                    return;
                }
                tilSearchTen.setError(null);
                tilSearchTen.setErrorEnabled(false);

                if (s.toString().trim().length() > 1) {
                    pbTenHSLoading.setVisibility(View.VISIBLE);
                } else {
                    pbTenHSLoading.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupGlobalAutocomplete() {
        ArrayAdapter<Map<String, Object>> adapterMa = new ArrayAdapter<Map<String, Object>>(this, R.layout.item_dropdown_2line, new ArrayList<>()) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_dropdown_2line, parent, false);
                }
                Map<String, Object> item = getItem(position);
                if (item != null) {
                    String ma = getStringValue(findValue(item, "MaHocSinh", "maHocSinh", "MAHOCSINH"));
                    String ten = getStringValue(findValue(item, "HoTen", "hoTen", "HOTEN"));
                    ((TextView) convertView.findViewById(R.id.text1)).setText(ma);
                    ((TextView) convertView.findViewById(R.id.text2)).setText(ten);
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
                        
                        if (constraint == null || constraint.toString().trim().length() <= 1) {
                            results.values = new ArrayList<>();
                            results.count = 0;
                            return results;
                        }

                        String query = constraint.toString().trim();
                        latestMaQuery = query;

                        if (!query.equals(latestMaQuery)) {
                            return null;
                        }

                        try {
                            Thread.sleep(400);

                            if (!query.equals(latestMaQuery)) {
                                return null;
                            }

                            Response<List<Map<String, Object>>> response =
                                    ApiClient.getApiService().searchStudentByNameOrId(query, null).execute();
                            
                            if (!query.equals(latestMaQuery)) {
                                return null;
                            }

                            if (response.isSuccessful() && response.body() != null) {
                                results.values = response.body();
                                results.count = response.body().size();
                                return results;
                            }
                        } catch (InterruptedException e) {
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        results.values = new ArrayList<>();
                        results.count = 0;
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        if (constraint != null && constraint.toString().trim().equals(latestMaQuery)) {
                            pbMaHSLoading.setVisibility(View.GONE);
                        }
                        
                        if (results == null) return;
                        clear();
                        if (results.count > 0) {
                            addAll((List<Map<String, Object>>) results.values);
                        }
                        notifyDataSetChanged();
                    }

                    @Override
                    public CharSequence convertResultToString(Object resultValue) {
                        Map<String, Object> map = (Map<String, Object>) resultValue;
                        return getStringValue(findValue(map, "MaHocSinh", "maHocSinh", "MAHOCSINH"));
                    }
                };
            }
        };

        ArrayAdapter<Map<String, Object>> adapterTen = new ArrayAdapter<Map<String, Object>>(this, R.layout.item_dropdown_2line, new ArrayList<>()) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_dropdown_2line, parent, false);
                }
                Map<String, Object> item = getItem(position);
                if (item != null) {
                    String ma = getStringValue(findValue(item, "MaHocSinh", "maHocSinh", "MAHOCSINH"));
                    String ten = getStringValue(findValue(item, "HoTen", "hoTen", "HOTEN"));
                    ((TextView) convertView.findViewById(R.id.text1)).setText(ten);
                    ((TextView) convertView.findViewById(R.id.text2)).setText(ma);
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
                        
                        if (constraint == null || constraint.toString().trim().length() <= 1) {
                            results.values = new ArrayList<>();
                            results.count = 0;
                            return results;
                        }

                        String query = constraint.toString().trim();
                        latestTenQuery = query;

                        if (!query.equals(latestTenQuery)) {
                            return null;
                        }

                        try {
                            Thread.sleep(400);

                            if (!query.equals(latestTenQuery)) {
                                return null;
                            }

                            Response<List<Map<String, Object>>> response =
                                    ApiClient.getApiService().searchStudentByNameOrId(null, query).execute();
                            
                            if (!query.equals(latestTenQuery)) {
                                return null;
                            }

                            if (response.isSuccessful() && response.body() != null) {
                                results.values = response.body();
                                results.count = response.body().size();
                                return results;
                            }
                        } catch (InterruptedException e) {
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        results.values = new ArrayList<>();
                        results.count = 0;
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        if (constraint != null && constraint.toString().trim().equals(latestTenQuery)) {
                            pbTenHSLoading.setVisibility(View.GONE);
                        }

                        if (results == null) return;
                        clear();
                        if (results.count > 0) {
                            addAll((List<Map<String, Object>>) results.values);
                        }
                        notifyDataSetChanged();
                    }

                    @Override
                    public CharSequence convertResultToString(Object resultValue) {
                        Map<String, Object> map = (Map<String, Object>) resultValue;
                        return getStringValue(findValue(map, "HoTen", "hoTen", "HOTEN"));
                    }
                };
            }
        };

        autoMaHS.setAdapter(adapterMa);
        autoTenHS.setAdapter(adapterTen);
        autoMaHS.setThreshold(1);
        autoTenHS.setThreshold(1);

        autoMaHS.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> selected = (Map<String, Object>) parent.getItemAtPosition(position);
            if (selected != null) {
                isProgrammaticChange = true;
                String ma = getStringValue(findValue(selected, "MaHocSinh", "maHocSinh", "MAHOCSINH"));
                String ten = getStringValue(findValue(selected, "HoTen", "hoTen", "HOTEN"));
                autoMaHS.setText(ma, false);
                autoTenHS.setText(ten, false);
                isProgrammaticChange = false;
                pbMaHSLoading.setVisibility(View.GONE);
                pbTenHSLoading.setVisibility(View.GONE);

                autoMaHS.clearFocus();
                autoTenHS.clearFocus();

                hideKeyboard();
            }
        });

        autoTenHS.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> selected = (Map<String, Object>) parent.getItemAtPosition(position);
            if (selected != null) {
                isProgrammaticChange = true;
                String ma = getStringValue(findValue(selected, "MaHocSinh", "maHocSinh", "MAHOCSINH"));
                String ten = getStringValue(findValue(selected, "HoTen", "hoTen", "HOTEN"));
                autoMaHS.setText(ma, false);
                autoTenHS.setText(ten, false);
                isProgrammaticChange = false;
                pbMaHSLoading.setVisibility(View.GONE);
                pbTenHSLoading.setVisibility(View.GONE);

                autoMaHS.clearFocus();
                autoTenHS.clearFocus();

                hideKeyboard();
            }
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

    private void performSearch() {
        String maHS = autoMaHS.getText().toString().trim();
        String tenHS = autoTenHS.getText().toString().trim();

        if (maHS.isEmpty() && tenHS.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Vui lòng nhập Mã học sinh hoặc Tên học sinh", Snackbar.LENGTH_LONG).show();
            return;
        }

        hideKeyboard();
        showLoading();
        btnTimKiem.setEnabled(false);

        ApiClient.getApiService().searchStudentByNameOrId(maHS, tenHS).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                hideLoading();
                btnTimKiem.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    searchResults = response.body();
                    if (searchResults.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvKetQua.setVisibility(View.GONE);
                        Snackbar.make(findViewById(android.R.id.content), "Không tìm thấy kết quả phù hợp", Snackbar.LENGTH_SHORT).show();
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
                showRetrySnackbar("Lỗi kết nối máy chủ", () -> performSearch());
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

            Object classesObj = findValue(item, "classes", "Classes");
            String classesString = "N/A";
            if (classesObj instanceof List) {
                List<?> classesList = (List<?>) classesObj;
                if (!classesList.isEmpty()) {
                    StringBuilder classesBuilder = new StringBuilder();
                    for (int i = 0; i < classesList.size(); i++) {
                        Object classObj = classesList.get(i);
                        if (classObj instanceof Map) {
                            Map<?, ?> classMap = (Map<?, ?>) classObj;
                            String className = getStringValue(findValueInMap(classMap, "TenLop", "tenLop", "lop"));
                            String yearName = getStringValue(findValueInMap(classMap, "NamHoc", "namHoc"));
                            if (classesBuilder.length() > 0) {
                                classesBuilder.append("; ");
                            }
                            classesBuilder.append(String.format("%s (%s)", className, yearName));
                        }
                    }
                    classesString = classesBuilder.toString();
                }
            } else {
                String lop = getStringValue(findValue(item, "TenLop", "lop"));
                String namHoc = getStringValue(findValue(item, "NamHoc", "namHoc", "NamHocBatDau"));
                if (!lop.equals("N/A") || !namHoc.equals("N/A")) {
                    classesString = String.format("%s (%s)", lop, namHoc);
                }
            }



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
                    Map<String, Object> preparedData = prepareStudentDataForIntent(item);
                    
                    if (which == 0) {
                        Intent intent = new Intent(this, StudentInfoActivity.class);
                        intent.putExtra("student_data", (Serializable) preparedData);
                        startActivity(intent);
                    } else if (which == 1) {
                        Intent intent = new Intent(this, StudentScoreActivity.class);
                        intent.putExtra("student_data", (Serializable) preparedData);
                        startActivity(intent);
                    }
                })
                .show();
    }

    /**
     * Chuyển đổi dữ liệu lồng nhau (classes) thành các trường phẳng ở root map
     * để StudentScoreActivity và StudentInfoActivity can trực tiếp hiển thị thông tin & điểm số mới nhất.
     */
    private Map<String, Object> prepareStudentDataForIntent(Map<String, Object> item) {
        Map<String, Object> flatMap = new HashMap<>(item);
        Object classesObj = findValue(item, "classes", "Classes");
        if (classesObj instanceof List) {
            List<?> classesList = (List<?>) classesObj;
            if (!classesList.isEmpty()) {
                Object latestClassObj = classesList.get(classesList.size() - 1);
                if (latestClassObj instanceof Map) {
                    Map<?, ?> classMap = (Map<?, ?>) latestClassObj;
                    for (Map.Entry<?, ?> entry : classMap.entrySet()) {
                        if (entry.getKey() != null) {
                            flatMap.put(entry.getKey().toString(), entry.getValue());
                        }
                    }
                }
            }
        }
        return flatMap;
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

    private Object findValueInMap(Map<?, ?> map, String... keys) {
        if (map == null) return null;
        for (String key : keys) {
            if (map.containsKey(key)) return map.get(key);
            for (Object actualKey : map.keySet()) {
                if (actualKey != null && actualKey.toString().equalsIgnoreCase(key)) return map.get(actualKey);
            }
        }
        return null;
    }

    private String getStringValue(Object obj) {
        if (obj == null || obj.toString().isEmpty() || obj.toString().equalsIgnoreCase("null")) return "N/A";
        return obj.toString();
    }
}
