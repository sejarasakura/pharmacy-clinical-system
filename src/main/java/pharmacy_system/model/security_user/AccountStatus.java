package pharmacy_system.model.security_user;

/**
 * Enumeration of account lifecycle statuses.
 * Represents the administrative state of a user account.
 * 
 * Status transitions:
 * - PENDING: initial state, awaiting administrator approval
 * - ACTIVE: approved and operational; can authenticate
 * - DISABLED: administratively disabled; cannot authenticate
 * - LOCKED: temporarily locked due to failed authentication attempts; cannot authenticate
 */
public enum AccountStatus {
    PENDING,   // awaiting approval
    ACTIVE,    // operational
    DISABLED,  // administratively disabled
    LOCKED     // temporarily locked
}
