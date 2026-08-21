package pharmacy_system.model.pharmacy_operations;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DispenseRecordTest {

    @Test
    void constructor_initializesWithPendingStatus() {
        Map<Long, Integer> requiredQuantities = Map.of(1L, 10, 2L, 5);
        LocalDateTime now = LocalDateTime.now();
        DispenseRecord record = new DispenseRecord(100L, 200L, 300L, requiredQuantities, now);

        assertEquals(100L, record.getDispenseId());
        assertEquals(200L, record.getPrescriptionId());
        assertEquals(300L, record.getPatientId());
        assertEquals(DispenseRecord.STATUS_PENDING, record.getStatus());
        assertEquals(now, record.getCreatedAt());
        assertEquals(now, record.getUpdatedAt());
        assertEquals(requiredQuantities, record.getRequiredQuantities());
        assertTrue(record.getDispensedQuantities().isEmpty());
        assertFalse(record.isCompleted());
        assertEquals(0L, record.getVersion());
    }

    @Test
    void verifyPatient_successfulVerification_transitionsToVerified() {
        DispenseRecord record = createTestRecord();
        
        boolean result = record.verifyPatient(300L);
        
        assertTrue(result);
        assertEquals(DispenseRecord.STATUS_VERIFIED, record.getStatus());
        assertNotNull(record.getVerifiedAt());
        assertNotEquals(record.getCreatedAt(), record.getUpdatedAt());
        assertEquals(1L, record.getVersion());
    }

    @Test
    void verifyPatient_failedVerification_transitionsToFailed() {
        DispenseRecord record = createTestRecord();
        
        boolean result = record.verifyPatient(999L); // Wrong patient ID
        
        assertFalse(result);
        assertEquals(DispenseRecord.STATUS_FAILED, record.getStatus());
        assertEquals("Patient verification mismatch", record.getFailureReason());
        assertNull(record.getVerifiedAt());
        assertNotEquals(record.getCreatedAt(), record.getUpdatedAt());
        assertEquals(1L, record.getVersion());
    }

    @Test
    void confirmDispensing_validQuantities_updatesDispensedQuantities() {
        DispenseRecord record = createTestRecord();
        record.verifyPatient(300L);
        
        Map<Long, Integer> quantities = new HashMap<>();
        quantities.put(1L, 10);
        quantities.put(2L, 5);
        
        record.confirmDispensing(quantities);
        
        assertEquals(quantities, record.getDispensedQuantities());
        assertEquals(DispenseRecord.STATUS_VERIFIED, record.getStatus()); // Still VERIFIED
        assertEquals(2L, record.getVersion()); // Constructor v0, verifyPatient v1, confirmDispensing v2
    }

    @Test
    void confirmDispensing_wrongState_throwsException() {
        DispenseRecord record = createTestRecord();
        // Not verified - still PENDING
        
        Map<Long, Integer> quantities = Map.of(1L, 10, 2L, 5);
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> record.confirmDispensing(quantities));
        assertEquals("Cannot confirm dispensing: record must be in VERIFIED state", exception.getMessage());
    }

    @Test
    void confirmDispensing_missingQuantity_throwsException() {
        DispenseRecord record = createTestRecord();
        record.verifyPatient(300L);
        
        Map<Long, Integer> quantities = Map.of(1L, 10); // Missing medicine 2
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> record.confirmDispensing(quantities));
        assertTrue(exception.getMessage().contains("Missing confirmed quantity for medicine 2"));
    }

    @Test
    void confirmDispensing_quantityMismatch_throwsException() {
        DispenseRecord record = createTestRecord();
        record.verifyPatient(300L);
        
        Map<Long, Integer> quantities = Map.of(1L, 10, 2L, 6); // 6 instead of 5
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> record.confirmDispensing(quantities));
        assertTrue(exception.getMessage().contains("Confirmed quantity 6 for medicine 2 does not match required quantity 5"));
    }

    @Test
    void completeFulfilment_validState_transitionsToDispensed() {
        DispenseRecord record = createTestRecord();
        record.verifyPatient(300L);
        record.confirmDispensing(Map.of(1L, 10, 2L, 5));
        
        record.completeFulfilment(400L); // pharmacistId
        
        assertEquals(DispenseRecord.STATUS_DISPENSED, record.getStatus());
        assertEquals(400L, record.getPharmacistId());
        assertNotNull(record.getDispensedAt());
        assertTrue(record.isCompleted());
        assertEquals(3L, record.getVersion());
    }

    @Test
    void completeFulfilment_notVerified_throwsException() {
        DispenseRecord record = createTestRecord();
        // Still PENDING
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> record.completeFulfilment(400L));
        assertEquals("Cannot complete fulfilment: record must be in VERIFIED state", exception.getMessage());
    }

    @Test
    void completeFulfilment_noConfirmedQuantities_throwsException() {
        DispenseRecord record = createTestRecord();
        record.verifyPatient(300L);
        // No confirmDispensing called
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> record.completeFulfilment(400L));
        assertEquals("Cannot complete fulfilment: dispensing quantities must be confirmed first", exception.getMessage());
    }

    @Test
    void markFailed_setsStatusAndReason() {
        DispenseRecord record = createTestRecord();
        record.verifyPatient(300L);
        
        record.markFailed("Insufficient stock for medicine 1");
        
        assertEquals(DispenseRecord.STATUS_FAILED, record.getStatus());
        assertEquals("Insufficient stock for medicine 1", record.getFailureReason());
        assertEquals(2L, record.getVersion());
    }

    @Test
    void markFailed_nullReason_throwsException() {
        DispenseRecord record = createTestRecord();
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> record.markFailed(null));
        assertEquals("Failure reason must not be null or blank", exception.getMessage());
    }

    @Test
    void markFailed_blankReason_throwsException() {
        DispenseRecord record = createTestRecord();
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> record.markFailed("   "));
        assertEquals("Failure reason must not be null or blank", exception.getMessage());
    }

    @Test
    void getRequiredQuantities_returnsUnmodifiableMap() {
        Map<Long, Integer> original = new HashMap<>();
        original.put(1L, 10);
        DispenseRecord record = new DispenseRecord(100L, 200L, 300L, original, LocalDateTime.now());
        
        Map<Long, Integer> returned = record.getRequiredQuantities();
        
        // Attempt to modify should throw
        assertThrows(UnsupportedOperationException.class, () -> returned.put(2L, 5));
        
        // Original can still be modified
        original.put(2L, 5);
        assertEquals(2, original.size());
        assertEquals(1, returned.size()); // Unmodified
    }

    @Test
    void getDispensedQuantities_returnsUnmodifiableMap() {
        DispenseRecord record = createTestRecord();
        record.verifyPatient(300L);
        record.confirmDispensing(Map.of(1L, 10, 2L, 5));
        
        Map<Long, Integer> returned = record.getDispensedQuantities();
        
        assertThrows(UnsupportedOperationException.class, () -> returned.put(3L, 5));
    }

    @Test
    void constants_areCorrectlyDefined() {
        assertEquals("PENDING", DispenseRecord.STATUS_PENDING);
        assertEquals("VERIFIED", DispenseRecord.STATUS_VERIFIED);
        assertEquals("DISPENSED", DispenseRecord.STATUS_DISPENSED);
        assertEquals("FAILED", DispenseRecord.STATUS_FAILED);
    }

    private DispenseRecord createTestRecord() {
        Map<Long, Integer> requiredQuantities = Map.of(1L, 10, 2L, 5);
        return new DispenseRecord(100L, 200L, 300L, requiredQuantities, LocalDateTime.now());
    }
}