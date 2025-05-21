package com.example.lol;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import com.example.lol.models.DailyHistoryItem;
import com.example.lol.models.WaterIntake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WaterIntakeManager {
    private static volatile WaterIntakeManager instance;
    public static final int DAILY_GOAL = 2000;
    private final List<WaterIntake> intakeHistory;
    private int totalIntake;
    private final SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WaterTrackerPrefs";
    private static final String HISTORY_KEY = "waterIntakeHistory";
    private static final String LAST_DATE_KEY = "lastDate";

    private WaterIntakeManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        intakeHistory = new ArrayList<>();
        loadHistory();
        calculateTodayIntake();
    }

    public static synchronized WaterIntakeManager getInstance(Context context) {
        if (instance == null) {
            synchronized (WaterIntakeManager.class) {
                if (instance == null) {
                    instance = new WaterIntakeManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private void loadHistory() {
        String json = sharedPreferences.getString(HISTORY_KEY, null);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    int amount = jsonObject.getInt("amount");
                    long timestamp = jsonObject.getLong("timestamp");
                    intakeHistory.add(new WaterIntake(amount, new Date(timestamp)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveHistory() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (WaterIntake intake : intakeHistory) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("amount", intake.getAmount());
                jsonObject.put("timestamp", intake.getTimestamp().getTime());
                jsonArray.put(jsonObject);
            }
            sharedPreferences.edit()
                    .putString(HISTORY_KEY, jsonArray.toString())
                    .apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void calculateTodayIntake() {
        totalIntake = 0;
        Date today = getStartOfDay();
        for (WaterIntake intake : intakeHistory) {
            if (intake.getTimestamp().after(today)) {
                totalIntake += intake.getAmount();
            }
        }
    }

    private Date getStartOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
    public void cleanOldData(int daysToKeep) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysToKeep);
        Date threshold = calendar.getTime();

        intakeHistory.removeIf(intake -> intake.getTimestamp().before(threshold));
        saveHistory();
    }

    public void addIntake(int amount) {
        checkDayReset();
        if (!canAddIntake(amount)) return;
        WaterIntake intake = new WaterIntake(amount, new Date());
        intakeHistory.add(intake);
        totalIntake += amount;
        saveHistory();
    }

    public void deleteIntakes(List<WaterIntake> intakesToDelete) {
        intakeHistory.removeAll(intakesToDelete);
        calculateTodayIntake();
        saveHistory();
    }

    public boolean canAddIntake(int amount) {
        return (totalIntake + amount) <= 10000;
    }

    public int getTotalIntake() {
        return totalIntake;
    }

    public int getRemainingAmount() {
        return DAILY_GOAL - totalIntake;
    }

    public List<WaterIntake> getAllIntakes() {
        return new ArrayList<>(intakeHistory);
    }

    public List<WaterIntake> getTodayIntakes() {
        List<WaterIntake> todayIntakes = new ArrayList<>();
        Date today = getStartOfDay();

        for (WaterIntake intake : intakeHistory) {
            if (isSameDay(intake.getTimestamp(), today)) {
                todayIntakes.add(intake);
            }
        }
        return todayIntakes;
    }


    public Map<String, List<WaterIntake>> getDailyHistory() {
        Map<String, List<WaterIntake>> dailyHistory = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

        for (WaterIntake intake : intakeHistory) {
            String dateKey = dateFormat.format(intake.getTimestamp());
            if (!dailyHistory.containsKey(dateKey)) {
                dailyHistory.put(dateKey, new ArrayList<>());
            }
            dailyHistory.get(dateKey).add(intake);
        }

        // Сортируем записи внутри каждого дня (новые сверху)
        for (List<WaterIntake> intakes : dailyHistory.values()) {
            Collections.sort(intakes, (i1, i2) -> i2.getTimestamp().compareTo(i1.getTimestamp()));
        }

        return dailyHistory;
    }

    public List<DailyHistoryItem> getGroupedHistory() {
        Map<String, List<WaterIntake>> dailyHistory = getDailyHistory();
        List<DailyHistoryItem> historyItems = new ArrayList<>();

        // Сортируем даты в обратном порядке (новые сверху)
        List<String> sortedDates = new ArrayList<>(dailyHistory.keySet());
        Collections.sort(sortedDates, Collections.reverseOrder());

        for (String date : sortedDates) {
            List<WaterIntake> intakes = dailyHistory.get(date);
            int dayTotal = 0;
            for (WaterIntake intake : intakes) {
                dayTotal += intake.getAmount();
            }
            historyItems.add(new DailyHistoryItem(date, intakes, dayTotal));
        }

        return historyItems;
    }

    public void clearAllHistory() {
        intakeHistory.clear();
        totalIntake = 0;
        saveHistory();
    }

    public void clearTodayIntake() {
        Date today = getStartOfDay();
        intakeHistory.removeIf(intake -> intake.getTimestamp().after(today));
        calculateTodayIntake();
        saveHistory();
    }
    public void checkDayReset() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        String lastDate = sharedPreferences.getString(LAST_DATE_KEY, "");

        if (!today.equals(lastDate)) {
            // День изменился, сбрасываем дневные данные
            totalIntake = 0;
            sharedPreferences.edit()
                    .putString(LAST_DATE_KEY, today)
                    .apply();
        }
    }


    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }



    public boolean isDailyGoalExceeded() {
        return totalIntake > DAILY_GOAL;
    }

    public boolean isDangerLevelExceeded() {
        return totalIntake > 7000;
    }
}