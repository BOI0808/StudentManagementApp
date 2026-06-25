package com.example.studentmanagementapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentScoreActivity extends AppCompatActivity {

    private TextView tvHoTen, tvMaHS;
    private ImageButton btnBack;
    private RecyclerView rvClassHistory;
    private ClassHistoryAdapter adapter;
    private List<Map<String, Object>> classHistoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_score);

        initViews();

        Map<String, Object> studentData = (Map<String, Object>) getIntent().getSerializableExtra("student_data");
        if (studentData != null) {
            displayStudentInfo(studentData);
            
            String maHS = getValue(studentData, "MaHocSinh", "maHocSinh", "MAHOCSINH", "MaHS");
            if (maHS != null && !maHS.equals("N/A")) {
                fetchScoreDetails(maHS);
            } else {
                Toast.makeText(this, "Thiếu thông tin mã học sinh", Toast.LENGTH_SHORT).show();
            }
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvHoTen = findViewById(R.id.tvHoTen);
        tvMaHS = findViewById(R.id.tvMaHS);
        btnBack = findViewById(R.id.btnBack);
        rvClassHistory = findViewById(R.id.rvClassHistory);

        rvClassHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClassHistoryAdapter(classHistoryList);
        rvClassHistory.setAdapter(adapter);
    }

    private void displayStudentInfo(Map<String, Object> data) {
        tvHoTen.setText("Họ tên: " + getValue(data, "HoTen", "hoTen", "HOTEN", "TenHocSinh"));
        tvMaHS.setText("Mã HS: " + getValue(data, "MaHocSinh", "maHocSinh", "MAHOCSINH", "MaHS"));
    }

    private void fetchScoreDetails(String maHocSinh) {
        ApiClient.getApiService().getStudentScoreDetails(maHocSinh, null).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    classHistoryList.clear();
                    classHistoryList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(StudentScoreActivity.this, "Không thể tải dữ liệu điểm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(StudentScoreActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getValue(Map<String, Object> map, String... keys) {
        if (map == null) return "N/A";
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) return map.get(key).toString();
            for (String k : map.keySet()) {
                if (k.equalsIgnoreCase(key) && map.get(k) != null) return map.get(k).toString();
            }
        }
        return "N/A";
    }

    private String formatScore(Object obj) {
        if (obj == null || obj.toString().isEmpty() || obj.toString().equalsIgnoreCase("null")) return "-";
        try {
            double score = Double.parseDouble(obj.toString());
            return String.format(Locale.getDefault(), "%.1f", score);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private class ClassHistoryAdapter extends RecyclerView.Adapter<ClassHistoryAdapter.ViewHolder> {
        private List<Map<String, Object>> list;

        public ClassHistoryAdapter(List<Map<String, Object>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class_score, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> classData = list.get(position);
            String tenLop = getValue(classData, "TenLop", "tenLop");
            String namHoc = getValue(classData, "NamHoc", "namHoc");
            holder.tvClassNameYear.setText(String.format("Lớp %s - Năm học %s", tenLop, namHoc));

            Map<String, Object> tongKet = (Map<String, Object>) classData.get("TongKetChung");
            if (tongKet != null) {
                holder.tvDiemHK1.setText(formatScore(tongKet.get("DiemHK1")));
                holder.tvDiemHK2.setText(formatScore(tongKet.get("DiemHK2")));
                holder.tvDiemCaNam.setText(formatScore(tongKet.get("DiemCaNam")));
            }

            holder.llSubjectsContainerHK1.removeAllViews();
            List<Map<String, Object>> dsMonHK1 = (List<Map<String, Object>>) classData.get("MonHocHocKy1");
            if (dsMonHK1 != null) {
                for (Map<String, Object> monHoc : dsMonHK1) {
                    addSubjectView(holder.llSubjectsContainerHK1, monHoc);
                }
            }

            holder.llSubjectsContainerHK2.removeAllViews();
            List<Map<String, Object>> dsMonHK2 = (List<Map<String, Object>>) classData.get("MonHocHocKy2");
            if (dsMonHK2 != null) {
                for (Map<String, Object> monHoc : dsMonHK2) {
                    addSubjectView(holder.llSubjectsContainerHK2, monHoc);
                }
            }
        }

        private void addSubjectView(LinearLayout container, Map<String, Object> monHoc) {
            View subjectView = LayoutInflater.from(container.getContext()).inflate(R.layout.item_subject_score, container, false);
            
            TextView tvSubjectName = subjectView.findViewById(R.id.tvSubjectName);
            TextView tvAverageScore = subjectView.findViewById(R.id.tvAverageScore);
            LinearLayout llDetailedScoresContainer = subjectView.findViewById(R.id.llDetailedScoresContainer);

            tvSubjectName.setText(getValue(monHoc, "TenMonHoc", "tenMonHoc"));
            tvAverageScore.setText(formatScore(monHoc.get("DiemTrungBinhMon")));

            llDetailedScoresContainer.removeAllViews();
            List<Map<String, Object>> chiTiet = (List<Map<String, Object>>) monHoc.get("DanhSachDiemChiTiet");
            if (chiTiet != null) {
                for (Map<String, Object> diem : chiTiet) {
                    TextView tv = new TextView(container.getContext());
                    String loai = getValue(diem, "TenLoaiKiemTra", "tenLoaiKiemTra");
                    String value = formatScore(diem.get("Diem"));
                    String ghiChu = getValue(diem, "GhiChu", "ghiChu");
                    
                    String text = loai + ": " + value;
                    if (!ghiChu.equals("N/A") && !ghiChu.isEmpty()) {
                        text += " (" + ghiChu + ")";
                    }
                    
                    tv.setText(text);
                    tv.setPadding(0, 4, 0, 4);
                    tv.setTextColor(0xFF333333);
                    tv.setTextSize(14);
                    llDetailedScoresContainer.addView(tv);
                }
            }
            container.addView(subjectView);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvClassNameYear, tvDiemHK1, tvDiemHK2, tvDiemCaNam;
            LinearLayout llSubjectsContainerHK1, llSubjectsContainerHK2;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClassNameYear = itemView.findViewById(R.id.tvClassNameYear);
                tvDiemHK1 = itemView.findViewById(R.id.tvDiemHK1);
                tvDiemHK2 = itemView.findViewById(R.id.tvDiemHK2);
                tvDiemCaNam = itemView.findViewById(R.id.tvDiemCaNam);
                llSubjectsContainerHK1 = itemView.findViewById(R.id.llSubjectsContainerHK1);
                llSubjectsContainerHK2 = itemView.findViewById(R.id.llSubjectsContainerHK2);
            }
        }
    }
}
