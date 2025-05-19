package com.example.lol.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.lol.R;
import com.example.lol.models.WaterIntake;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<WaterIntake> intakes;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public HistoryAdapter(List<WaterIntake> intakes) {
        this.intakes = intakes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaterIntake intake = intakes.get(position);
        holder.tvAmount.setText(String.format("%d мл", intake.getAmount()));
        holder.tvTime.setText(timeFormat.format(intake.getTimestamp()));

        // Устанавливаем цвет в зависимости от количества
        int color;
        if (intake.getAmount() >= 500) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.blue);
        } else {
            color = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black);
        }
        holder.tvAmount.setTextColor(color);
    }


    @Override
    public int getItemCount() {
        return intakes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount;
        TextView tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }
}