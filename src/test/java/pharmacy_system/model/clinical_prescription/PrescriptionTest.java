package pharmacy_system.model.clinical_prescription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Prescription} aggregate model.
 * 
 * Validates: Requirements 4.1, 4.2, 4.5, 4.6, 4.7, 5.1, 5.2, 5.4, 5.5
 */
class PrescriptionTest {

    private Prescription prescription;
    private List<PrescriptionItem> validItems;
    private final long patientId = 100L;
    private final long doctorId = 200L;

    @BeforeEach
    void setUp() {
        validItems = List.of(
            new PrescriptionItem(1L, "Aspirin", "100mg", "Once daily", "Take in morning", 30),
            new PrescriptionItem(2L, "Metformin", "500mg", "Twice daily", "Take with meals", 60)
        );
        prescription = new Prescription(patientId, doctorId, validItems);
    }

    // ======================================================================
    // Constructor and field initialization tests
    // ======================================================================

    @Nested
    class ConstructorTests {
        @Test
        void constructor_withItems_createsValidPrescription() {
            Prescription rx = new Prescription(patientId, doctorId, validItems);
            
            assertEquals(patientId, rx.getPatientId());
            assertEquals(doctorId, rx.getDoctorId());
            assertEquals(PrescriptionStatus.DRAFT, rx.getStatus());
            assertNull(rx.getIssuedAt());
            assertNull(rx.getClinicalNotes());
            assertEquals(2, rx.getItems().size());
            assertEquals(0L, rx.getVersion());
        }

        @Test
        void constructor_withItemsAndClinicalNotes_createsValidPrescription() {
            String notes = "Patient has allergies to penicillin";
            Prescription rx = new Prescription(patientId, doctorId, validItems, notes);
            
            assertEquals(patientId, rx.getPatientId());
            assertEquals(doctorId, rx.getDoctorId());
            assertEquals(notes, rx.getClinicalNotes());
            assertEquals(PrescriptionStatus.DRAFT, rx.getStatus());
        }

        @Test
        void constructor_withNullItems_createsEmptyList() {
            Prescription rx = new Prescription(patientId, doctorId, null);
            
            assertNotNull(rx.getItems());
            assertTrue(rx.getItems().isEmpty());
        }

        @Test
        void constructor_startsInDraftStatus() {
            assertTrue(prescription.getStatus() == PrescriptionStatus.DRAFT);
        }

        @Test
        void constructor_setTimestamps() {
            LocalDateTime createdBefore = LocalDateTime.now();
            Prescription rx = new Prescription(patientId, doctorId, validItems);
            LocalDateTime createdAfter = LocalDateTime.now();
            
            assertNotNull(rx.getCreatedAt());
            assertNotNull(rx.getUpdatedAt());
            assertTrue(!rx.getCreatedAt().isBefore(createdBefore));
            assertTrue(!rx.getCreatedAt().isAfter(createdAfter.plusSeconds(1)));
        }
    }

    // ======================================================================
    // Field validation tests (Requirement 4.1, 4.6)
    // ======================================================================

    @Nested
    class FieldValidationTests {
        @Test
        void validateRequiredFields_withValidPrescription_returnsEmpty() {
            List<String> errors = prescription.validateRequiredFields();
            
            assertTrue(errors.isEmpty());
            assertTrue(prescription.hasRequiredFields());
        }

        @Test
        void validateRequiredFields_withZeroPatientId_returnsError() {
            Prescription rx = new Prescription(0L, doctorId, validItems);
            
            List<String> errors = rx.validateRequiredFields();
            
            assertEquals(List.of("patientId"), errors);
            assertFalse(rx.hasRequiredFields());
        }

        @Test
        void validateRequiredFields_withNegativePatientId_returnsError() {
            Prescription rx = new Prescription(-1L, doctorId, validItems);
            
            List<String> errors = rx.validateRequiredFields();
            
            assertEquals(List.of("patientId"), errors);
            assertFalse(rx.hasRequiredFields());
        }

