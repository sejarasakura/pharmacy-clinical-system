package pharmacy_system.model.patient_information;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import pharmacy_system.model.clinical_prescription.Prescription;
import pharmacy_system.model.clinical_prescription.PrescriptionStatus;
import pharmacy_system.model.pharmacy_operations.DispenseRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PrescriptionStatusSummary} read model.
 *
 * <p>Tests the factory methods, clinical/fulfilment status derivation,
 * read-only invariants, and expiry handling.</p>
 *
 * <p>Validates: Requirements 6.1, 6.2, 6.4</p>
 */
class PrescriptionStatusSummaryTest {

    private Prescription testPrescription;
    private DispenseRecord testDispenseRecord;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.of(2024, 1, 15);
        
        // Create a test Prescription with ISSUED status, issued 10 days ago
        LocalDateTime issuedAt = LocalDateTime.of(2024, 1, 5, 10, 30);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 10, 14, 45);
        
        // Using reflection or a test double - since Prescription constructor is complex,
        // we'll create a minimal test implementation for our purposes
        testPrescription = new TestPrescription(
            100L, 200L, 300L, PrescriptionStatus.ISSUED, issuedAt, updatedAt);
        
        // Create a test DispenseRecord with VERIFIED status
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 12, 9, 0);
        LocalDateTime dispenseUpdatedAt = LocalDateTime.of(2024, 1, 14, 11, 30);
        
        testDispenseRecord = new TestDispenseRecord(
            500L, 100L, 200L, "VERIFIED", dispenseUpdatedAt);
    }

    @Test
    @DisplayName("buildFromPrescription creates summary without fulfilment record")
    void buildFromPrescription_createsSummaryWithoutFulfilmentRecord() {
        PrescriptionStatusSummary summary = PrescriptionStatusSummary.buildFromPrescription(testPrescription, today);
        
        assertEquals(100L, summary.getPrescriptionId());
        assertEquals(200L, summary.getPatientId());
        assertEquals(300L, summary.getDoctorId());
        assertEquals(testPrescription.getIssuedAt(), summary.getPrescribedAt());
        assertEquals(testPrescription.getUpdatedAt(), summary.getLastUpdatedAt());
        assertEquals("ISSUED", summary.getClinicalStatus());
        assertEquals(PrescriptionStatusSummary.FULFILMENT_NOT_STARTED, summary.getFulfilmentStatus());
        assertFalse(summary.hasFulfilmentRecord());
        assertFalse(summary.isCancelled());
        assertFalse(summary.isExpired());
        assertFalse(summary.isDispensed());
    }

    @Test
    @DisplayName("buildFromPrescription with null prescription throws exception")
    void buildFromPrescription_nullPrescription_throwsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PrescriptionStatusSummary.buildFromPrescription(null, today)
        );
        assertEquals("prescription must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("combineClinicalAndFulfilmentStatus creates summary with fulfilment record")
    void combineClinicalAndFulfilmentStatus_createsSummaryWithFulfilmentRecord() {
        PrescriptionStatusSummary summary = PrescriptionStatusSummary.combineClinicalAndFulfilmentStatus(
            testPrescription, testDispenseRecord, today);
        
        assertEquals(100L, summary.getPrescriptionId());
        assertEquals(200L, summary.getPatientId());
        assertEquals(300L, summary.getDoctorId());
        assertEquals(testPrescription.getIssuedAt(), summary.getPrescribedAt());
        // Should be latest of prescription updatedAt and dispenseRecord updatedAt
        assertEquals(testDispenseRecord.getUpdatedAt(), summary.getLastUpdatedAt());
        assertEquals("ISSUED", summary.getClinicalStatus());
        assertEquals("VERIFIED", summary.getFulfilmentStatus());
        assertTrue(summary.hasFulfilmentRecord());
        assertFalse(summary.isCancelled());
        assertFalse(summary.isExpired());
        assertFalse(summary.isDispensed());
    }

    @Test
    @DisplayName("combineClinicalAndFulfilmentStatus with DISPENSED record marks as dispensed")
    void combineClinicalAndFulfilmentStatus_withDispensedRecord_marksAsDispensed() {
        DispenseRecord dispensedRecord = new TestDispenseRecord(
            500L, 100L, 200L, "DISPENSED", testDispenseRecord.getUpdatedAt());
        
        PrescriptionStatusSummary summary = PrescriptionStatusSummary.combineClinicalAndFulfilmentStatus(
            testPrescription, dispensedRecord, today);
        
        assertEquals("DISPENSED", summary.getFulfilmentStatus());
        assertTrue(summary.isDispensed());
    }

    @Test
    @DisplayName("combineClinicalAndFulfilmentStatus with null prescription throws exception")
    void combineClinicalAndFulfilmentStatus_nullPrescription_throwsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PrescriptionStatusSummary.combineClinicalAndFulfilmentStatus(null, testDispenseRecord, today)
        );
        assertEquals("prescription must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("combineClinicalAndFulfilmentStatus with null dispenseRecord throws exception")
    void combineClinicalAndFulfilmentStatus_nullDispenseRecord_throwsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PrescriptionStatusSummary.combineClinicalAndFulfilmentStatus(testPrescription, null, today)
        );
        assertEquals("dispenseRecord must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("from factory method uses dispenseRecord when available")
    void from_withDispenseRecord_usesCombineMethod() {
        PrescriptionStatusSummary summary = PrescriptionStatusSummary.from(
            testPrescription, testDispenseRecord, today);
        
        assertTrue(summary.hasFulfilmentRecord());
        assertEquals("VERIFIED", summary.getFulfilmentStatus());
    }

    @Test
    @DisplayName("from factory method builds from prescription only when dispenseRecord is null")
    void from_withoutDispenseRecord_usesBuildFromPrescription() {
        PrescriptionStatusSummary summary = PrescriptionStatusSummary.from(
            testPrescription, null, today);
        
        assertFalse(summary.hasFulfilmentRecord());
        assertEquals(PrescriptionStatusSummary.FULFILMENT_NOT_STARTED, summary.getFulfilmentStatus());
    }

    @Test
    @DisplayName("clinical status shows EXPIRED when prescription is expired")
    void clinicalStatus_showsExpiredWhenPrescriptionExpired() {
        // Create a prescription issued 2 months ago (expired)
        LocalDateTime issuedAt = LocalDateTime.of(2023, 11, 1, 10, 30);
        Prescription expiredPrescription = new TestPrescription(
            100L, 200L, 300L, PrescriptionStatus.ISSUED, issuedAt, LocalDateTime.now());
        
        PrescriptionStatusSummary summary = PrescriptionStatusSummary.buildFromPrescription(
            expiredPrescription, today);
        
        assertEquals(PrescriptionStatusSummary.CLINICAL_STATUS_EXPIRED, summary.getClinicalStatus());
        assertTrue(summary.isExpired());
    }

    @Test
    @DisplayName("clinical status shows CANCELLED when prescription is cancelled")
    void clinicalStatus_showsCancelledWhenPrescriptionCancelled() {
        Prescription cancelledPrescription = new TestPrescription(
            100L, 200L, 300L, PrescriptionStatus.CANCELLED, 
            LocalDateTime.now().minusDays(5), LocalDateTime.now());
        
        PrescriptionStatusSummary summary = PrescriptionStatusSummary.buildFromPrescription(
            cancelledPrescription, today);
        
        assertEquals("CANCELLED", summary.getClinicalStatus());
        assertTrue(summary.isCancelled());
    }

    @Test
    @DisplayName("lastUpdatedAt uses latest of prescription and dispenseRecord updated times")
    void lastUpdatedAt_usesLatestOfPrescriptionAndDispenseRecord() {
        // Prescription updated later than dispense record
        LocalDateTime prescriptionLater = LocalDateTime.of(2024, 1, 15, 10, 0);
        Prescription laterPrescription = new TestPrescription(
            100L, 200L, 300L, PrescriptionStatus.ISSUED, 
            LocalDateTime.of(2024, 1, 5, 10, 30), prescriptionLater);
        
        LocalDateTime dispenseEarlier = LocalDateTime.of(2024, 1, 10, 10, 0);
        DispenseRecord earlierRecord = new TestDispenseRecord(
            500L, 100L, 200L, "VERIFIED", dispenseEarlier);
        
        PrescriptionStatusSummary summary = PrescriptionStatusSummary.combineClinicalAndFulfilmentStatus(
            laterPrescription, earlierRecord, today);
        
        assertEquals(prescriptionLater, summary.getLastUpdatedAt());
    }

    @Test
    @DisplayName("summary is immutable - fields are read-only")
    void summary_isImmutable_readOnlyFields() {
        PrescriptionStatusSummary summary = PrescriptionStatusSummary.buildFromPrescription(testPrescription, today);
        
        // Verify all getters return expected values
        assertNotNull(summary.getPrescriptionId());
        assertNotNull(summary.getPatientId());
        assertNotNull(summary.getDoctorId());
        assertNotNull(summary.getPrescribedAt());
        assertNotNull(summary.getLastUpdatedAt());
        assertNotNull(summary.getClinicalStatus());
        assertNotNull(summary.getFulfilmentStatus());
        
        // Verify boolean helpers work
        assertFalse(summary.isCancelled());
        assertFalse(summary.isExpired());
        assertFalse(summary.isDispensed());
    }

    @Test
    @DisplayName("constants are correctly defined")
    void constants_areCorrectlyDefined() {
        assertEquals("NOT_STARTED", PrescriptionStatusSummary.FULFILMENT_NOT_STARTED);
        assertEquals("EXPIRED", PrescriptionStatusSummary.CLINICAL_STATUS_EXPIRED);
    }

    // Test implementations for Prescription and DispenseRecord
    private static class TestPrescription extends Prescription {
        private final long prescriptionId;
        private final PrescriptionStatus status;
        private final LocalDateTime issuedAt;
        private final LocalDateTime updatedAt;
        
        TestPrescription(long prescriptionId, long patientId, long doctorId, 
                        PrescriptionStatus status, LocalDateTime issuedAt, LocalDateTime updatedAt) {
            super(patientId, doctorId, java.util.Collections.emptyList());
            this.prescriptionId = prescriptionId;
            this.status = status;
            this.issuedAt = issuedAt;
            this.updatedAt = updatedAt;
        }
        
        @Override
        public long getPrescriptionId() { return prescriptionId; }
        
        @Override
        public long getPatientId() { return 200L; }
        
        @Override
        public long getDoctorId() { return 300L; }
        
        @Override
        public PrescriptionStatus getStatus() { return status; }
        
        @Override
        public LocalDateTime getIssuedAt() { return issuedAt; }
        
        @Override
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        
        @Override
        public boolean isExpired(LocalDate today) {
            // Simple expiry logic: if issued more than 1 month before today
            if (issuedAt == null) return false;
            return today.isAfter(issuedAt.toLocalDate().plusMonths(1));
        }
    }
    
    private static class TestDispenseRecord extends DispenseRecord {
        private final long dispenseId;
        private final long prescriptionId;
        private final long patientId;
        private final String status;
        private final LocalDateTime updatedAt;
        
        TestDispenseRecord(long dispenseId, long prescriptionId, long patientId, 
                          String status, LocalDateTime updatedAt) {
            super(dispenseId, prescriptionId, patientId, 
                  java.util.Collections.emptyMap(), LocalDateTime.now());
            this.dispenseId = dispenseId;
            this.prescriptionId = prescriptionId;
            this.patientId = patientId;
            this.status = status;
            this.updatedAt = updatedAt;
        }
        
        @Override
        public long getDispenseId() { return dispenseId; }
        
        @Override
        public long getPrescriptionId() { return prescriptionId; }
        
        @Override
        public long getPatientId() { return patientId; }
        
        @Override
        public String getStatus() { return status; }
        
        @Override
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }
}