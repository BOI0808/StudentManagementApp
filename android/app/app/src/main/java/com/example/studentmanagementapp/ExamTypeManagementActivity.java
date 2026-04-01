package com.example.studentmanagementapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class ExamTypeManagementActivity extends AppCompatActivity {

    private TextInputEditText edtTestTypeName, edtCoefficient;
    private MaterialButton btnAddExamType;
    private RecyclerView rvExamTypes;
    private ExamTypeAdapter adapter;
    private List<ExamType> examTypeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_type_management);

        initViews();
        setupRecyclerView();
        setEvents();
    }

    private void initViews() {
        edtTestTypeName = findViewById(R.id.edtTestTypeName);
        edtCoefficient = findViewById(R.id.edtCoefficient);
        btnAddExamType = findViewById(R.id.btnAddExamType);
        rvExamTypes = findViewById(R.id.rvExamTypes);
        
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        examTypeList = new ArrayList<>();
        // Dữ liệu mẫu ban đầu
        examTypeList.add(new ExamType("Kiểm tra 15 phút", 1.0));
        examTypeList.add(new ExamType("Kiểm tra 1 tiết", 2.0));

        adapter = new ExamTypeAdapter(examTypeList, position -> {
            // Xử lý xóa
            examTypeList.remove(position);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Đã xóa loại hình kiểm tra", Toast.LENGTH_SHORT).show();
        });

        rvExamTypes.setLayoutManager(new LinearLayoutManager(this));
        rvExamTypes.setAdapter(adapter);
    }

    private void setEvents() {
        btnAddExamType.setOnClickListener(v -> {
            String name = edtTestTypeName.getText().toString().trim();
            String coefStr = edtCoefficient.getText().toString().trim();

            if (name.isEmpty() || coefStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double coefficient = Double.parseDouble(coefStr);
                
                // Thêm vào danh sách
                ExamType newType = new ExamType(name, coefficient);
                examTypeList.add(newType);
                
                // Cập nhật giao diện
                adapter.notifyItemInserted(examTypeList.size() - 1);
                rvExamTypes.scrollToPosition(examTypeList.size() - 1);
                
                // Xóa trống ô nhập
                edtTestTypeName.setText("");
                edtCoefficient.setText("");
                edtTestTypeName.requestFocus();
                
                Toast.makeText(this, "Đã thêm loại hình kiểm tra mới", Toast.LENGTH_SHORT).show();
                
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Hệ số phải là một con số", Toast.LENGTH_SHORT).show();
            }
        });
    }
}