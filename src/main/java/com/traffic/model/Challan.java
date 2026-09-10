package com.traffic.model;

public class Challan {
    private String challanId;
    private String vehicleNumber;
    private Violation violation;
    private double fineAmount;
    private boolean isPaid;

    public Challan(String challanId, String vehicleNumber, Violation violation, double fineAmount) {
        this.challanId = challanId;
        this.vehicleNumber = vehicleNumber;
        this.violation = violation;
        this.fineAmount = fineAmount;
        this.isPaid = false;
    }

    public String getChallanId() { return challanId; }
    public String getVehicleNumber() { return vehicleNumber; }
    public Violation getViolation() { return violation; }
    public double getFineAmount() { return fineAmount; }
    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }
}
