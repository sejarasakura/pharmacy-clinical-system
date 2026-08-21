package pharmacy_system.model.clinical_prescription;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.statistics.Statistics;
import net.jqwik.time.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for the Prescription model.
 * 
 * **Validates: Requirements 4.5**
 * **Property 7: Expiry monotonicity**
 */
@PropertyDefaults(tries = 100) // Run each property 100 times with random inputs
public class PrescriptionPropertyTest {

    // ------------------------------------------------------------------
    // Property 7: Expiry monotonicity (Requirements 4.5, Algorithm 3)
    // ------------------------------------------------------------------

    /**
     * Property 7: Prescription expiry is evaluated deterministically as one month
     * after issue date.
     * 
     * For all prescriptions:
     * - DRAFT prescriptions never expire
     * - CANCELLED prescriptions never expire (they are terminal in their own right)
     * - Prescriptions with null issuedAt never expire
     * - For ISSUED/ON_HOLD prescriptions with issuedAt not null:
     *   - isExpired(today) returns false when today ≤ issuedAt + 1 month
     *   - isExpired(today) returns true when today > issuedAt + 1 month
     */
    @Property
    @Report(Reporting.GENERATED)
    @Label("Property 7: Expiry monotonicity - Prescription expiry is deterministic as one month after issue date")
    void expiryIsDeterministicOneMonthAfterIssueDate(
            @ForAll("prescriptionWithArbitraryDates") Prescription prescription,
            @ForAll LocalDate evaluationDate) {
        
        boolean isExpired = prescription.isExpired(evaluationDate);
        
        // Determine expected expiry based on prescription state
        boolean expectedExpired = calculateExpectedExpiry(prescription, evaluationDate);
        
        assertEquals(expectedExpired, isExpired,
                String.format("Prescription expiry evaluation should be deterministic: " +
                    "prescription=%s, evaluationDate=%s, issuedAt=%s, status=%s",
                    prescription.getPrescriptionId(), evaluationDate,
                    prescription.getIssuedAt(), prescription.getStatus()));
    }

    /**
     * Helper method to calculate expected expiry according to Algorithm 3.
     */
    private boolean calculateExpectedExpiry(Prescription prescription, LocalDate evaluationDate) {
        // DRAFT prescriptions never expire
        if (prescription.getStatus() == PrescriptionStatus.DRAFT) {
            return false;
        }
        
        // CANCELLED prescriptions never expire (they are terminal in their own right)
        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            return false;
        }
        
        // Prescriptions with null issuedAt never expire
        if (prescription.getIssuedAt() == null) {
            return false;
        }
        
