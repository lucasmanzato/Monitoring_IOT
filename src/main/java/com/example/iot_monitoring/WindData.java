package com.example.iot_monitoring;

import java.util.Date;

public class WindData {
    private double speed;
    private double direction;
    private long timestamp;

    public WindData() {
        this.timestamp = new Date().getTime();
    }

    public WindData(double speed, double direction) {
        this.speed = speed;
        this.direction = direction;
        this.timestamp = new Date().getTime();
    }

    // Getters e Setters
    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getDirection() {
        return direction;
    }

    public void setDirection(double direction) {
        this.direction = direction;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("WindData{speed=%.2f, direction=%.2f, timestamp=%d}",
                speed, direction, timestamp);
    }
}