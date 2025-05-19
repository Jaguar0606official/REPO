package com.example.lol;

import com.example.lol.models.WaterIntake;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryManager {
    private static HistoryManager instance;
    private final Map<String, List<WaterIntake>> weeklyHistory;

    private HistoryManager() {
        weeklyHistory = new HashMap<>();
    }

    public static synchronized HistoryManager getInstance() {
        if (instance == null) {
            instance = new HistoryManager();
        }
        return instance;
    }

    public void addIntakeToHistory(WaterIntake intake) {
        String dateKey = getDateKey(intake.getTimestamp());
        if (!weeklyHistory.containsKey(dateKey)) {
            weeklyHistory.put(dateKey, new ArrayList<>());
        }
        weeklyHistory.get(dateKey).add(intake);
    }

    public Map<String, Integer> getWeeklySummary() {
        Map<String, Integer> summary = new HashMap<>();
        for (Map.Entry<String, List<WaterIntake>> entry : weeklyHistory.entrySet()) {
            int total = 0;
            for (WaterIntake intake : entry.getValue()) {
                total += intake.getAmount();
            }
            summary.put(entry.getKey(), total);
        }
        return summary;
    }

    private String getDateKey(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR) + "/" +
                (calendar.get(Calendar.MONTH) + 1) + "/" +
                calendar.get(Calendar.DAY_OF_MONTH);
    }
}