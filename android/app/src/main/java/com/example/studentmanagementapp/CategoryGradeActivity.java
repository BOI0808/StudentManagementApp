package com.example.studentmanagementapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanagementapp.api.ApiClient;
import com.example.studentmanagementapp.model.Block;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryGradeActivity extends AppCompatActivity {

    private TextInputEditText edtTenKhoi;
    private MaterialButton btnThem;
    private RecyclerView rvKhoiLop;
    private ImageButton btnBack;
    private List<Block> blockList = new ArrayList<>();
    private GenericAdapter<Block> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_grade);

        initViews();
        loadBlockList();

        btnBack.setOnClickListener(v -> finish());
        btnThem.setOnClickListener(v -> performAddBlock());
    }

    private void initViews() {
        edtTenKhoi = findViewById(R.id.edtTenKhoi);
        btnThem = findViewById(R.id.btnThem);
        rvKhoiLop = findViewById(R.id.rvKhoiLop);
        btnBack = findViewById(R.id.btnBack);

        rvKhoiLop.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadBlockList() {
        ApiClient.getApiService().getBlockList().enqueue(new Callback<List<Block>>() {
            @Override
            public void onResponse(Call<List<Block>> call, Response<List<Block>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    blockList.clear();
                    blockList.addAll(response.body());
                    updateRecyclerView();
                }
            }

            @Override
            public void onFailure(Call<List<Block>> call, Throwable t) {
                Toast.makeText(CategoryGradeActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecyclerView() {
        adapter = new GenericAdapter<>(blockList, R.layout.item_category, (item, itemView, position) -> {
            TextView tvSTT = itemView.findViewById(R.id.tvSTT);
            TextView tvMa = itemView.findViewById(R.id.tvMa);
            TextView tvTen = itemView.findViewById(R.id.tvTen);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDelete);

            tvSTT.setText(String.valueOf(position + 1));
            tvMa.setText(item.getMaKhoiLop());
            tvTen.setText(item.getTenKhoiLop());

            btnDelete.setOnClickListener(v -> performDeleteBlock(item.getMaKhoiLop()));
        });
        rvKhoiLop.setAdapter(adapter);
    }

    private void performAddBlock() {
        String tenKhoi = edtTenKhoi.getText() != null ? edtTenKhoi.getText().toString().trim() : "";
        if (tenKhoi.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên khối", Toast.LENGTH_SHORT).show();
            return;
        }

        Block newBlock = new Block();
        newBlock.setTenKhoiLopInput(tenKhoi);

        ApiClient.getApiService().createBlock(newBlock).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CategoryGradeActivity.this, "Thêm thành công", Toast.LENGTH_SHORT).show();
                    edtTenKhoi.setText("");
                    loadBlockList();
                } else {
                    Toast.makeText(CategoryGradeActivity.this, "Khối đã tồn tại hoặc lỗi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(CategoryGradeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performDeleteBlock(String maKhoiLop) {
        Map<String, Integer> status = new HashMap<>();
        status.put("TrangThai", 0);

        ApiClient.getApiService().updateBlockStatus(maKhoiLop, status).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CategoryGradeActivity.this, "Xóa thành công", Toast.LENGTH_SHORT).show();
                    loadBlockList();
                } else {
                    Toast.makeText(CategoryGradeActivity.this, "Không thể xóa khối đang có lớp", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(CategoryGradeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
