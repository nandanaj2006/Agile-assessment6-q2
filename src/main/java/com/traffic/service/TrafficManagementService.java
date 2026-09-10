package com.traffic.service;

import com.traffic.exception.ChallanNotFoundException;
import com.traffic.exception.DuplicateChallanException;
import com.traffic.exception.InvalidVehicleException;
import com.traffic.model.*;

import java.util.*;

public class TrafficManagementService {
    private final Map<String, Vehicle> vehicleRegistry = new HashMap<>();
    private final List<Challan> allChallans = new ArrayList<>();
    private int challanCounter = 5000;

    public void registerVehicle(Vehicle vehicle) {
        if (vehicle == null || vehicle.getVehicleNumber() == null || vehicle.getVehicleNumber().trim().isEmpty()) {
            throw new InvalidVehicleException("Vehicle registration failed: Invalid vehicle identifier information.");
        }
        if (vehicle.getOwnerName() == null || vehicle.getOwnerName().trim().isEmpty()) {
            throw new InvalidVehicleException("Vehicle registration failed: Missing explicit owner details.");
        }
        vehicleRegistry.put(vehicle.getVehicleNumber(), vehicle);
    }

    public Challan generateChallan(String vehicleNumber, Violation violation) {
        if (violation == null) {
            throw new IllegalArgumentException("Violation contextual tracking info cannot be null.");
        }

        Vehicle vehicle = vehicleRegistry.get(vehicleNumber);
        if (vehicle == null) {
            throw new InvalidVehicleException("Operation Denied: Vehicle target entry '" + vehicleNumber + "' is not registered.");
        }

        // Exact Duplication Prevention Protocol (Checks type, target asset, and exact time bucket)
        for (Challan tracking : allChallans) {
            if (tracking.getVehicleNumber().equals(vehicleNumber) &&
                tracking.getViolation().getViolationType().equalsIgnoreCase(violation.getViolationType()) &&
                tracking.getViolation().getTimestamp().withSecond(0).withNano(0)
                        .equals(violation.getTimestamp().withSecond(0).withNano(0))) {
                throw new DuplicateChallanException("Security Alert: Suppressing duplicate challan instance entry generated for the same incident.");
            }
        }

        // Fine Escalation Engine based on Type Severity
        double calculatedBaseFine = 0;
        switch (violation.getViolationType().toUpperCase()) {
            case "OVER_SPEEDING":
                double deltaSpeed = violation.getSpeed() - violation.getPermittedSpeed();
                calculatedBaseFine = (deltaSpeed > 30) ? 2500.0 : 1200.0;
                break;
            case "SIGNAL_VIOLATION":
                calculatedBaseFine = 1500.0;
                break;
            case "ILLEGAL_PARKING":
                calculatedBaseFine = 700.0;
                break;
            default:
                calculatedBaseFine = 400.0;
        }

        // Recidivism Factor Escalation Strategy (Higher penalties for repeat violations)
        long historicIncidents = vehicle.getViolationHistory().stream()
                .filter(c -> c.getViolation().getViolationType().equalsIgnoreCase(violation.getViolationType()))
                .count();

        if (historicIncidents > 0) {
            calculatedBaseFine += (historicIncidents * 600.0);
        }

        String targetChallanId = "E-CHLN-" + (++challanCounter);
        Challan formalChallan = new Challan(targetChallanId, vehicleNumber, violation, calculatedBaseFine);

        allChallans.add(formalChallan);
        vehicle.addViolationToHistory(formalChallan);

        return formalChallan;
    }

    public void processChallanPayment(String challanId) {
        Challan activeRecord = allChallans.stream()
                .filter(c -> c.getChallanId().equalsIgnoreCase(challanId))
                .findFirst()
                .orElseThrow(() -> new ChallanNotFoundException("Transaction Error: Challan identifier code not located inside database logs."));
        
        activeRecord.setPaid(true);
    }

    public double calculateTotalOutstandingFines(String vehicleNumber) {
        if (!vehicleRegistry.containsKey(vehicleNumber)) {
            throw new InvalidVehicleException("Cannot check balance logs: Unregistered profile.");
        }
        return allChallans.stream()
                .filter(c -> c.getVehicleNumber().equals(vehicleNumber) && !c.isPaid())
                .mapToDouble(Challan::getFineAmount)
                .sum();
    }

    public String classifyVehicleProfile(String vehicleNumber) {
        Vehicle target = vehicleRegistry.get(vehicleNumber);
        if (target == null) {
            throw new InvalidVehicleException("Classification error: Profile targeting empty entries.");
        }

        int incidentWeight = target.getViolationHistory().size();
        if (incidentWeight == 0) return "EXCELLENT_CITIZEN";
        if (incidentWeight <= 2) return "MODERATE_OFFENDER";
        return "CHRONIC_RECIDIVIST";
    }

    public Map<String, Vehicle> getVehicleRegistry() { return vehicleRegistry; }
    public List<Challan> getAllChallans() { return allChallans; }
}

