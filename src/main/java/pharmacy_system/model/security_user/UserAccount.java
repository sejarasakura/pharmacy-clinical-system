package pharmacy_system.model.security_user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a system user account with authentication state, authorization role,
 * lifecycle status, and audit timestamps.
 * 
 * One account = one username + one operational role (Patient, Doctor, Pharmacist, Administrator).
 * Lifecycle: PENDING (awaiting approval) -> ACTIVE (operational) or DISABLED (admin deactivation)
 * or LOCKED (temporary lockout from failed auth attempts).
 * 
 * [UCD-04, UCD-06]
 */
public class UserAccount {
    private long userId;
    private String username;
    private String email;
    private AccountStatus status;
    private boolean registrationApproved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime disabledAt;
    private String disabledReason;
    private long version;  // optimistic locking

    // Constructor
    public UserAccount(long userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.status = AccountStatus.PENDING;
        this.registrationApproved = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lastLoginAt = null;
        this.disabledAt = null;
        this.disabledReason = null;
        this.version = 0L;
    }

    public UserAccount(long userId, String username, String email, AccountStatus status,
                       boolean registrationApproved, LocalDateTime createdAt,
                       LocalDateTime updatedAt, LocalDateTime lastLoginAt,
                       LocalDateTime disabledAt, String disabledReason, long version) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.status = status;
        this.registrationApproved = registrationApproved;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLoginAt = lastLoginAt;
        this.disabledAt = disabledAt;
        this.disabledReason = disabledReason;
        this.version = version;
    }

    // Getters
    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public boolean isRegistrationApproved() {
        return registrationApproved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getDisabledAt() {
        return disabledAt;
    }

    public String getDisabledReason() {
        return disabledReason;
    }

    public long getVersion() {
        return version;
    }

    // Setters
    public void setEmail(String email) {
        if (email != null && !email.isBlank()) {
            this.email = email;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementVersion() {
        this.version++;
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic
    /**
     * Check if account is active and eligible for authentication.
     * @return true if status is ACTIVE, approved, and not locked, false otherwise
     */
    public boolean isActive() {
        return status == AccountStatus.ACTIVE && registrationApproved;
    }

    /**
     * Check if account can authenticate (not disabled, not locked, active).
     * @return true if account can authenticate, false otherwise
     */
    public boolean canAuthenticate() {
        if (status == AccountStatus.DISABLED || status == AccountStatus.LOCKED) {
            return false;
        }
        if (!registrationApproved || status != AccountStatus.ACTIVE) {
            return false;
        }
        return true;
    }

    /**
     * Approve pending registration (transitions PENDING -> ACTIVE).
     */
    public void approveRegistration() {
        if (status == AccountStatus.PENDING) {
            this.registrationApproved = true;
            this.status = AccountStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
            this.version++;
        }
    }

    /**
     * Enable a disabled account (transitions DISABLED -> ACTIVE).
     */
    public void enable() {
        if (status == AccountStatus.DISABLED) {
            this.status = AccountStatus.ACTIVE;
            this.disabledAt = null;
            this.disabledReason = null;
            this.updatedAt = LocalDateTime.now();
            this.version++;
        }
    }

    /**
     * Disable account (transitions ACTIVE/PENDING -> DISABLED).
     * Requires a reason for audit trail.
     * @param reason the reason for disabling
     */
    public void disable(String reason) {
        if (status != AccountStatus.DISABLED) {
            this.status = AccountStatus.DISABLED;
            this.disabledAt = LocalDateTime.now();
            this.disabledReason = reason != null ? reason : "No reason provided";
            this.updatedAt = LocalDateTime.now();
            this.version++;
        }
    }

    /**
     * Temporarily lock account (transitions to LOCKED, typically from Credential.recordFailedAttempt).
     * Locked accounts cannot authenticate until unlocked or lockout expires.
     */
    public void lock() {
        if (status != AccountStatus.LOCKED) {
            this.status = AccountStatus.LOCKED;
            this.updatedAt = LocalDateTime.now();
            this.version++;
        }
    }

    /**
     * Unlock a locked account (transitions LOCKED -> ACTIVE if originally ACTIVE).
     */
    public void unlock() {
        if (status == AccountStatus.LOCKED) {
            this.status = AccountStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
            this.version++;
        }
    }

    /**
     * Validate required fields for user account creation or update.
     * @return list of validation error messages; empty list if valid
     */
    public List<String> validateRequiredFields() {
        List<String> errors = new ArrayList<>();

        if (username == null || username.isBlank()) {
            errors.add("username is required");
        } else if (username.length() < 3 || username.length() > 50) {
            errors.add("username must be between 3 and 50 characters");
        }

        if (email == null || email.isBlank()) {
            errors.add("email is required");
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.add("email must be of the form local@domain");
        }

        if (status == null) {
            errors.add("Account status is required");
        }

        return errors;
    }

    public boolean hasRequiredFields() { return validateRequiredFields().isEmpty(); }

    public static boolean hasExactlyOneOperationalRole(List<?> roles) {
        return roles != null && roles.size() == 1 && roles.get(0) instanceof RolePermission;
    }

    @Override
    public String toString() {
        return "UserAccount{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", registrationApproved=" + registrationApproved +
                ", version=" + version +
                '}';
    }
}
