package com.example.lol;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.lol.adapters.DailyHistoryAdapter;
import com.example.lol.models.DailyHistoryItem;
import com.example.lol.models.WaterIntake;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity
        implements DailyHistoryAdapter.OnItemClickListener {

    private WaterIntakeManager waterIntakeManager;
    private DailyHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("История потребления");
        }

        waterIntakeManager = WaterIntakeManager.getInstance(this);
        setupRecyclerView();
        setupButtons();
    }

    private void setupRecyclerView() {
        RecyclerView historyRecyclerView = findViewById(R.id.history_recycler);
        adapter = new DailyHistoryAdapter(waterIntakeManager.getGroupedHistory(), this);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                historyRecyclerView.getContext(),
                LinearLayoutManager.VERTICAL);
        historyRecyclerView.addItemDecoration(dividerItemDecoration);
        historyRecyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        Button btnClearAll = findViewById(R.id.btn_clear_all);
        btnClearAll.setOnClickListener(v -> showClearAllConfirmation());
    }

    private void showClearAllConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Очистка всей истории")
                .setMessage("Вы уверены, что хотите удалить ВСЮ историю? Это действие нельзя отменить.")
                .setPositiveButton("Очистить", (dialog, which) -> {
                    waterIntakeManager.clearAllHistory();
                    adapter.updateData(new ArrayList<>());
                    notifyDataChanged();
                    Toast.makeText(this, "Вся история удалена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onDeleteClick(int position) {
        DailyHistoryItem item = adapter.historyItems.get(position);
        showDeleteConfirmation(item.getDate(), item.getIntakes());
    }

    private void showDeleteConfirmation(String date, List<WaterIntake> intakes) {
        new AlertDialog.Builder(this)
                .setTitle("Удаление записей")
                .setMessage("Удалить " + intakes.size() + " записей за " + formatDisplayDate(date) + "?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    waterIntakeManager.deleteIntakes(intakes);
                    adapter.updateData(waterIntakeManager.getGroupedHistory());
                    notifyDataChanged();
                    Toast.makeText(this, "Записи удалены", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private String formatDisplayDate(String storageDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(storageDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return storageDate;
        }
    }

    private void notifyDataChanged() {
        setResult(RESULT_OK);
        sendBroadcast(new Intent("DATA_UPDATED"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}