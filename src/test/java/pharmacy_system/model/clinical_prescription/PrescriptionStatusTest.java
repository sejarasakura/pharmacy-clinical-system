package pharmacy_system.model.clinical_prescription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PrescriptionStatus} enum and its allowed transitions.
 * 
 * Validates: Requirements 5.1, 5.5
 */
class PrescriptionStatusTest {

    @Test
    void allowedTransitions_draftToIssuedOrCancelled() {
        Set<PrescriptionStatus> allowed = PrescriptionStatus.DRAFT.allowedTransitions();
        
        assertEquals(2, allowed.size());
        assertTrue(allowed.contains(PrescriptionStatus.ISSUED));
        assertTrue(allowed.contains(PrescriptionStatus.CANCELLED));
        assertFalse(allowed.contains(PrescriptionStatus.ON_HOLD));
        assertFalse(allowed.contains(PrescriptionStatus.EXPIRED));
        assertFalse(allowed.contains(PrescriptionStatus.DRAFT));
    }

    @Test
    void allowedTransitions_issuedToOnHoldOrCancelled() {
        Set<PrescriptionStatus> allowed = PrescriptionStatus.ISSUED.allowedTransitions();
        
        assertEquals(2, allowed.size());
        assertTrue(allowed.contains(PrescriptionStatus.ON_HOLD));
        assertTrue(allowed.contains(PrescriptionStatus.CANCELLED));
        assertFalse(allowed.contains(PrescriptionStatus.ISSUED));
        assertFalse(allowed.contains(PrescriptionStatus.DRAFT));
        assertFalse(allowed.contains(PrescriptionStatus.EXPIRED));
    }

    @Test
    void allowedTransitions_onHoldToIssuedOrCancelled() {
        Set<PrescriptionStatus> allowed = PrescriptionStatus.ON_HOLD.allowedTransitions();
        
        assertEquals(2, allowed.size());
        assertTrue(allowed.contains(PrescriptionStatus.ISSUED));
        assertTrue(allowed.contains(PrescriptionStatus.CANCELLED));
        assertFalse(allowed.contains(PrescriptionStatus.ON_HOLD));
        assertFalse(allowed.contains(PrescriptionStatus.DRAFT));
        assertFalse(allowed.contains(PrescriptionStatus.EXPIRED));
    }

    @Test
    void allowedTransitions_cancelledNoTransitions() {
        Set<PrescriptionStatus> allowed = PrescriptionStatus.CANCELLED.allowedTransitions();
        
        assertTrue(allowed.isEmpty());
    }

    @Test
    void allowedTransitions_expiredNoTransitions() {
        Set<PrescriptionStatus> allowed = PrescriptionStatus.EXPIRED.allowedTransitions();
        
        assertTrue(allowed.isEmpty());
    }

    @Test
    void allowedTransitions_returnsImmutableSet() {
        Set<PrescriptionStatus> allowed = PrescriptionStatus.DRAFT.allowedTransitions();
        
        // Should be immutable
        assertThrows(UnsupportedOperationException.class, () -> allowed.add(PrescriptionStatus.ON_HOLD));
    }

    @ParameterizedTest
    @EnumSource(PrescriptionStatus.class)
    void allowedTransitions_neverContainsSelf(PrescriptionStatus status) {
        Set<PrescriptionStatus> allowed = status.allowedTransitions();
        assertFalse(allowed.contains(status), status + " should not transition to itself");
    }

    @Test
    void allowedTransitions_draftCannotTransitionToOnHold() {
        Set<PrescriptionStatus> allowed = PrescriptionStatus.DRAFT.allowedTransitions();
        assertFalse(allowed.contains(PrescriptionStatus.ON_HOLD), 
            "DRAFT should not be able to transition directly to ON_HOLD (must go through ISSUED first)");
    }

    @Test
    void allowedTransitions_draftCannotTransitionToExpired() {
        Set<PrescriptionStatus> allowed = PrescriptionStatus.DRAFT.allowedTransitions();
        assertFalse(allowed.contains(PrescriptionStatus.EXPIRED), 
            "DRAFT should not be able to transition to EXPIRED (expiry is derived, not a direct transition)");
    }

    @Test
    void allowedTransitions_issuedCannotTransitionToDraft() {
        Set<PrescriptionStatus> allowed = PrescriptionStatus.ISSUED.allowedTransitions();
        assertFalse(allowed.contains(PrescriptionStatus.DRAFT), 
            "ISSUED should not be able to transition back to DRAFT (one-way transition)");
    }

    @Test
    void allowedTransitions_onHoldCannotTransitionToDraft() {
        Set<PrescriptionStatus> allowed = PrescriptionStatus.ON_HOLD.allowedTransitions();
        assertFalse(allowed.contains(PrescriptionStatus.DRAFT), 
            "ON_HOLD should not be able to transition to DRAFT");
    }

    @Test
    void allowedTransitions_issuedCannotTransitionToExpired() {
        Set<PrescriptionStatus> allowed = PrescriptionStatus.ISSUED.allowedTransitions();
        assertFalse(allowed.contains(PrescriptionStatus.EXPIRED), 
            "ISSUED should not be able to transition to EXPIRED (expiry is derived, not a direct transition)");
    }

    @Test
    void allowedTransitions_onHoldCannotTransitionToExpired() {
        Set<PrescriptionStatus> allowed = PrescriptionStatus.ON_HOLD.allowedTransitions();
        assertFalse(allowed.contains(PrescriptionStatus.EXPIRED), 
            "ON_HOLD should not be able to transition to EXPIRED (expiry is derived, not a direct transition)");
    }

    @Test
    void allowedTransitions_consistencyWithRequirements() {
        // Based on Requirements 5.1: DRAFT→ISSUED|CANCELLED; ISSUED→ON_HOLD|CANCELLED; ON_HOLD→ISSUED|CANCELLED
        
        assertEquals(Set.of(PrescriptionStatus.ISSUED, PrescriptionStatus.CANCELLED), 
            PrescriptionStatus.DRAFT.allowedTransitions());
        
        assertEquals(Set.of(PrescriptionStatus.ON_HOLD, PrescriptionStatus.CANCELLED), 
            PrescriptionStatus.ISSUED.allowedTransitions());
        
        assertEquals(Set.of(PrescriptionStatus.ISSUED, PrescriptionStatus.CANCELLED), 
            PrescriptionStatus.ON_HOLD.allowedTransitions());
        
        assertEquals(Set.of(), PrescriptionStatus.CANCELLED.allowedTransitions());
        assertEquals(Set.of(), PrescriptionStatus.EXPIRED.allowedTransitions());
    }
}