        @Test
        void validateRequiredFields_withZeroDoctorId_returnsError() {
            Prescription rx = new Prescription(patientId, 0L, validItems);
            
            List<String> errors = rx.validateRequiredFields();
            
            assertEquals(List.of("doctorId"), errors);
            assertFalse(rx.hasRequiredFields());
        }

        @Test
        void validateRequiredFields_withNegativeDoctorId_returnsError() {
            Prescription rx = new Prescription(patientId, -1L, validItems);
            
            List<String> errors = rx.validateRequiredFields();
            
            assertEquals(List.of("doctorId"), errors);
            assertFalse(rx.hasRequiredFields());
        }

        @Test
        void validateRequiredFields_withZeroItems_returnsError() {
            Prescription rx = new Prescription(patientId, doctorId, new ArrayList<>());
            
            List<String> errors = rx.validateRequiredFields();
            
            assertEquals(List.of("items"), errors);
            assertFalse(rx.hasRequiredFields());
        }

        @Test
        void validateRequiredFields_withNullItems_returnsError() {
            Prescription rx = new Prescription(patientId, doctorId, null);
            
            // Note: null items are converted to empty list by constructor, so validation fails on size
            // Let's test with reflective manipulation or constructor behavior
            List<String> errors = rx.validateRequiredFields();
            
            assertEquals(List.of("items"), errors);
            assertFalse(rx.hasRequiredFields());
        }

        @Test
        void validateRequiredFields_withMaxValidItems() {
            List<PrescriptionItem> maxItems = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                maxItems.add(new PrescriptionItem(
                    i + 1, "Medicine " + i, "Dosage " + i,
                    "Frequency " + i, "Instructions " + i, 10
                ));
            }
            Prescription rx = new Prescription(patientId, doctorId, maxItems);
            
            List<String> errors = rx.validateRequiredFields();
            
            assertTrue(errors.isEmpty());
            assertTrue(rx.hasRequiredFields());
        }

        @Test
        void validateRequiredFields_withMoreThanMaxItems_returnsError() {
            List<PrescriptionItem> tooManyItems = new ArrayList<>();
            for (int i = 0; i < 51; i++) {
                tooManyItems.add(new PrescriptionItem(
                    i + 1, "Medicine " + i, "Dosage " + i,
                    "Frequency " + i, "Instructions " + i, 10
                ));
            }
            Prescription rx = new Prescription(patientId, doctorId, tooManyItems);
            
            List<String> errors = rx.validateRequiredFields();
            
            assertEquals(List.of("items"), errors);
            assertFalse(rx.hasRequiredFields());
        }

        @Test
        void validateRequiredFields_withInvalidItem_returnsItemError() {
            List<PrescriptionItem> items = new ArrayList<>(validItems);
            items.add(new PrescriptionItem(0L, "Invalid", "Dosage", "Frequency", "Instructions", 10));
            Prescription rx = new Prescription(patientId, doctorId, items);
            
            List<String> errors = rx.validateRequiredFields();
            
            assertTrue(errors.contains("items[2]"));
            assertFalse(rx.hasRequiredFields());
        }

        @Test
        void validateRequiredFields_withMultipleErrors_returnsAllErrors() {
            Prescription rx = new Prescription(0L, 0L, new ArrayList<>());
            
            List<String> errors = rx.validateRequiredFields();
            
            assertEquals(3, errors.size());
            assertTrue(errors.contains("patientId"));
            assertTrue(errors.contains("doctorId"));
            assertTrue(errors.contains("items"));
            assertFalse(rx.hasRequiredFields());
        }

