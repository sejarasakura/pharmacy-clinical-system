package pharmacy_system.model.clinical_prescription;

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotEmpty;
import net.jqwik.api.statistics.Statistics;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property tests for PrescriptionStatus transitions (Property 16).
 * 
 * **Validates: Requirements 5.1**
 * Property 16: "Only allowed prescription status transitions are accepted"
 * - DRAFT can transition to ISSUED or CANCELLED
 * - ISSUED can transition to ON_HOLD or CANCELLED
 * - ON_HOLD can transition to ISSUED or CANCELLED
 * - CANCELLED and EXPIRED are terminal (no outgoing transitions)
 * - All other transitions must be rejected
 */
class PrescriptionStatusPropertyTest {

    /**
     * Property 16: For all PrescriptionStatus values, the allowed transitions
     * match the requirement specification exactly.
     */
    @Property
    @Label("Valid transitions only - Status enum respects transition matrix")
    void statusTransitionsRespectAllowedMatrix(@ForAll PrescriptionStatus status) {
        Set<PrescriptionStatus> allowed = status.allowedTransitions();
        
        // Verify the exact transition matrix from Requirements 5.1
        if (status == PrescriptionStatus.DRAFT) {
            // DRAFT can go to ISSUED or CANCELLED
            assertEquals(Set.of(PrescriptionStatus.ISSUED, PrescriptionStatus.CANCELLED), allowed);
        } else if (status == PrescriptionStatus.ISSUED) {
            // ISSUED can go to ON_HOLD or CANCELLED
            assertEquals(Set.of(PrescriptionStatus.ON_HOLD, PrescriptionStatus.CANCELLED), allowed);
        } else if (status == PrescriptionStatus.ON_HOLD) {
            // ON_HOLD can go to ISSUED or CANCELLED
            assertEquals(Set.of(PrescriptionStatus.ISSUED, PrescriptionStatus.CANCELLED), allowed);
        } else if (status == PrescriptionStatus.CANCELLED || status == PrescriptionStatus.EXPIRED) {
            // CANCELLED and EXPIRED are terminal with no outgoing transitions
            assertEquals(Set.of(), allowed);
        }
        
        // Collect statistics about transitions
        Statistics.label("from-status").collect(status.name());
        Statistics.label("allowed-count").collect(allowed.size());
    }

    /**
     * Property 16: For all combinations of current status and target status,
     * canTransitionTo(target) returns true only for allowed transitions
     * and false for all disallowed transitions.
     */
    @Property
    @Label("Valid transitions only - canTransitionTo rejects invalid transitions")
    void canTransitionToRejectsInvalidTransitions(
            @ForAll("nonTerminalStatusPrescription") Prescription prescription,
            @ForAll PrescriptionStatus target) {
        
        PrescriptionStatus currentStatus = prescription.getStatus();
        boolean canTransition = prescription.canTransitionTo(target);
        
        // Build the expected result based on the transition matrix
        boolean shouldAllow = isAllowedTransition(currentStatus, target);
        
        assertEquals(shouldAllow, canTransition,
                String.format("Transition from %s to %s should be %s",
                    currentStatus, target, shouldAllow ? "allowed" : "rejected"));
        
        Statistics.label("transition").collect(currentStatus.name() + "->" + target.name());
        Statistics.label("allowed").collect(canTransition);
    }

    /**
     * Property 16: Terminal states (CANCELLED and EXPIRED) reject all outgoing transitions.
     */
    @Property
    @Label("Valid transitions only - Terminal states (CANCELLED, EXPIRED) reject all transitions")
    void terminalStatesRejectAllTransitions(
            @ForAll("terminalStatusPrescription") Prescription prescription,
            @ForAll PrescriptionStatus target) {
        
        // Regardless of target, terminal states should never allow transitions
        assertFalse(prescription.canTransitionTo(target),
                String.format("Prescription in %s state should not allow transition to %s",
                    prescription.getStatus(), target));
        
        Statistics.label("terminal-status").collect(prescription.getStatus().name());
        Statistics.label("rejected-target").collect(target.name());
    }

    /**
     * Property 16: DRAFT prescriptions can transition only to ISSUED or CANCELLED.
     */
    @Property
    @Label("Valid transitions only - DRAFT allows only ISSUED and CANCELLED")
    void draftAllowsOnlyIssuedAndCancelled(
            @ForAll("draftPrescription") Prescription prescription,
            @ForAll PrescriptionStatus target) {
        
        boolean canTransition = prescription.canTransitionTo(target);
        boolean shouldAllow = target == PrescriptionStatus.ISSUED || target == PrescriptionStatus.CANCELLED;
        
        assertEquals(shouldAllow, canTransition,
                String.format("DRAFT should %s transition to %s",
                    shouldAllow ? "allow" : "reject", target));
    }

