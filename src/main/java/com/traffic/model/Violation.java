package com.traffic.model;

import java.time.LocalDateTime;

public class Violation {
    private String violationType; // "OVER_SPEEDING", "SIGNAL_VIOLATION", "ILLEGAL_PARKING"
    private String location;
    private LocalDateTime timestamp;
    private double speed;
    private double permittedSpeed;

    public Violation(String violationType, String location, LocalDateTime timestamp, double speed, double permittedSpeed) {
        this.violationType = violationType;
        this.location = location;
        this.timestamp = timestamp;
        this.speed = speed;
        this.permittedSpeed = permittedSpeed;
    }

    public String getViolationType() { return violationType; }
    public String getLocation() { return location; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public double getSpeed() { return speed; }
    public double getPermittedSpeed() { return permittedSpeed; }
}
