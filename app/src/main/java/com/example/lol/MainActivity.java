package com.example.lol;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.lol.adapters.HistoryAdapter;
import com.example.lol.models.WaterIntake;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private WaterIntakeManager waterIntakeManager;
    private HistoryManager historyManager;
    private NotificationHelper notificationHelper;
    private TextView tvTotal;
    private TextView tvRemaining;
    private ProgressBar progressBar;
    private Button btnAdd250;
    private Button btnAdd500;
    private Button btnHistory;
    private Button btnClearToday;
    private RecyclerView historyRecyclerView;
    private String currentDateKey;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private final BroadcastReceiver dataUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initManagers();
        setupButtons();
        setupReminders();
        checkNotificationPermission();
        checkDayChange();
        updateUI();

        waterIntakeManager.cleanOldData(30);

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setMax(2000);
        progressBar.setProgress(0); // Начинаем с 0

        // Убедимся что прогресс невидим в начале
        LayerDrawable progressDrawable = (LayerDrawable) progressBar.getProgressDrawable();
        ClipDrawable progressClip = (ClipDrawable) progressDrawable.findDrawableByLayerId(android.R.id.progress);
        GradientDrawable progressShape = (GradientDrawable) progressClip.getDrawable();
        progressShape.setColor(Color.TRANSPARENT);


        if (historyRecyclerView != null) {
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                    historyRecyclerView.getContext(),
                    LinearLayoutManager.VERTICAL);
            dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider_item));
            historyRecyclerView.addItemDecoration(dividerItemDecoration);
            historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Сохраняем текущий прогресс
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putInt("water_progress", progressBar.getProgress()).apply();
    }

    private void checkDayChange() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        String today = dateFormat.format(new Date());

        if (currentDateKey == null || !currentDateKey.equals(today)) {
            currentDateKey = today;
//            waterIntakeManager.checkDayReset();
        }
    }

    private void initViews() {
        tvTotal = findViewById(R.id.tv_total);
        tvRemaining = findViewById(R.id.tv_remaining);
        progressBar = findViewById(R.id.progress_bar);
        btnAdd250 = findViewById(R.id.btn_add_250);
        btnAdd500 = findViewById(R.id.btn_add_500);
        btnHistory = findViewById(R.id.btn_history);
        btnClearToday = findViewById(R.id.btn_clear_today);
        historyRecyclerView = findViewById(R.id.history_recycler);

        // Initialize test notification button
        Button testNotificationBtn = findViewById(R.id.test_notification);
        if (testNotificationBtn != null) {
            testNotificationBtn.setOnClickListener(v -> {
                notificationHelper.sendReminderNotification();
            });
        }
    }

    private void initManagers() {
        waterIntakeManager = WaterIntakeManager.getInstance(this);
        historyManager = HistoryManager.getInstance();
        notificationHelper = new NotificationHelper(this);
    }

    private void setupButtons() {
        btnAdd250.setOnClickListener(v -> {
            if (checkWaterLimit()) return;
            waterIntakeManager.addIntake(250);
            updateUI();
            checkGoalAchieved();
        });

        btnAdd500.setOnClickListener(v -> {
            if (checkWaterLimit()) return;
            waterIntakeManager.addIntake(500);
            updateUI();
            checkGoalAchieved();
        });

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
        });

        btnClearToday.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Подтверждение")
                    .setMessage("Очистить сегодняшнюю историю?")
                    .setPositiveButton("Очистить", (dialog, which) -> {
                        waterIntakeManager.clearTodayIntake();
                        updateUI();
                        Toast.makeText(this, "Сегодняшние данные очищены", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
    }

    private boolean checkWaterLimit() {
        if (waterIntakeManager.getTotalIntake() >= 10000) {
            Toast.makeText(this, "Дальнейшее потребление воды не будет учитываться", Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    private void checkGoalAchieved() {
        int total = waterIntakeManager.getTotalIntake();
        if (total >= 2000 && total < 7000) {
            Toast.makeText(this, "Вы достигли дневной нормы потребления воды.", Toast.LENGTH_LONG).show();
        } else if (total >= 7000) {
            Toast.makeText(this, "Немедленно прекратите потреблять воду, это может негативно сказаться на вашем состоянии", Toast.LENGTH_LONG).show();
        }
    }

    private void setupReminders() {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, ReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.HOUR, 2);

            if (alarmManager != null) {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_HOUR * 2,
                        pendingIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI() {
        int total = waterIntakeManager.getTotalIntake();
        int remaining = Math.max(0, waterIntakeManager.getRemainingAmount());

        tvTotal.setText(getString(R.string.total_water, total));
        tvRemaining.setText(getString(R.string.remaining_water, remaining));

        // Обновляем прогресс бар
        updateProgressBar(total);

        updateTextColors(total);
        updateHistoryList();
    }

    private void updateProgressBar(int total) {
        // Определяем цвет
        int color;
        if (total > 7000) {
            color = ContextCompat.getColor(this, R.color.danger_red);
        } else if (total > 2000) {
            color = ContextCompat.getColor(this, R.color.danger_yellow);
        } else {
            color = ContextCompat.getColor(this, R.color.blue);
        }

        // Получаем drawable прогресса
        LayerDrawable progressDrawable = (LayerDrawable) progressBar.getProgressDrawable();
        ClipDrawable progressClip = (ClipDrawable) progressDrawable.findDrawableByLayerId(android.R.id.progress);

        // Устанавливаем цвет только если есть прогресс
        if (total > 0) {
            GradientDrawable progressShape = (GradientDrawable) progressClip.getDrawable();
            progressShape.setColor(color);
        }

        // Анимация ТОЛЬКО до 2000 мл
        if (total <= 2000) {
            ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), total);
            animator.setDuration(500);
            animator.start();
        } else {
            progressBar.setProgress(total); // Без анимации после 2000 мл
        }
    }

    private void updateTextColors(int total) {
        int color;
        if (total > 7000) {
            color = ContextCompat.getColor(this, R.color.danger_red);
        } else if (total > 2000) {
            color = ContextCompat.getColor(this, R.color.warning_burgundy);
        } else {
            color = ContextCompat.getColor(this, android.R.color.black);
        }

        tvTotal.setTextColor(color);
        tvRemaining.setTextColor(color);
    }

    private void updateHistoryList() {
        List<WaterIntake> todayIntakes = waterIntakeManager.getTodayIntakes();
        HistoryAdapter adapter = new HistoryAdapter(todayIntakes);
        if (historyRecyclerView != null) {
            historyRecyclerView.setAdapter(adapter);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        waterIntakeManager.checkDayReset(); // Проверяем смену дня
        updateUI();
        // Для Android 13+ (API level 33) добавляем флаг экспорта
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                    dataUpdateReceiver,
                    new IntentFilter("DATA_UPDATED"),
                    Context.RECEIVER_NOT_EXPORTED // Приёмник не экспортируется
            );
        } else {

        }

        updateUI();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено
            } else {
                Toast.makeText(this,
                        "Для работы напоминаний необходимо разрешение на уведомления",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}