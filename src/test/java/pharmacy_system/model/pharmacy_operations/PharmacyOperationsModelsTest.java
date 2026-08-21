package pharmacy_system.model.pharmacy_operations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for pharmacy operations models.
 * 
 * Covers:
 * - InventoryItem: expired batches contribute 0 to available quantity
 * - InventoryItem: deduction guards (canDeduct, deduct)
 * - InventoryItem: adjustment prevents negative balance
 * - Medicine: name validation (1..255 chars), required fields
 * - DispenseRecord: state transitions (PENDING/VERIFIED/DISPENSED/FAILED)
 * - DispenseRecord: patient verification
 * - StockMovement: reason mandatory for ADJUSTMENT types
 * 
 * Requirements: 8.5, 9.1, 9.3, 9.4, 9.5
 */
class PharmacyOperationsModelsTest {

    // ========================================================================
    // InventoryItem Tests
    // ========================================================================

    @Test
    void inventoryItem_expiredBatch_availableQuantityIsZero() {
        // Requirement 8.5: expired stock excluded from eligible stock
        LocalDate yesterday = LocalDate.now().minusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", yesterday, 50);

        assertTrue(batch.isExpired(), "Batch should be expired");
        assertEquals(0, batch.getAvailableQuantity(), "Expired batch should contribute 0 to available quantity");
    }

    @Test
    void inventoryItem_expiredBatch_recordRetained() {
        // Requirement 9.4: expired batches displayed but marked non-dispensable
        LocalDate yesterday = LocalDate.now().minusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", yesterday, 50);

        assertTrue(batch.isExpired());
        assertEquals(50, batch.getQuantityOnHand(), "Physical balance retained even when expired");
    }

    @Test
    void inventoryItem_nonExpiredBatch_availableQuantityEqualsBalance() {
        // Requirement 8.5: non-expired stock is available
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        assertFalse(batch.isExpired());
        assertEquals(50, batch.getAvailableQuantity());
    }

