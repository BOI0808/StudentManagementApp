package com.example.studentmanagementapp;

import android.os.Bundle;
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
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategorySubjectActivity extends AppCompatActivity {

    private TextInputEditText edtTenMon;
    private MaterialButton btnThem;
    private RecyclerView rvMonHoc;
    private ImageButton btnBack;
    private List<Subject> subjectList = new ArrayList<>();
    private GenericAdapter<Subject> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_subject);

        edtTenMon = findViewById(R.id.edtTenMonHoc);
        btnThem = findViewById(R.id.btnThem);
        rvMonHoc = findViewById(R.id.rvMonHoc);
        btnBack = findViewById(R.id.btnBack);

        rvMonHoc.setLayoutManager(new LinearLayoutManager(this));
        
        btnBack.setOnClickListener(v -> finish());
        btnThem.setOnClickListener(v -> performAddSubject());

        loadSubjectList();
    }

    private void loadSubjectList() {
        ApiClient.getApiService().getSubjectList().enqueue(new Callback<List<Subject>>() {
            @Override
            public void onResponse(@NonNull Call<List<Subject>> call, @NonNull Response<List<Subject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    subjectList = response.body();
                    updateRecyclerView();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Subject>> call, @NonNull Throwable t) {
                Toast.makeText(CategorySubjectActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecyclerView() {
        adapter = new GenericAdapter<>(subjectList, R.layout.item_category, (subject, itemView, position) -> {
            ((TextView) itemView.findViewById(R.id.tvSTT)).setText(String.valueOf(position + 1));
            ((TextView) itemView.findViewById(R.id.tvMa)).setText(subject.getMaMonHoc());
            ((TextView) itemView.findViewById(R.id.tvTen)).setText(subject.getTenMonHoc());
            
            itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
                String maMon = subject.getMaMonHoc();
                if (maMon != null) {
                    performSoftDeleteSubject(maMon);
                }
            });
        });
        rvMonHoc.setAdapter(adapter);
    }

    private void performSoftDeleteSubject(String maMonHoc) {
        // Gửi trạng thái TrangThai = 0 để ẩn môn học (Soft Delete)
        Map<String, Integer> status = new HashMap<>();
        status.put("TrangThai", 0);

        ApiClient.getApiService().updateSubjectStatus(maMonHoc, status).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CategorySubjectActivity.this, "Đã ẩn môn học khỏi danh sách", Toast.LENGTH_SHORT).show();
                    loadSubjectList(); // Tải lại danh sách (Backend sẽ chỉ trả về những môn có TrangThai = 1)
                } else {
                    Toast.makeText(CategorySubjectActivity.this, "Không thể cập nhật trạng thái môn học", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                Toast.makeText(CategorySubjectActivity.this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performAddSubject() {
        String tenMon = edtTenMon.getText() != null ? edtTenMon.getText().toString().trim() : "";
        if (tenMon.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên môn", Toast.LENGTH_SHORT).show();
            return;
        }

        Subject newSubject = new Subject();
        newSubject.setTenMonHocInput(tenMon);

        btnThem.setEnabled(false);
        ApiClient.getApiService().createSubject(newSubject).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                btnThem.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(CategorySubjectActivity.this, "Thêm môn học thành công", Toast.LENGTH_SHORT).show();
                    edtTenMon.setText("");
                    loadSubjectList();
                } else {
                    showErrorDetails(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                btnThem.setEnabled(true);
                Toast.makeText(CategorySubjectActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showErrorDetails(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                JSONObject jObjError = new JSONObject(errorBody);
                String errorMsg = jObjError.optString("error", "Dữ liệu không hợp lệ");
                Toast.makeText(this, "Thất bại: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
        }
    }
}