        // For ISSUED/ON_HOLD prescriptions with issuedAt not null:
        // Expired when evaluationDate is after issuedAt + 1 month
        LocalDate oneMonthAfterIssue = prescription.getIssuedAt().toLocalDate().plusMonths(1);
        return evaluationDate.isAfter(oneMonthAfterIssue);
    }

    /**
     * Additional property: DRAFT prescriptions and prescriptions with null issuedAt
     * never expire, regardless of evaluation date.
     */
    @Property
    @Label("Draft prescriptions and null issuedAt never expire")
    void draftAndNullIssuedAtNeverExpire(
            @ForAll("draftOrNullIssuedAtPrescription") Prescription prescription,
            @ForAll LocalDate evaluationDate) {
        
        boolean isExpired = prescription.isExpired(evaluationDate);
        
        assertFalse(isExpired,
                String.format("DRAFT prescriptions and prescriptions with null issuedAt should never expire: " +
                    "prescription=%s, evaluationDate=%s, issuedAt=%s, status=%s",
                    prescription.getPrescriptionId(), evaluationDate,
                    prescription.getIssuedAt(), prescription.getStatus()));
    }

    /**
     * Property: Expired prescriptions are never eligible for dispensing.
     * This is a corollary of Property 7 and Requirement 5.4.
     */
    @Property
    @Label("Expired prescriptions are never eligible for dispensing")
    void expiredPrescriptionsNotEligibleForDispensing(
            @ForAll("expiredPrescription") Prescription prescription) {
        
        assertFalse(prescription.isEligibleForDispensing(),
                String.format("Expired prescriptions should not be eligible for dispensing: " +
                    "prescription=%s, status=%s, issuedAt=%s",
                    prescription.getPrescriptionId(), prescription.getStatus(), prescription.getIssuedAt()));
    }

    /**
     * Property: Effective status correctly shows EXPIRED for ISSUED/ON_HOLD
     * prescriptions past their expiry date.
     * Note: This property tests the logic directly since we can't easily override
     * LocalDate.now() in getEffectiveStatus().
     */
    @Property
    @Label("ISSUED/ON_HOLD prescriptions past expiry are effectively expired")
    void issuedOrOnHoldPastExpiryAreEffectivelyExpired(
            @ForAll("issuedOrOnHoldPrescription") Prescription prescription,
            @ForAll("dateAfterExpiry") LocalDate evaluationDate) {
        
        // Test the isExpired logic directly
        boolean isExpired = prescription.isExpired(evaluationDate);
        
        // For ISSUED/ON_HOLD prescriptions with issuedAt not null and evaluationDate after expiry
        if (prescription.getIssuedAt() != null) {
            LocalDate oneMonthAfterIssue = prescription.getIssuedAt().toLocalDate().plusMonths(1);
            if (evaluationDate.isAfter(oneMonthAfterIssue)) {
                assertTrue(isExpired,
                        String.format("ISSUED/ON_HOLD prescription should be expired when evaluation date is after expiry: " +
                            "prescription=%s, status=%s, issuedAt=%s, evaluationDate=%s, oneMonthAfterIssue=%s",
                            prescription.getPrescriptionId(), prescription.getStatus(),
                            prescription.getIssuedAt(), evaluationDate, oneMonthAfterIssue));
            }
        }
    }

    /**
     * Property: Prescriptions not yet past expiry are not expired.
     */
    @Property
    @Label("Prescriptions not yet past expiry are not expired")
    void prescriptionsNotPastExpiryAreNotExpired(
            @ForAll("issuedOrOnHoldPrescription") Prescription prescription,
            @ForAll("dateBeforeOrOnExpiry") LocalDate evaluationDate) {
        
        boolean isExpired = prescription.isExpired(evaluationDate);
        
        // For ISSUED/ON_HOLD prescriptions with issuedAt not null and evaluationDate on or before expiry
        if (prescription.getIssuedAt() != null) {
            LocalDate oneMonthAfterIssue = prescription.getIssuedAt().toLocalDate().plusMonths(1);
            if (!evaluationDate.isAfter(oneMonthAfterIssue)) {
                assertFalse(isExpired,
                        String.format("ISSUED/ON_HOLD prescription should not be expired when evaluation date is on or before expiry: " +
                            "prescription=%s, status=%s, issuedAt=%s, evaluationDate=%s, oneMonthAfterIssue=%s",
                            prescription.getPrescriptionId(), prescription.getStatus(),
                            prescription.getIssuedAt(), evaluationDate, oneMonthAfterIssue));
            }
        }
    }

    // ------------------------------------------------------------------
    // Arbitrary providers
    // ------------------------------------------------------------------

    /**
     * Provides prescriptions with arbitrary dates, statuses, and issue dates.
     */
    @Provide
    Arbitrary<Prescription> prescriptionWithArbitraryDates() {
        Arbitrary<Long> id = Arbitraries.longs().between(1, 1000);
        Arbitrary<Long> patientId = Arbitraries.longs().between(1, 1000);
        Arbitrary<Long> doctorId = Arbitraries.longs().between(1, 1000);
        Arbitrary<PrescriptionStatus> status = Arbitraries.of(PrescriptionStatus.class);
        Arbitrary<LocalDateTime> issuedAt = Arbitraries.oneOf(
            Arbitraries.just((LocalDateTime) null),
            DateTimes.dateTimes().between(
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 59)
            )
        );
        
        return Combinators.combine(id, patientId, doctorId, status, issuedAt)
                .as(this::createTestPrescription);
    }

    /**
     * Provides DRAFT prescriptions or prescriptions with null issuedAt.
     */
    @Provide
    Arbitrary<Prescription> draftOrNullIssuedAtPrescription() {
        Arbitrary<Long> id = Arbitraries.longs().between(1, 1000);
        Arbitrary<Long> patientId = Arbitraries.longs().between(1, 1000);
        Arbitrary<Long> doctorId = Arbitraries.longs().between(1, 1000);
        
        // Either DRAFT status or any status with null issuedAt
        Arbitrary<PrescriptionStatus> status = Arbitraries.of(PrescriptionStatus.class);
        Arbitrary<LocalDateTime> issuedAt = Arbitraries.just(null);
        
        Arbitrary<Prescription> draftPrescription = Combinators.combine(
            id, patientId, doctorId, Arbitraries.just(PrescriptionStatus.DRAFT), issuedAt
        ).as(this::createTestPrescription);
        
        Arbitrary<Prescription> nullIssuedAtPrescription = Combinators.combine(
            id, patientId, doctorId, status.filter(s -> s != PrescriptionStatus.DRAFT), issuedAt
        ).as(this::createTestPrescription);
        
        return Arbitraries.oneOf(draftPrescription, nullIssuedAtPrescription);
    }

    /**
     * Provides ISSUED or ON_HOLD prescriptions with non-null issuedAt.
     */
    @Provide
    Arbitrary<Prescription> issuedOrOnHoldPrescription() {
        Arbitrary<Long> id = Arbitraries.longs().between(1, 1000);
        Arbitrary<Long> patientId = Arbitraries.longs().between(1, 1000);
        Arbitrary<Long> doctorId = Arbitraries.longs().between(1, 1000);
        Arbitrary<PrescriptionStatus> status = Arbitraries.of(
            PrescriptionStatus.ISSUED, PrescriptionStatus.ON_HOLD
        );
        Arbitrary<LocalDateTime> issuedAt = DateTimes.dateTimes().between(
            LocalDateTime.of(2020, 1, 1, 0, 0),
            LocalDateTime.of(2025, 12, 31, 23, 59)
        );
        
        return Combinators.combine(id, patientId, doctorId, status, issuedAt)
                .as(this::createTestPrescription);
    }

    /**
     * Provides expired prescriptions (ISSUED/ON_HOLD with issuedAt more than 1 month ago).
     */
    @Provide
    Arbitrary<Prescription> expiredPrescription() {
        Arbitrary<Long> id = Arbitraries.longs().between(1, 1000);
        Arbitrary<Long> patientId = Arbitraries.longs().between(1, 1000);
        Arbitrary<Long> doctorId = Arbitraries.longs().between(1, 1000);
        Arbitrary<PrescriptionStatus> status = Arbitraries.of(
            PrescriptionStatus.ISSUED, PrescriptionStatus.ON_HOLD
        );
        
        // issuedAt at least 2 months ago to ensure expired
        LocalDateTime now = LocalDateTime.now();
        Arbitrary<LocalDateTime> issuedAt = DateTimes.dateTimes().between(
            LocalDateTime.of(2020, 1, 1, 0, 0),
            now.minusMonths(2) // At least 2 months ago
        );
        
        return Combinators.combine(id, patientId, doctorId, status, issuedAt)
                .as(this::createTestPrescription);
    }

    /**
     * Provides dates that are after a prescription's expiry date.
     * Used with issuedOrOnHoldPrescription to test effective status.
     */
    @Provide
    Arbitrary<LocalDate> dateAfterExpiry() {
        // Generate a date that is at least 1 month + 1 day after some reference date
        Arbitrary<LocalDate> referenceDate = Dates.dates().between(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2025, 12, 31)
        );
        
        return referenceDate.map(date -> date.plusMonths(1).plusDays(1));
    }

    /**
     * Provides dates that are on or before a prescription's expiry date.
     */
    @Provide
    Arbitrary<LocalDate> dateBeforeOrOnExpiry() {
        // Generate a date that is on or before some reference date plus 1 month
        Arbitrary<LocalDate> referenceDate = Dates.dates().between(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2025, 12, 31)
        );
        
        return referenceDate.flatMap(date -> {
            LocalDate expiryDate = date.plusMonths(1);
            return Dates.dates().between(date, expiryDate);
        });
    }

    /**
     * Provides arbitrary LocalDate values for evaluation.
     */
    @Provide
    Arbitrary<LocalDate> arbitraryEvaluationDate() {
        return Dates.dates().between(
            LocalDate.of(2019, 1, 1), // Start before any prescription issue dates
            LocalDate.of(2026, 12, 31) // End after any prescription issue dates
        );
    }

    // ------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------

    /**
     * Creates a test prescription with the given parameters.
     * Uses empty items list and default timestamps.
     */
    private Prescription createTestPrescription(
            long id, long patientId, long doctorId,
            PrescriptionStatus status, LocalDateTime issuedAt) {
        
        // Create prescription with default values
        Prescription prescription = new Prescription(
            id,
            patientId,
            doctorId,
            null, // clinicalNotes
            status,
            issuedAt,
            issuedAt != null ? issuedAt.plusHours(1) : null, // statusChangedAt
            1L, // statusChangedBy
            status == PrescriptionStatus.ON_HOLD || status == PrescriptionStatus.CANCELLED 
                ? "Test reason" : null, // statusChangeReason
            issuedAt != null ? issuedAt.minusDays(1) : LocalDateTime.now().minusDays(1), // createdAt
            issuedAt != null ? issuedAt : LocalDateTime.now(), // updatedAt
            0L, // version
            Collections.emptyList() // items
        );
        
        return prescription;
    }
}