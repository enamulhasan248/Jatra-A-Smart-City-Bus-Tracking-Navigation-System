package com.example.jatradriver;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class BusDriverInfo {

    // Common field
    private String driverName;

    // Public bus fields
    private String busNumber;
    private String busName;

    // Private bus fields
    private String from;
    private String to;
    private String mobile;

    public BusDriverInfo() {}

    // Constructor for Public Bus
    public BusDriverInfo(String driverName, String busName) {
        this.driverName = driverName;
        this.busName = busName;
    }

    // Constructor for Private Bus
    public BusDriverInfo(String driverName, String from, String to, String mobile) {
        this.driverName = driverName;
        this.from = from;
        this.to = to;
        this.mobile = mobile;
    }

    // ---- Getters and Setters ----

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    // Public bus
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }

    public String getBusName() { return busName; }
    public void setBusName(String busName) { this.busName = busName; }

    // Private bus
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
}