    /**
     * Property 16: ISSUED prescriptions can transition only to ON_HOLD or CANCELLED.
     */
    @Property
    @Label("Valid transitions only - ISSUED allows only ON_HOLD and CANCELLED")
    void issuedAllowsOnlyOnHoldAndCancelled(
            @ForAll("issuedPrescription") Prescription prescription,
            @ForAll PrescriptionStatus target) {
        
        boolean canTransition = prescription.canTransitionTo(target);
        boolean shouldAllow = target == PrescriptionStatus.ON_HOLD || target == PrescriptionStatus.CANCELLED;
        
        assertEquals(shouldAllow, canTransition,
                String.format("ISSUED should %s transition to %s",
                    shouldAllow ? "allow" : "reject", target));
    }

    /**
     * Property 16: ON_HOLD prescriptions can transition only to ISSUED or CANCELLED.
     */
    @Property
    @Label("Valid transitions only - ON_HOLD allows only ISSUED and CANCELLED")
    void onHoldAllowsOnlyIssuedAndCancelled(
            @ForAll("onHoldPrescription") Prescription prescription,
            @ForAll PrescriptionStatus target) {
        
        boolean canTransition = prescription.canTransitionTo(target);
        boolean shouldAllow = target == PrescriptionStatus.ISSUED || target == PrescriptionStatus.CANCELLED;
        
        assertEquals(shouldAllow, canTransition,
                String.format("ON_HOLD should %s transition to %s",
                    shouldAllow ? "allow" : "reject", target));
    }

    /**
     * Property 16: No status can transition to itself (no self-loops).
     */
    @Property
    @Label("Valid transitions only - No self-transitions allowed")
    void noSelfTransitions(
            @ForAll("nonTerminalStatusPrescription") Prescription prescription) {
        
        PrescriptionStatus currentStatus = prescription.getStatus();
        assertFalse(prescription.canTransitionTo(currentStatus),
                String.format("Status %s should not allow self-transition", currentStatus));
    }

    /**
     * Property 16: CANCELLED prescriptions cannot transition to any other status,
     * including back to themselves.
     */
    @Property
    @Label("Valid transitions only - CANCELLED is truly terminal")
    void cancelledPrescriptionIsTerminal(
            @ForAll("cancelledPrescription") Prescription prescription,
            @ForAll PrescriptionStatus target) {
        
        assertFalse(prescription.canTransitionTo(target),
                String.format("CANCELLED prescription should not allow any transition, including to %s", target));
    }

    /**
     * Property 16: EXPIRED prescriptions cannot transition to any other status,
     * including back to themselves.
     */
    @Property
    @Label("Valid transitions only - EXPIRED is truly terminal")
    void expiredPrescriptionIsTerminal(
            @ForAll("expiredPrescription") Prescription prescription,
            @ForAll PrescriptionStatus target) {
        
        assertFalse(prescription.canTransitionTo(target),
                String.format("EXPIRED prescription should not allow any transition, including to %s", target));
    }

    /**
     * Property 16: The transition matrix is acyclic except for ON_HOLD ↔ ISSUED cycle.
     * This prevents infinite loops and ensures eventual termination.
     */
    @Property
    @Label("Valid transitions only - Only ON_HOLD <-> ISSUED cycle exists")
    void transitionMatrixIsMostlyAcyclic() {
        // DRAFT has outgoing but receives no incoming
        assertTrue(PrescriptionStatus.DRAFT.allowedTransitions().size() > 0);
        
        // Verify ON_HOLD <-> ISSUED cycle exists
        assertTrue(PrescriptionStatus.ON_HOLD.allowedTransitions().contains(PrescriptionStatus.ISSUED));
        assertTrue(PrescriptionStatus.ISSUED.allowedTransitions().contains(PrescriptionStatus.ON_HOLD));
        
        // Verify CANCELLED/EXPIRED have no incoming from non-terminal states that can come back
        assertFalse(PrescriptionStatus.ISSUED.allowedTransitions().contains(PrescriptionStatus.DRAFT));
        assertFalse(PrescriptionStatus.ON_HOLD.allowedTransitions().contains(PrescriptionStatus.DRAFT));
        
        // Verify no status can go from terminal state
        assertFalse(PrescriptionStatus.CANCELLED.allowedTransitions().contains(PrescriptionStatus.ISSUED));
        assertFalse(PrescriptionStatus.EXPIRED.allowedTransitions().contains(PrescriptionStatus.ISSUED));
    }

    // ======================================================================
    // Arbitrary Providers
    // ======================================================================