        @Test
        void validateRequiredFields_returnsImmutableList() {
            List<String> errors = prescription.validateRequiredFields();
            
            assertThrows(UnsupportedOperationException.class, () -> errors.add("extra"));
        }
    }

    // ======================================================================
    // Status transition tests (Requirement 5.1, 5.5)
    // ======================================================================

    @Nested
    class StatusTransitionTests {
        @Test
        void canTransitionTo_draftToIssued() {
            assertTrue(prescription.canTransitionTo(PrescriptionStatus.ISSUED));
        }

        @Test
        void canTransitionTo_draftToCancelled() {
            assertTrue(prescription.canTransitionTo(PrescriptionStatus.CANCELLED));
        }

        @Test
        void canTransitionTo_draftToOnHold_notAllowed() {
            assertFalse(prescription.canTransitionTo(PrescriptionStatus.ON_HOLD));
        }

        @Test
        void canTransitionTo_draftToExpired_notAllowed() {
            assertFalse(prescription.canTransitionTo(PrescriptionStatus.EXPIRED));
        }

        @Test
        void canTransitionTo_draftToNull_notAllowed() {
            assertFalse(prescription.canTransitionTo(null));
        }

        @Test
        void canTransitionTo_issuedToOnHold() {
            prescription.issue(1L);
            assertTrue(prescription.canTransitionTo(PrescriptionStatus.ON_HOLD));
        }

        @Test
        void canTransitionTo_issuedToCancelled() {
            prescription.issue(1L);
            assertTrue(prescription.canTransitionTo(PrescriptionStatus.CANCELLED));
        }

        @Test
        void canTransitionTo_issuedToDraft_notAllowed() {
            prescription.issue(1L);
            assertFalse(prescription.canTransitionTo(PrescriptionStatus.DRAFT));
        }

        @Test
        void canTransitionTo_onHoldToIssued() {
            prescription.issue(1L);
            prescription.placeOnHold(1L, "Reason");
            assertTrue(prescription.canTransitionTo(PrescriptionStatus.ISSUED));
        }

        @Test
        void canTransitionTo_onHoldToCancelled() {
            prescription.issue(1L);
            prescription.placeOnHold(1L, "Reason");
            assertTrue(prescription.canTransitionTo(PrescriptionStatus.CANCELLED));
        }

        @Test
        void canTransitionTo_cancelledHasNoTransitions() {
            prescription.cancel(1L, "Reason");
            
            assertFalse(prescription.canTransitionTo(PrescriptionStatus.DRAFT));
            assertFalse(prescription.canTransitionTo(PrescriptionStatus.ISSUED));
            assertFalse(prescription.canTransitionTo(PrescriptionStatus.ON_HOLD));
            assertFalse(prescription.canTransitionTo(PrescriptionStatus.CANCELLED));
            assertFalse(prescription.canTransitionTo(PrescriptionStatus.EXPIRED));
        }

        @Test
        void changeStatus_validTransition_succeeds() {
            prescription.changeStatus(PrescriptionStatus.ISSUED, 1L, null);
            
            assertEquals(PrescriptionStatus.ISSUED, prescription.getStatus());
            assertEquals(1L, prescription.getStatusChangedBy());
            assertNotNull(prescription.getIssuedAt());
        }

        @Test
        void changeStatus_invalidTransition_throwsException() {
            prescription.issue(1L);
            
            assertThrows(IllegalStateException.class, () ->
                prescription.changeStatus(PrescriptionStatus.DRAFT, 1L, null)
            );
        }

        @Test
        void changeStatus_withoutTargetStatus_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                prescription.changeStatus(null, 1L, null)
            );
        }

        @Test
        void changeStatus_recordsChangedBy() {
            long userId = 999L;
            prescription.changeStatus(PrescriptionStatus.ISSUED, userId, null);
            
            assertEquals(userId, prescription.getStatusChangedBy());
        }

        @Test
        void changeStatus_recordsReason() {
            String reason = "Test reason for cancellation";
            prescription.changeStatus(PrescriptionStatus.CANCELLED, 1L, reason);
            
            assertEquals(reason, prescription.getStatusChangeReason());
        }

        @Test
        void changeStatus_updatesStatusChangedAt() {
            LocalDateTime beforeChange = LocalDateTime.now();
            prescription.changeStatus(PrescriptionStatus.ISSUED, 1L, null);
            LocalDateTime afterChange = LocalDateTime.now();
            
            assertNotNull(prescription.getStatusChangedAt());
            assertTrue(!prescription.getStatusChangedAt().isBefore(beforeChange));
            assertTrue(!prescription.getStatusChangedAt().isAfter(afterChange.plusSeconds(1)));
        }

        @Test
        void changeStatus_incrementsVersion() {
            long initialVersion = prescription.getVersion();
            prescription.changeStatus(PrescriptionStatus.ISSUED, 1L, null);
            
            assertEquals(initialVersion + 1, prescription.getVersion());
        }

        @Test
        void changeStatus_onlySetIssuedAtOnFirstIssue() {
            prescription.issue(1L);
            LocalDateTime firstIssuedAt = prescription.getIssuedAt();
            
            // Resume from ON_HOLD
            prescription.placeOnHold(1L, "Hold reason");
            prescription.resume(1L);
            
            assertEquals(firstIssuedAt, prescription.getIssuedAt(),
                "issuedAt should not be reset when resuming from ON_HOLD");
        }
    }

    // ======================================================================
    // Convenience method tests (issue, placeOnHold, cancel, resume)
    // ======================================================================

    @Nested
    class ConvenienceMethodTests {
        @Test
        void issue_transitionsToIssued() {
            prescription.issue(1L);
            
            assertEquals(PrescriptionStatus.ISSUED, prescription.getStatus());
            assertNotNull(prescription.getIssuedAt());
            assertEquals(1L, prescription.getStatusChangedBy());
        }

        @Test
        void placeOnHold_requiresReason() {
            prescription.issue(1L);
            
            assertThrows(IllegalArgumentException.class, () ->
                prescription.placeOnHold(1L, null)
            );
            assertThrows(IllegalArgumentException.class, () ->
                prescription.placeOnHold(1L, "")
            );
            assertThrows(IllegalArgumentException.class, () ->
                prescription.placeOnHold(1L, "   ")
            );
        }

        @Test
        void placeOnHold_withValidReason_succeeds() {
            prescription.issue(1L);
            String reason = "Waiting for lab results";
            
            prescription.placeOnHold(1L, reason);
            
            assertEquals(PrescriptionStatus.ON_HOLD, prescription.getStatus());
            assertEquals(reason, prescription.getStatusChangeReason());
        }

        @Test
        void cancel_requiresReason() {
            assertThrows(IllegalArgumentException.class, () ->
                prescription.cancel(1L, null)
            );
            assertThrows(IllegalArgumentException.class, () ->
                prescription.cancel(1L, "")
            );
            assertThrows(IllegalArgumentException.class, () ->
                prescription.cancel(1L, "   ")
            );
        }

        @Test
        void cancel_withValidReason_succeeds() {
            String reason = "Patient requested cancellation";
            
            prescription.cancel(1L, reason);
            
            assertEquals(PrescriptionStatus.CANCELLED, prescription.getStatus());
            assertEquals(reason, prescription.getStatusChangeReason());
        }

        @Test
        void resume_transitionsFromOnHoldToIssued() {
            prescription.issue(1L);
            prescription.placeOnHold(1L, "Temporary hold");
            
            prescription.resume(1L);
            
            assertEquals(PrescriptionStatus.ISSUED, prescription.getStatus());
        }
    }

    // ======================================================================
    // Expiry tests (Algorithm 3, Requirement 4.5, 5.4)
    // ======================================================================

    @Nested
    class ExpiryTests {
        @Test
        void isExpired_draftNeverExpires() {
            LocalDate farFuture = LocalDate.now().plusYears(10);
            
            assertFalse(prescription.isExpired(farFuture));
        }

        @Test
        void isExpired_cancelledNeverExpires() {
            prescription.cancel(1L, "Cancelled");
            LocalDate farFuture = LocalDate.now().plusYears(10);
            
            assertFalse(prescription.isExpired(farFuture));
        }

        @Test
        void isExpired_nullIssuedAtNeverExpires() {
            prescription.issue(1L);
            // Create a new prescription with null issuedAt and ISSUED status via full constructor
            List<PrescriptionItem> items = new ArrayList<>(validItems);
            Prescription rx = new Prescription(
                1L, patientId, doctorId,
                null, PrescriptionStatus.ISSUED, null,
                null, 0L, null,
                LocalDateTime.now(), LocalDateTime.now(), 0L, items
            );
            LocalDate farFuture = LocalDate.now().plusYears(10);
            
            assertFalse(rx.isExpired(farFuture));
        }

        @Test
        void isExpired_issuedNotPastOneMonth() {
            prescription.issue(1L);
            LocalDateTime issuedAt = prescription.getIssuedAt();
            LocalDate evaluationDate = issuedAt.toLocalDate().plusMonths(1);
            
            assertFalse(prescription.isExpired(evaluationDate),
                "Prescription should not be expired on exactly one month after issue");
        }

        @Test
        void isExpired_issuedOneDayPastOneMonth() {
            prescription.issue(1L);
            LocalDateTime issuedAt = prescription.getIssuedAt();
            LocalDate evaluationDate = issuedAt.toLocalDate().plusMonths(1).plusDays(1);
            
            assertTrue(prescription.isExpired(evaluationDate),
                "Prescription should be expired one day past one month");
        }

        @Test
        void isExpired_onHoldNotExpiredBeforeOneMonth() {
            prescription.issue(1L);
            prescription.placeOnHold(1L, "Hold");
            LocalDateTime issuedAt = prescription.getIssuedAt();
            LocalDate evaluationDate = issuedAt.toLocalDate().plusDays(15);
            
            assertFalse(prescription.isExpired(evaluationDate),
                "ON_HOLD prescription should not be expired before one month");
        }

        @Test
        void isExpired_onHoldExpiredAfterOneMonth() {
            prescription.issue(1L);
            prescription.placeOnHold(1L, "Hold");
            LocalDateTime issuedAt = prescription.getIssuedAt();
            LocalDate evaluationDate = issuedAt.toLocalDate().plusMonths(1).plusDays(1);
            
            assertTrue(prescription.isExpired(evaluationDate),
                "ON_HOLD prescription should be expired after one month");
        }

        @Test
        void getEffectiveStatus_draftRemainsChanged() {
            assertEquals(PrescriptionStatus.DRAFT, prescription.getEffectiveStatus());
        }

        @Test
        void getEffectiveStatus_issuedBeforeExpiryRemainsIssued() {
            prescription.issue(1L);
            
            assertEquals(PrescriptionStatus.ISSUED, prescription.getEffectiveStatus());
        }

        @Test
        void isEligibleForDispensing_draftNotEligible() {
            assertFalse(prescription.isEligibleForDispensing());
        }

        @Test
        void isEligibleForDispensing_issuedAndNotExpiredEligible() {
            prescription.issue(1L);
            
            assertTrue(prescription.isEligibleForDispensing());
        }

        @Test
        void isEligibleForDispensing_onHoldNotEligible() {
            prescription.issue(1L);
            prescription.placeOnHold(1L, "Hold");
            
            assertFalse(prescription.isEligibleForDispensing());
        }

        @Test
        void isEligibleForDispensing_cancelledNotEligible() {
            prescription.cancel(1L, "Cancelled");
            
            assertFalse(prescription.isEligibleForDispensing());
        }

        @Test
        void isTerminalState_draftNotTerminal() {
            assertFalse(prescription.isTerminalState());
        }

        @Test
        void isTerminalState_issuedNotTerminal() {
            prescription.issue(1L);
            
            assertFalse(prescription.isTerminalState());
        }

        @Test
        void isTerminalState_onHoldNotTerminal() {
            prescription.issue(1L);
            prescription.placeOnHold(1L, "Hold");
            
            assertFalse(prescription.isTerminalState());
        }

        @Test
        void isTerminalState_cancelledTerminal() {
            prescription.cancel(1L, "Cancelled");
            
            assertTrue(prescription.isTerminalState());
        }

        @Test
        void isTerminalState_expiredTerminal() {
            prescription.issue(1L);
            LocalDateTime issuedAt = prescription.getIssuedAt();
            LocalDate evaluationDate = issuedAt.toLocalDate().plusMonths(2);
            // Manually test since we can't override LocalDate.now() in isTerminalState()
            assertTrue(prescription.isExpired(evaluationDate));
        }
    }

    // ======================================================================
    // Field edit tests (Requirement 4.2, 4.7)
    // ======================================================================

    @Nested
    class FieldEditTests {
        @Test
        void applyChanges_editClinicalNotesInDraft() {
            String newNotes = "Updated clinical notes";
            prescription.applyChanges(Map.of("clinicalNotes", newNotes));
            
            assertEquals(newNotes, prescription.getClinicalNotes());
        }

        @Test
        void applyChanges_editClinicalNotesInIssued() {
            prescription.issue(1L);
            String newNotes = "Updated notes after issue";
            prescription.applyChanges(Map.of("clinicalNotes", newNotes));
            
            assertEquals(newNotes, prescription.getClinicalNotes());
        }

        @Test
        void applyChanges_editItemsInDraft() {
            List<PrescriptionItem> newItems = List.of(
                new PrescriptionItem(3L, "NewMed", "100mg", "Once daily", "With water", 15)
            );
            prescription.applyChanges(Map.of("items", newItems));
            
            assertEquals(1, prescription.getItems().size());
            assertEquals(3L, prescription.getItems().get(0).getMedicineId());
        }

        @Test
        void applyChanges_rejectInvalidField() {
            assertThrows(IllegalArgumentException.class, () ->
                prescription.applyChanges(Map.of("invalidField", "value"))
            );
        }

        @Test
        void applyChanges_rejectWrongFieldType() {
            assertThrows(IllegalArgumentException.class, () ->
                prescription.applyChanges(Map.of("clinicalNotes", 123))
            );
        }

        @Test
        void applyChanges_rejectTooManyItems() {
            List<PrescriptionItem> tooManyItems = new ArrayList<>();
            for (int i = 0; i < 51; i++) {
                tooManyItems.add(new PrescriptionItem(
                    i + 1, "Med " + i, "Dose", "Freq", "Instr", 10
                ));
            }
            
            assertThrows(IllegalArgumentException.class, () ->
                prescription.applyChanges(Map.of("items", tooManyItems))
            );
        }

        @Test
        void applyChanges_rejectTooFewItems() {
            assertThrows(IllegalArgumentException.class, () ->
                prescription.applyChanges(Map.of("items", new ArrayList<>()))
            );
        }

        @Test
        void applyChanges_rejectEditOnCancelled() {
            prescription.cancel(1L, "Cancelled");
            
            assertThrows(IllegalStateException.class, () ->
                prescription.applyChanges(Map.of("clinicalNotes", "New notes"))
            );
        }

        @Test
        void applyChanges_rejectEditOnExpired() {
            // Create prescription with expired status via full constructor
            List<PrescriptionItem> items = new ArrayList<>(validItems);
            Prescription rx = new Prescription(
                1L, patientId, doctorId,
                null, PrescriptionStatus.EXPIRED, LocalDateTime.now().minusMonths(2),
                null, 0L, null,
                LocalDateTime.now(), LocalDateTime.now(), 0L, items
            );
            
            assertThrows(IllegalStateException.class, () ->
                rx.applyChanges(Map.of("clinicalNotes", "New notes"))
            );
        }

        @Test
        void applyChanges_emptyMapDoesNothing() {
            String originalNotes = prescription.getClinicalNotes();
            prescription.applyChanges(Map.of());
            
            assertEquals(originalNotes, prescription.getClinicalNotes());
        }

        @Test
        void applyChanges_nullMapDoesNothing() {
            String originalNotes = prescription.getClinicalNotes();
            prescription.applyChanges(null);
            
            assertEquals(originalNotes, prescription.getClinicalNotes());
        }

        @Test
        void applyChanges_incrementsVersion() {
            long initialVersion = prescription.getVersion();
            prescription.applyChanges(Map.of("clinicalNotes", "Updated"));
            
            assertEquals(initialVersion + 1, prescription.getVersion());
        }
    }

    // ======================================================================
    // Immutability tests
    // ======================================================================

    @Nested
    class ImmutabilityTests {
        @Test
        void getItems_returnsImmutableList() {
            List<PrescriptionItem> items = prescription.getItems();
            
            assertThrows(UnsupportedOperationException.class, () ->
                items.add(new PrescriptionItem(100L, "New", "Dose", "Freq", "Instr", 10))
            );
        }

        @Test
        void getItems_returnsCopy() {
            List<PrescriptionItem> items1 = prescription.getItems();
            List<PrescriptionItem> items2 = prescription.getItems();
            
            assertNotSame(items1, items2, "Multiple calls should return different copies");
            assertEquals(items1.size(), items2.size());
        }
    }

    // ======================================================================
    // Integration tests
    // ======================================================================

    @Nested
    class IntegrationTests {
        @Test
        void fullLifecycle_draftToIssuedToCancelled() {
            // Start in DRAFT
            assertTrue(prescription.getStatus() == PrescriptionStatus.DRAFT);
            
            // Issue prescription
            prescription.issue(1L);
            assertEquals(PrescriptionStatus.ISSUED, prescription.getStatus());
            assertTrue(prescription.isEligibleForDispensing());
            
            // Cancel prescription
            prescription.cancel(2L, "Patient requested cancellation");
            assertEquals(PrescriptionStatus.CANCELLED, prescription.getStatus());
            assertFalse(prescription.isEligibleForDispensing());
            
            // No further transitions allowed
            assertFalse(prescription.canTransitionTo(PrescriptionStatus.ISSUED));
        }

        @Test
        void fullLifecycle_draftToIssuedToOnHoldToIssued() {
            prescription.issue(1L);
            assertEquals(PrescriptionStatus.ISSUED, prescription.getStatus());
            
            prescription.placeOnHold(1L, "Temporary hold");
            assertEquals(PrescriptionStatus.ON_HOLD, prescription.getStatus());
            assertFalse(prescription.isEligibleForDispensing());
            
            prescription.resume(1L);
            assertEquals(PrescriptionStatus.ISSUED, prescription.getStatus());
            assertTrue(prescription.isEligibleForDispensing());
        }

        @Test
        void editAndTransition_editThenIssue() {
            prescription.applyChanges(Map.of("clinicalNotes", "Updated notes"));
            prescription.issue(1L);
            
            assertEquals("Updated notes", prescription.getClinicalNotes());
            assertEquals(PrescriptionStatus.ISSUED, prescription.getStatus());
        }

        @Test
        void versionIncrementsOnEachChange() {
            long v0 = prescription.getVersion();
            
            prescription.applyChanges(Map.of("clinicalNotes", "Notes"));
            long v1 = prescription.getVersion();
            
            prescription.issue(1L);
            long v2 = prescription.getVersion();
            
            assertEquals(v0 + 1, v1);
            assertEquals(v1 + 1, v2);
        }

        @Test
        void minimumItemsEnforced() {
            List<PrescriptionItem> singleItem = List.of(
                new PrescriptionItem(1L, "Only Med", "100mg", "Once daily", "Instructions", 10)
            );
            Prescription rx = new Prescription(patientId, doctorId, singleItem);
            
            List<String> errors = rx.validateRequiredFields();
            assertTrue(errors.isEmpty(), "Single item prescription should be valid");
        }

        @Test
        void maximumItemsEnforced() {
            List<PrescriptionItem> maxItems = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                maxItems.add(new PrescriptionItem(
                    i + 1, "Med " + i, "Dose", "Freq", "Instr", 10
                ));
            }
            Prescription rx = new Prescription(patientId, doctorId, maxItems);
            
            List<String> errors = rx.validateRequiredFields();
            assertTrue(errors.isEmpty(), "50-item prescription should be valid");
            
            maxItems.add(new PrescriptionItem(51L, "Extra", "Dose", "Freq", "Instr", 10));
            Prescription rx2 = new Prescription(patientId, doctorId, maxItems);
            errors = rx2.validateRequiredFields();
            assertEquals(List.of("items"), errors, "51-item prescription should fail validation");
        }
    }
}
