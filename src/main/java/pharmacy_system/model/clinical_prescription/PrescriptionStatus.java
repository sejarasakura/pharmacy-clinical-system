package pharmacy_system.model.clinical_prescription;

import java.util.Collections;
import java.util.Set;

/**
 * Enumeration of clinical prescription lifecycle statuses.
 * Represents the clinical workflow state of a prescription.
 * 
 * Status transitions:
 * - DRAFT: initial state, hidden from patients, editable by doctor
 * - ISSUED: prescription released for dispensing, may transition to ON_HOLD or CANCELLED
 * - ON_HOLD: temporarily suspended, may resume to ISSUED or transition to CANCELLED
 * - CANCELLED: terminal state, cannot be modified
 * - EXPIRED: derived state (ISSUED/ON_HOLD past 1 month), terminal
 * 
 * [UCD-07]
 */
public enum PrescriptionStatus {
    DRAFT,      // editable, hidden from patients
    ISSUED,     // active for dispensing
    ON_HOLD,    // temporarily suspended
    CANCELLED,  // terminal
    EXPIRED;    // terminal (derived, not user-set)

    /**
     * Get the set of allowed target statuses for this status.
     * Implements FR-020, SC-005.
     * 
     * @return set of statuses this status can transition to
     */
    public Set<PrescriptionStatus> allowedTransitions() {
        return switch (this) {
            case DRAFT -> Set.of(ISSUED, CANCELLED);
            case ISSUED -> Set.of(ON_HOLD, CANCELLED);
            case ON_HOLD -> Set.of(ISSUED, CANCELLED);
            case CANCELLED, EXPIRED -> Collections.emptySet();  // terminal states
        };
    }

    /**
     * Check if this status is terminal (no further transitions allowed).
     * @return true if status is CANCELLED or EXPIRED, false otherwise
     */
    public boolean isTerminal() {
        return this == CANCELLED || this == EXPIRED;
    }

    /**
     * Check if this status allows editing.
     * Only DRAFT allows full editing; ISSUED/ON_HOLD may allow limited edits.
     * @return true if status allows editing, false otherwise
     */
    public boolean isEditable() {
        return this == DRAFT;
    }

    /**
     * Check if this status allows dispensing.
     * @return true if status is ISSUED or ON_HOLD, false otherwise
     */
    public boolean isDispensable() {
        return this == ISSUED || this == ON_HOLD;
    }
}
