package com.traffic.service;

import com.traffic.exception.ChallanNotFoundException;
import com.traffic.exception.DuplicateChallanException;
import com.traffic.exception.InvalidVehicleException;
import com.traffic.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class TrafficManagementServiceTest {
    private TrafficManagementService service;
    private final String VALID_CAR_ID = "KA-03-MD-8899";
    private final String VALID_BIKE_ID = "MH-12-QQ-4411";

    @BeforeEach
    public void initializeEnvironment() {
        service = new TrafficManagementService();
        
        // Seed typical baseline valid registry data definitions
        Vehicle primaryCar = new Vehicle(VALID_CAR_ID, "Alice Smith", VehicleType.CAR);
        Vehicle secondaryBike = new Vehicle(VALID_BIKE_ID, "Bob Jones", VehicleType.BIKE);
        
        service.registerVehicle(primaryCar);
        service.registerVehicle(secondaryBike);
    }

    // ==========================================
    // POSITIVE SCENARIO TEST CASES
    // ==========================================

    @Test
    public void testRegisterVehicle_Positive_ShouldAddToRegistry() {
        Vehicle newTruck = new Vehicle("DL-01-A-0001", "Charlie Brown", VehicleType.TRUCK);
        assertDoesNotThrow(() -> service.registerVehicle(newTruck));
        assertTrue(service.getVehicleRegistry().containsKey("DL-01-A-0001"));
    }

    @Test
    public void testGenerateChallan_Positive_SignalViolation() {
        Violation redLightRun = new Violation("SIGNAL_VIOLATION", "Broadway Ave", LocalDateTime.now(), 0, 0);
        Challan ticket = service.generateChallan(VALID_CAR_ID, redLightRun);

        assertNotNull(ticket);
        assertEquals(1500.0, ticket.getFineAmount());
        assertFalse(ticket.isPaid());
    }

    @Test
    public void testProcessChallanPayment_Positive_ShouldClearBalance() {
        Violation parkingOffense = new Violation("ILLEGAL_PARKING", "Commercial Hub", LocalDateTime.now(), 0, 0);
        Challan entry = service.generateChallan(VALID_BIKE_ID, parkingOffense);
        
        assertEquals(700.0, service.calculateTotalOutstandingFines(VALID_BIKE_ID));
        
        service.processChallanPayment(entry.getChallanId());
        
        assertTrue(entry.isPaid());
        assertEquals(0.0, service.calculateTotalOutstandingFines(VALID_BIKE_ID));
    }

    @Test
    public void testClassifyVehicleProfile_Positive_ExcellentCitizen() {
        String profileCategory = service.classifyVehicleProfile(VALID_CAR_ID);
        assertEquals("EXCELLENT_CITIZEN", profileCategory);
    }

    // ==========================================
    // NEGATIVE & EXCEPTION TEST CASES
    // ==========================================

    @Test
    public void testRegisterVehicle_Negative_EmptyOrNullDetails() {
        Vehicle nullIdVehicle = new Vehicle("", "No Name", VehicleType.CAR);
        Vehicle nullNameVehicle = new Vehicle("TX-99-XX", "  ", VehicleType.BUS);

        assertThrows(InvalidVehicleException.class, () -> service.registerVehicle(nullIdVehicle));
        assertThrows(InvalidVehicleException.class, () -> service.registerVehicle(nullNameVehicle));
        assertThrows(InvalidVehicleException.class, () -> service.registerVehicle(null));
    }

    @Test
    public void testGenerateChallan_Negative_UnregisteredVehicle() {
        Violation typicalViolation = new Violation("ILLEGAL_PARKING", "Sector 5", LocalDateTime.now(), 0, 0);
        
        assertThrows(InvalidVehicleException.class, () -> 
            service.generateChallan("UNREGISTERED_PLATE_NUM", typicalViolation)
        );
    }

    @Test
    public void testGenerateChallan_Negative_DuplicatePreventionTracking() {
        LocalDateTime timestampMarker = LocalDateTime.of(2026, 9, 10, 10, 30, 0);
        
        Violation initialEvent = new Violation("SIGNAL_VIOLATION", "Crossroad 2", timestampMarker, 0, 0);
        Violation duplicateEvent = new Violation("SIGNAL_VIOLATION", "Crossroad 2", timestampMarker.plusSeconds(15), 0, 0);

        service.generateChallan(VALID_CAR_ID, initialEvent);

        // Security engine blocks subsequent event generation inside the same minute time window
        assertThrows(DuplicateChallanException.class, () -> 
            service.generateChallan(VALID_CAR_ID, duplicateEvent)
        );
    }

    @Test
    public void testProcessChallanPayment_Negative_InvalidChallanCode() {
        assertThrows(ChallanNotFoundException.class, () -> 
            service.processChallanPayment("NON-EXISTENT-ID-LOG")
        );
    }

    // ==========================================
    // BOUNDARY & ESCALATION TEST CASES
    // ==========================================

        @Test
    public void testGenerateChallan_Boundary_OverSpeedingTierCalculations() {
        // Case A: Just under or at threshold (+25 Over Speed) -> Base Tier Fine
        Violation lowerTierSpeed = new Violation("OVER_SPEEDING", "Exp Way", LocalDateTime.now(), 105, 80);
        Challan ticketA = service.generateChallan(VALID_CAR_ID, lowerTierSpeed);
        assertEquals(1200.0, ticketA.getFineAmount());

        // Register a fresh vehicle for Case B to keep the boundary check isolated from repeat penalties
        String FRESH_CAR_ID = "DL-05-AB-9999";
        Vehicle freshCar = new Vehicle(FRESH_CAR_ID, "Charlie Green", VehicleType.CAR);
        service.registerVehicle(freshCar);

        // Case B: Critical delta threshold (+35 Over Speed) -> High Tier Fine (Isolated from repeat penalty)
        Violation extremeTierSpeed = new Violation("OVER_SPEEDING", "Exp Way", LocalDateTime.now().plusHours(1), 115, 80);
        Challan ticketB = service.generateChallan(FRESH_CAR_ID, extremeTierSpeed);
        assertEquals(2500.0, ticketB.getFineAmount());
    }


    @Test
    public void testSystemEscalation_RecidivismAndProfileProgression() {
        LocalDateTime timeClock = LocalDateTime.of(2026, 5, 4, 12, 0);
        
        // Incident 1: Base line fine charge applied
        Violation fine1 = new Violation("ILLEGAL_PARKING", "Zone A", timeClock, 0, 0);
        Challan res1 = service.generateChallan(VALID_BIKE_ID, fine1);
        assertEquals(700.0, res1.getFineAmount()); 

        // Incident 2: Compound penalty scalar adjustment added (+600)
        Violation fine2 = new Violation("ILLEGAL_PARKING", "Zone B", timeClock.plusDays(1), 0, 0);
        Challan res2 = service.generateChallan(VALID_BIKE_ID, fine2);
        assertEquals(1300.0, res2.getFineAmount()); 

        // Check categorization scaling metrics transition
        assertEquals("MODERATE_OFFENDER", service.classifyVehicleProfile(VALID_BIKE_ID));

        // Incident 3: Third offense forces transition into a chronic offender classification
        Violation fine3 = new Violation("ILLEGAL_PARKING", "Zone C", timeClock.plusDays(2), 0, 0);
        service.generateChallan(VALID_BIKE_ID, fine3);

        assertEquals("CHRONIC_RECIDIVIST", service.classifyVehicleProfile(VALID_BIKE_ID));
    }
}