    @Test
    void inventoryItem_canDeduct_sufficientStock_returnsTrue() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        assertTrue(batch.canDeduct(30), "Should be able to deduct when sufficient stock");
        assertTrue(batch.canDeduct(50), "Should be able to deduct exactly the balance");
    }

    @Test
    void inventoryItem_canDeduct_insufficientStock_returnsFalse() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        assertFalse(batch.canDeduct(51), "Should not be able to deduct more than available");
        assertFalse(batch.canDeduct(100), "Should not be able to deduct more than available");
    }

    @Test
    void inventoryItem_canDeduct_negativeQuantity_returnsFalse() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        assertFalse(batch.canDeduct(-10), "Should not allow negative deduction");
    }

    @Test
    void inventoryItem_deduct_validQuantity_reducesBalance() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        batch.deduct(20);

        assertEquals(30, batch.getQuantityOnHand());
    }

    @Test
    void inventoryItem_deduct_exactBalance_reducesToZero() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        batch.deduct(50);

        assertEquals(0, batch.getQuantityOnHand());
    }

    @Test
    void inventoryItem_deduct_insufficientStock_throwsException() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> batch.deduct(51));
        assertTrue(ex.getMessage().contains("Insufficient stock"), "Exception message should indicate insufficient stock");
    }

    @Test
    void inventoryItem_deduct_incrementsVersion() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50, 0, 0L, LocalDateTime.now(), LocalDateTime.now());
        long initialVersion = batch.getVersion();

        batch.deduct(10);

        assertEquals(initialVersion + 1, batch.getVersion());
    }

    @Test
    void inventoryItem_canAdjust_positiveAdjustment_sufficient() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        assertTrue(batch.canAdjust(10), "Should allow positive adjustment");
    }

    @Test
    void inventoryItem_canAdjust_negativeAdjustment_sufficient() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        assertTrue(batch.canAdjust(-20), "Should allow negative adjustment that keeps balance >= 0");
    }

    @Test
    void inventoryItem_canAdjust_negativeAdjustment_wouldGoNegative() {
        // Requirement 9.5: no negative balance
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        assertFalse(batch.canAdjust(-51), "Should not allow adjustment that would take balance below 0");
    }

    @Test
    void inventoryItem_adjust_validNegativeAdjustment_reducesBalance() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        batch.adjust(-20);

        assertEquals(30, batch.getQuantityOnHand());
    }

    @Test
    void inventoryItem_adjust_validPositiveAdjustment_increasesBalance() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        batch.adjust(10);

        assertEquals(60, batch.getQuantityOnHand());
    }

    @Test
    void inventoryItem_adjust_invalidNegativeAdjustment_throwsException() {
        // Requirement 9.5: adjustment prevents negative balance
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> batch.adjust(-51));
        assertTrue(ex.getMessage().contains("below zero"), "Exception should indicate balance would go negative");
    }

    @Test
    void inventoryItem_adjust_incrementsVersion() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50, 0, 0L, LocalDateTime.now(), LocalDateTime.now());
        long initialVersion = batch.getVersion();

        batch.adjust(-10);

        assertEquals(initialVersion + 1, batch.getVersion());
    }

    @Test
    void inventoryItem_receive_validQuantity_increasesBalance() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        batch.receive(20);

        assertEquals(70, batch.getQuantityOnHand());
    }

    @Test
    void inventoryItem_receive_zeroQuantity_throwsException() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> batch.receive(0));
        assertTrue(ex.getMessage().contains("greater than 0"), "Should reject zero quantity");
    }

    @Test
    void inventoryItem_receive_negativeQuantity_throwsException() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> batch.receive(-10));
        assertTrue(ex.getMessage().contains("greater than 0"), "Should reject negative quantity");
    }

    @Test
    void inventoryItem_validateStockData_validData() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        List<String> errors = batch.validateStockData(20, LocalDate.now().plusDays(30));

        assertTrue(errors.isEmpty(), "Valid data should produce no errors");
    }

    @Test
    void inventoryItem_validateStockData_zeroQuantity() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        List<String> errors = batch.validateStockData(0, LocalDate.now().plusDays(30));

        assertFalse(errors.isEmpty(), "Zero quantity should be invalid");
        assertTrue(errors.stream().anyMatch(e -> e.contains("greater than 0")), "Should report quantity error");
    }

    @Test
    void inventoryItem_validateStockData_negativeQuantity() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        List<String> errors = batch.validateStockData(-5, LocalDate.now().plusDays(30));

        assertFalse(errors.isEmpty(), "Negative quantity should be invalid");
    }

    @Test
    void inventoryItem_validateStockData_nullExpiryDate() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        List<String> errors = batch.validateStockData(20, null);

        assertFalse(errors.isEmpty(), "Null expiry date should be invalid");
        assertTrue(errors.stream().anyMatch(e -> e.contains("required")), "Should report expiry date required error");
    }

    @Test
    void inventoryItem_validateStockData_bothInvalid() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        InventoryItem batch = new InventoryItem(1L, 100L, "BATCH001", tomorrow, 50);

        List<String> errors = batch.validateStockData(-1, null);

        assertEquals(2, errors.size(), "Both quantity and expiry errors should be reported");
    }

    // ========================================================================
    // Medicine Tests
    // ========================================================================

    @Test
    void medicine_nameValidation_validName() {
        // Requirement 9.1: name validation (1..255 chars)
        Medicine med = new Medicine(1L, "Aspirin");

        List<String> errors = med.validateMedicineInformation();

        assertTrue(errors.isEmpty(), "Valid name should produce no errors");
    }

    @Test
    void medicine_nameValidation_nullName() {
        Medicine med = new Medicine(1L, null);

        List<String> errors = med.validateMedicineInformation();

        assertFalse(errors.isEmpty(), "Null name should be invalid");
        assertTrue(errors.stream().anyMatch(e -> e.contains("required")), "Should report name required error");
    }

    @Test
    void medicine_nameValidation_blankName() {
        Medicine med = new Medicine(1L, "   ");

        List<String> errors = med.validateMedicineInformation();

        assertFalse(errors.isEmpty(), "Blank name should be invalid");
    }

    @Test
    void medicine_nameValidation_emptyName() {
        Medicine med = new Medicine(1L, "");

        List<String> errors = med.validateMedicineInformation();

        assertFalse(errors.isEmpty(), "Empty name should be invalid");
    }

    @Test
    void medicine_nameValidation_maxLength255() {
        // Requirement 9.1: exactly 255 chars is valid
        String name255 = "A".repeat(255);
        Medicine med = new Medicine(1L, name255);

        List<String> errors = med.validateMedicineInformation();

        assertTrue(errors.isEmpty(), "255-char name should be valid");
    }

    @Test
    void medicine_nameValidation_exceeds255() {
        // Requirement 9.1: more than 255 chars is invalid
        String name256 = "A".repeat(256);
        Medicine med = new Medicine(1L, name256);

        List<String> errors = med.validateMedicineInformation();

        assertFalse(errors.isEmpty(), "256-char name should be invalid");
        assertTrue(errors.stream().anyMatch(e -> e.contains("255")), "Should mention 255 character limit");
    }

    @Test
    void medicine_nameValidation_singleChar() {
        // Requirement 9.1: minimum 1 char
        Medicine med = new Medicine(1L, "A");

        List<String> errors = med.validateMedicineInformation();

        assertTrue(errors.isEmpty(), "Single character name should be valid");
    }

    @Test
    void medicine_fullConstructor_withAllFields() {
        // Requirement 9.1: required fields check
        Medicine med = new Medicine(
            1L,
            "ASP001",
            "Aspirin",
            "Acetylsalicylic acid",
            "Tablet",
            "500mg",
            "mg",
            "Pain reliever"
        );

        List<String> errors = med.validateMedicineInformation();
        assertTrue(errors.isEmpty(), "Medicine with all fields should be valid");
        assertEquals("ASP001", med.getMedicineCode());
        assertEquals("Aspirin", med.getMedicineName());
        assertEquals("Acetylsalicylic acid", med.getGenericName());
    }

    @Test
    void medicine_validateRequiredFields_delegatesToValidateMedicineInformation() {
        Medicine med = new Medicine(1L, null);

        List<String> errors = med.validateRequiredFields();

        assertFalse(errors.isEmpty(), "validateRequiredFields should delegate to validateMedicineInformation");
    }

    // ========================================================================
    // StockMovement Tests
    // ========================================================================

    @Test
    void stockMovement_receiptMovement_noReasonRequired() {
        // Requirement 9.3: reason only required for ADJUSTMENT
        StockMovement movement = StockMovement.of(
            StockMovementType.RECEIVE,
            1L,
            10L,
            100L,
            20,
            70,
            null,  // No reason
            999L
        );

        List<String> errors = movement.validateMovement();
        assertTrue(errors.isEmpty(), "RECEIVE movement should not require reason");
    }

    @Test
    void stockMovement_dispenseMovement_noReasonRequired() {
        StockMovement movement = StockMovement.of(
            StockMovementType.DISPENSE,
            2L,
            10L,
            100L,
            -30,
            40,
            null,  // No reason
            999L
        );

        List<String> errors = movement.validateMovement();
        assertTrue(errors.isEmpty(), "DISPENSE movement should not require reason");
    }

    @Test
    void stockMovement_adjustmentMovement_reasonRequired() {
        // Requirement 9.3: reason mandatory for ADJUSTMENT
        StockMovement movement = StockMovement.of(
            StockMovementType.ADJUSTMENT,
            3L,
            10L,
            100L,
            5,
            75,
            "Inventory correction",
            999L
        );

        List<String> errors = movement.validateMovement();
        assertTrue(errors.isEmpty(), "ADJUSTMENT with reason should be valid");
    }

    @Test
    void stockMovement_adjustmentMovement_nullReason_invalid() {
        // Requirement 9.3: reason mandatory for ADJUSTMENT
        assertThrows(
            IllegalArgumentException.class,
            () -> StockMovement.of(
                StockMovementType.ADJUSTMENT,
                3L,
                10L,
                100L,
                5,
                75,
                null,  // No reason
                999L
            ),
            "ADJUSTMENT with null reason should throw exception"
        );
    }

    @Test
    void stockMovement_adjustmentMovement_blankReason_invalid() {
        // Requirement 9.3: reason mandatory for ADJUSTMENT
        assertThrows(
            IllegalArgumentException.class,
            () -> StockMovement.of(
                StockMovementType.ADJUSTMENT,
                3L,
                10L,
                100L,
                5,
                75,
                "   ",  // Blank reason
                999L
            ),
            "ADJUSTMENT with blank reason should throw exception"
        );
    }

    @Test
    void stockMovement_adjustmentMovement_emptyReason_invalid() {
        // Requirement 9.3: reason mandatory for ADJUSTMENT
        assertThrows(
            IllegalArgumentException.class,
            () -> StockMovement.of(
                StockMovementType.ADJUSTMENT,
                3L,
                10L,
                100L,
                5,
                75,
                "",  // Empty reason
                999L
            ),
            "ADJUSTMENT with empty reason should throw exception"
        );
    }

    @Test
    void stockMovement_nullMovementType_invalid() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new StockMovement(
                1L,
                10L,
                100L,
                null,  // null type
                20,
                70,
                null,
                999L,
                LocalDateTime.now()
            ),
            "Null movement type should throw exception"
        );
    }

    @Test
    void stockMovement_isReceipt_methodWorks() {
        StockMovement receipt = StockMovement.of(StockMovementType.RECEIVE, 1L, 10L, 100L, 20, 70, null, 999L);
        StockMovement adjustment = StockMovement.of(StockMovementType.ADJUSTMENT, 2L, 10L, 100L, 5, 75, "Correction", 999L);

        assertTrue(receipt.isReceipt());
        assertFalse(adjustment.isReceipt());
    }

    @Test
    void stockMovement_isAdjustment_methodWorks() {
        StockMovement receipt = StockMovement.of(StockMovementType.RECEIVE, 1L, 10L, 100L, 20, 70, null, 999L);
        StockMovement adjustment = StockMovement.of(StockMovementType.ADJUSTMENT, 2L, 10L, 100L, 5, 75, "Correction", 999L);

        assertFalse(receipt.isAdjustment());
        assertTrue(adjustment.isAdjustment());
    }

    @Test
    void stockMovement_isDispense_methodWorks() {
        StockMovement dispense = StockMovement.of(StockMovementType.DISPENSE, 1L, 10L, 100L, -30, 40, null, 999L);
        StockMovement adjustment = StockMovement.of(StockMovementType.ADJUSTMENT, 2L, 10L, 100L, 5, 75, "Correction", 999L);

        assertTrue(dispense.isDispense());
        assertFalse(adjustment.isDispense());
    }

    // ========================================================================
    // DispenseRecord State Transitions Tests
    // ========================================================================

    @Test
    void dispenseRecord_initialState_isPending() {
        // Requirement 8.6: state machine
        DispenseRecord record = createTestDispenseRecord();

        assertEquals(DispenseRecord.STATUS_PENDING, record.getStatus());
    }

    @Test
    void dispenseRecord_patientVerification_successTransitionsToVerified() {
        // Requirement 8.6: patient verification
        DispenseRecord record = createTestDispenseRecord();

        boolean result = record.verifyPatient(300L);

        assertTrue(result);
        assertEquals(DispenseRecord.STATUS_VERIFIED, record.getStatus());
    }

    @Test
    void dispenseRecord_patientVerification_failureTransitionsToFailed() {
        DispenseRecord record = createTestDispenseRecord();

        boolean result = record.verifyPatient(999L);  // Wrong patient

        assertFalse(result);
        assertEquals(DispenseRecord.STATUS_FAILED, record.getStatus());
        assertNotNull(record.getFailureReason());
    }

    @Test
    void dispenseRecord_confirmDispensing_transitionsFromVerified() {
        DispenseRecord record = createTestDispenseRecord();
        record.verifyPatient(300L);

        record.confirmDispensing(Map.of(1L, 10, 2L, 5));

        assertEquals(DispenseRecord.STATUS_VERIFIED, record.getStatus());
        assertEquals(Map.of(1L, 10, 2L, 5), record.getDispensedQuantities());
    }

    @Test
    void dispenseRecord_completeFulfilment_transitionsToDispensed() {
        // Requirement 8.4: DISPENSED state
        DispenseRecord record = createTestDispenseRecord();
        record.verifyPatient(300L);
        record.confirmDispensing(Map.of(1L, 10, 2L, 5));

        record.completeFulfilment(400L);

        assertEquals(DispenseRecord.STATUS_DISPENSED, record.getStatus());
        assertTrue(record.isCompleted());
    }

    @Test
    void dispenseRecord_markFailed_transitionsToFailed() {
        DispenseRecord record = createTestDispenseRecord();

        record.markFailed("Insufficient stock");

        assertEquals(DispenseRecord.STATUS_FAILED, record.getStatus());
        assertEquals("Insufficient stock", record.getFailureReason());
    }

    @Test
    void dispenseRecord_verifyPatient_incrementsVersion() {
        DispenseRecord record = createTestDispenseRecord();
        long initialVersion = record.getVersion();

        record.verifyPatient(300L);

        assertEquals(initialVersion + 1, record.getVersion());
    }

    @Test
    void dispenseRecord_confirmDispensing_incrementsVersion() {
        DispenseRecord record = createTestDispenseRecord();
        record.verifyPatient(300L);
        long versionAfterVerify = record.getVersion();

        record.confirmDispensing(Map.of(1L, 10, 2L, 5));

        assertEquals(versionAfterVerify + 1, record.getVersion());
    }

    @Test
    void dispenseRecord_completeFulfilment_incrementsVersion() {
        DispenseRecord record = createTestDispenseRecord();
        record.verifyPatient(300L);
        record.confirmDispensing(Map.of(1L, 10, 2L, 5));
        long versionAfterConfirm = record.getVersion();

        record.completeFulfilment(400L);

        assertEquals(versionAfterConfirm + 1, record.getVersion());
    }

    @Test
    void dispenseRecord_noPartialDispensing() {
        // Requirement 8.2: full quantity only
        DispenseRecord record = createTestDispenseRecord();
        record.verifyPatient(300L);

        // Attempt to confirm with different quantities
        Map<Long, Integer> partialQuantities = Map.of(1L, 10, 2L, 3);  // 5 required, 3 confirmed

        assertThrows(IllegalArgumentException.class, () -> record.confirmDispensing(partialQuantities));
    }

    @Test
    void dispenseRecord_patientVerificationMismatch() {
        DispenseRecord record = createTestDispenseRecord();
        
        boolean result = record.verifyPatient(999L);
        
        assertFalse(result);
        assertEquals(DispenseRecord.STATUS_FAILED, record.getStatus());
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private DispenseRecord createTestDispenseRecord() {
        Map<Long, Integer> requiredQuantities = Map.of(1L, 10, 2L, 5);
        return new DispenseRecord(100L, 200L, 300L, requiredQuantities, LocalDateTime.now());
    }
}
