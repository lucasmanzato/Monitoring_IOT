package com.example.iot_monitoring;

import java.util.Date;

public class WindData {
    private double speed;
    private double direction;
    private long timestamp;

    public WindData(double speed, double direction) {
        this.speed = speed;
        this.direction = direction;
        this.timestamp = new Date().getTime();
    }

    // Getters
    public double getSpeed() { return speed; }
    public double getDirection() { return direction; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("Velocidade: %.2f km/h, Direção: %.1f°", speed, direction);
    }
}