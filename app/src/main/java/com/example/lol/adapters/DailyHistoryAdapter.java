package com.example.lol.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.lol.R;
import com.example.lol.models.DailyHistoryItem;
import com.example.lol.models.WaterIntake;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyHistoryAdapter extends RecyclerView.Adapter<DailyHistoryAdapter.ViewHolder> {
    public List<DailyHistoryItem> historyItems;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(int position);
    }

    public DailyHistoryAdapter(List<DailyHistoryItem> historyItems, OnItemClickListener listener) {
        this.historyItems = historyItems;
        this.listener = listener;
    }

    public void updateData(List<DailyHistoryItem> newItems) {
        this.historyItems = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyHistoryItem item = historyItems.get(position);

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            var date = inputFormat.parse(item.getDate());
            holder.tvDate.setText(outputFormat.format(date));
        } catch (ParseException e) {
            holder.tvDate.setText(item.getDate());
        }

        holder.tvDayTotal.setText(String.format("Всего: %d мл", item.getDayTotal()));

        HistoryAdapter adapter = new HistoryAdapter(item.getIntakes());
        holder.recyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerView.setAdapter(adapter);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvDayTotal;
        RecyclerView recyclerView;
        Button btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDayTotal = itemView.findViewById(R.id.tv_day_total);
            recyclerView = itemView.findViewById(R.id.daily_intakes_recycler);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}