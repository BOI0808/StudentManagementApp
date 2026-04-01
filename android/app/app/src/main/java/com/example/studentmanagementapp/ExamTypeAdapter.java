package com.example.studentmanagementapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ExamTypeAdapter extends RecyclerView.Adapter<ExamTypeAdapter.ViewHolder> {

    private List<ExamType> examTypes;
    private OnDeleteClickListener deleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    public ExamTypeAdapter(List<ExamType> examTypes, OnDeleteClickListener deleteClickListener) {
        this.examTypes = examTypes;
        this.deleteClickListener = deleteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exam_type, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExamType examType = examTypes.get(position);
        holder.tvSTT.setText(String.valueOf(position + 1));
        holder.tvLoaiKiemTra.setText(examType.getName());
        holder.tvHeSo.setText(String.valueOf(examType.getCoefficient()));
        
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return examTypes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSTT, tvLoaiKiemTra, tvHeSo;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSTT = itemView.findViewById(R.id.tvSTT);
            tvLoaiKiemTra = itemView.findViewById(R.id.tvLoaiKiemTra);
            tvHeSo = itemView.findViewById(R.id.tvHeSo);
            btnDelete = itemView.findViewById(R.id.btnDeleteExamType);
        }
    }
}