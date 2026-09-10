package com.traffic.model;

import java.util.ArrayList;
import java.util.List;

public class Vehicle {
    private String vehicleNumber;
    private String ownerName;
    private VehicleType vehicleType;
    private List<Challan> violationHistory;

    public Vehicle(String vehicleNumber, String ownerName, VehicleType vehicleType) {
        this.vehicleNumber = vehicleNumber;
        this.ownerName = ownerName;
        this.vehicleType = vehicleType;
        this.violationHistory = new ArrayList<>();
    }

    public String getVehicleNumber() { return vehicleNumber; }
    public String getOwnerName() { return ownerName; }
    public VehicleType getVehicleType() { return vehicleType; }
    public List<Challan> getViolationHistory() { return violationHistory; }
    
    public void addViolationToHistory(Challan challan) { 
        this.violationHistory.add(challan); 
    }
}
