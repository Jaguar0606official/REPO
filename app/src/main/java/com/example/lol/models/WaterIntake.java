package com.example.lol.models;

import java.util.Date;

public class WaterIntake {
    private int amount;
    private Date timestamp;

    public WaterIntake(int amount, Date timestamp) {
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public int getAmount() {
        return amount;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}