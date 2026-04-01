package com.example.studentmanagementapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MonHocAdapter extends RecyclerView.Adapter<MonHocAdapter.MonHocViewHolder> {

    private List<MonHoc> monHocList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public MonHocAdapter(List<MonHoc> monHocList, OnItemClickListener listener) {
        this.monHocList = monHocList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MonHocViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Lưu ý: Nếu bạn đặt tên file XML là item_danh_muc.xml thì sửa lại chữ R.layout.danh_muc nhé
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_danh_muc, parent, false);
        return new MonHocViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MonHocViewHolder holder, int position) {
        MonHoc monHoc = monHocList.get(position);

        // 1. Gắn Số Thứ Tự (STT) tự động tăng dựa vào vị trí của dòng (position bắt đầu từ 0)
        holder.tvSTT.setText(String.valueOf(position + 1));

        // 2. Gắn Mã môn và Tên môn
        holder.tvMa.setText(monHoc.getMaMon());
        holder.tvTen.setText(monHoc.getTenMon());
    }

    @Override
    public int getItemCount() {
        if (monHocList != null) {
            return monHocList.size();
        }
        return 0;
    }

    public class MonHocViewHolder extends RecyclerView.ViewHolder {
        // Khai báo 3 TextView khớp với XML mới
        TextView tvSTT, tvMa, tvTen;

        public MonHocViewHolder(@NonNull View itemView) {
            super(itemView);

            // Ánh xạ đúng ID bạn vừa đặt
            tvSTT = itemView.findViewById(R.id.tvSTT);
            tvMa = itemView.findViewById(R.id.tvMa);
            tvTen = itemView.findViewById(R.id.tvTen);

            // Xử lý sự kiện khi bấm vào dòng
            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getAdapterPosition());
                }
            });
        }
    }
}