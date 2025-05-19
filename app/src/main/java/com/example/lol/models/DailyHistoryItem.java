package com.example.lol.models;

import java.util.List;

public class DailyHistoryItem {
    private String date;
    private List<WaterIntake> intakes;
    private int dayTotal;

    public DailyHistoryItem(String date, List<WaterIntake> intakes, int dayTotal) {
        this.date = date;
        this.intakes = intakes;
        this.dayTotal = dayTotal;
    }

    // Геттеры
    public String getDate() { return date; }
    public List<WaterIntake> getIntakes() { return intakes; }
    public int getDayTotal() { return dayTotal; }
}