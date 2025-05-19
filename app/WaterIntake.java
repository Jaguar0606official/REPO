package com.example.lol.models;

import java.util.Date;

public class WaterIntake {
    private int amount; // в мл
    private Date timestamp;

    public WaterIntake(int amount, Date timestamp) {
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // Геттеры и сеттеры
    public int getAmount() {
        return amount;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