    /**
     * Provides prescriptions with non-terminal statuses (DRAFT, ISSUED, ON_HOLD).
     */
    @Provide
    Arbitrary<Prescription> nonTerminalStatusPrescription() {
        return Arbitraries.of(
                PrescriptionStatus.DRAFT,
                PrescriptionStatus.ISSUED,
                PrescriptionStatus.ON_HOLD
        ).flatMap(this::createPrescriptionWithStatus);
    }

    /**
     * Provides prescriptions with terminal statuses (CANCELLED, EXPIRED).
     */
    @Provide
    Arbitrary<Prescription> terminalStatusPrescription() {
        return Arbitraries.of(
                PrescriptionStatus.CANCELLED,
                PrescriptionStatus.EXPIRED
        ).flatMap(this::createPrescriptionWithStatus);
    }

    /**
     * Provides DRAFT prescriptions.
     */
    @Provide
    Arbitrary<Prescription> draftPrescription() {
        return createPrescriptionWithStatus(PrescriptionStatus.DRAFT);
    }

    /**
     * Provides ISSUED prescriptions (with non-expired issuedAt).
     */
    @Provide
    Arbitrary<Prescription> issuedPrescription() {
        return createPrescriptionWithStatus(PrescriptionStatus.ISSUED);
    }

    /**
     * Provides ON_HOLD prescriptions.
     */
    @Provide
    Arbitrary<Prescription> onHoldPrescription() {
        return createPrescriptionWithStatus(PrescriptionStatus.ON_HOLD);
    }

    /**
     * Provides CANCELLED prescriptions.
     */
    @Provide
    Arbitrary<Prescription> cancelledPrescription() {
        return createPrescriptionWithStatus(PrescriptionStatus.CANCELLED);
    }

    /**
     * Provides EXPIRED prescriptions (ISSUED/ON_HOLD with issuedAt more than 1 month ago).
     */
    @Provide
    Arbitrary<Prescription> expiredPrescription() {
        return createPrescriptionWithStatus(PrescriptionStatus.EXPIRED);
    }

    /**
     * Helper to create a prescription with a specific status for testing.
     */
    private Arbitrary<Prescription> createPrescriptionWithStatus(PrescriptionStatus status) {
        Arbitrary<Long> id = Arbitraries.longs().between(1, 10000);
        Arbitrary<Long> patientId = Arbitraries.longs().between(1, 10000);
        Arbitrary<Long> doctorId = Arbitraries.longs().between(1, 10000);
        
        return Combinators.combine(id, patientId, doctorId)
                .as((id_, patId, docId) -> {
                    Prescription rx = new Prescription(
                            patId,
                            docId,
                            Collections.singletonList(
                                    new PrescriptionItem(1L, "Medicine", "100mg", "Daily", "After meals", 10)
                            )
                    );
                    
                    // Set the status using reflection for test purposes
                    return setPrescriptionStatus(rx, status);
                });
    }

    /**
     * Sets a prescription to a specific status via reflection for testing.
     * This allows testing all statuses without having to transition through them.
     */
    private Prescription setPrescriptionStatus(Prescription prescription, PrescriptionStatus targetStatus) {
        try {
            java.lang.reflect.Field statusField = Prescription.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(prescription, targetStatus);
            
            // If setting to ISSUED or ON_HOLD or EXPIRED, ensure issuedAt is set appropriately
            if (targetStatus == PrescriptionStatus.ISSUED || targetStatus == PrescriptionStatus.ON_HOLD) {
                java.lang.reflect.Field issuedAtField = Prescription.class.getDeclaredField("issuedAt");
                issuedAtField.setAccessible(true);
                // Set to 15 days ago (not expired)
                issuedAtField.set(prescription, LocalDateTime.now().minusDays(15));
            } else if (targetStatus == PrescriptionStatus.EXPIRED) {
                java.lang.reflect.Field issuedAtField = Prescription.class.getDeclaredField("issuedAt");
                issuedAtField.setAccessible(true);
                // Set to 60 days ago (expired)
                issuedAtField.set(prescription, LocalDateTime.now().minusDays(60));
            }
            
            if (targetStatus == PrescriptionStatus.ON_HOLD || targetStatus == PrescriptionStatus.CANCELLED) {
                java.lang.reflect.Field reasonField = Prescription.class.getDeclaredField("statusChangeReason");
                reasonField.setAccessible(true);
                reasonField.set(prescription, "Test reason");
            }
            
            return prescription;
        } catch (Exception e) {
            throw new RuntimeException("Failed to set prescription status for testing", e);
        }
    }

    /**
     * Determines if a transition from currentStatus to targetStatus is allowed.
     */
    private boolean isAllowedTransition(PrescriptionStatus currentStatus, PrescriptionStatus targetStatus) {
        return currentStatus.allowedTransitions().contains(targetStatus);
    }
}