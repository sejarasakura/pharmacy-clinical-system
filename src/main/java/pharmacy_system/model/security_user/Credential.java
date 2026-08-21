package pharmacy_system.model.security_user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Authentication credential for a user account.
 * Stores salted password hash (raw passwords never stored) and security state including
 * failed attempt tracking, temporary locking, and password reset token management.
 * 
 * [UCD-04]
 */
public class Credential {
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int MIN_PASSWORD_LENGTH = 8;
    private long credentialId;
    private long userId;
    private String passwordHash;
    private int failedAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime passwordUpdatedAt;
    private LocalDateTime resetTokenExpiry;
    private String resetTokenHash;
    private final PasswordHasher passwordHasher;
    private long version;

    // Constructor
    public Credential(long credentialId, long userId, String passwordHash) {
        this(credentialId, userId, passwordHash, 0, null, LocalDateTime.now(),
                new Sha256PasswordHasher(), 0L);
    }

    public Credential(long credentialId, long userId, char[] rawPassword, PasswordHasher passwordHasher) {
        this(credentialId, userId, hashValidated(rawPassword, passwordHasher), 0, null,
                LocalDateTime.now(), passwordHasher, 0L);
    }

    public Credential(long credentialId, long userId, String passwordHash, int failedAttempts,
                      LocalDateTime lockedUntil, LocalDateTime passwordUpdatedAt,
                      PasswordHasher passwordHasher) {
        this(credentialId, userId, passwordHash, failedAttempts, lockedUntil,
                passwordUpdatedAt, passwordHasher, 0L);
    }

    public Credential(long credentialId, long userId, String passwordHash, int failedAttempts,
                      LocalDateTime lockedUntil, LocalDateTime passwordUpdatedAt,
                      PasswordHasher passwordHasher, long version) {
        this.credentialId = credentialId;
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
        this.passwordUpdatedAt = passwordUpdatedAt;
        this.resetTokenExpiry = null;
        this.resetTokenHash = null;
        this.passwordHasher = passwordHasher;
        this.version = version;
    }

    // Getters
    public long getCredentialId() {
        return credentialId;
    }

    public long getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public LocalDateTime getPasswordUpdatedAt() {
        return passwordUpdatedAt;
    }

    public LocalDateTime getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public String getResetTokenHash() {
        return resetTokenHash;
    }

    public long getVersion() { return version; }

    // Business logic
    /**
     * Verify submitted password against stored hash.
     * Actual hash verification algorithm is deferred to planning; this method signature
     * represents the contract expected by authentication flows.
     * 
     * @param password the plaintext password to verify
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(char[] password) {
        if (password == null || passwordHash == null) {
            return false;
        }
        return !isTemporarilyLocked() && passwordHasher.matches(password, passwordHash);
    }

    /**
     * Record a failed authentication attempt and apply lockout if threshold exceeded.
     * Threshold: 5 failed attempts; lockout duration: 15 minutes.
     */
    public void recordFailedAttempt() {
        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }

    /**
     * Reset failed attempt counter (called on successful authentication).
     */
    public void resetFailedAttempts() {
        failedAttempts = 0;
        lockedUntil = null;
    }

    /**
     * Check if credential is temporarily locked due to failed attempts.
     * @return true if locked and lockout has not yet expired, false otherwise
     */
    public boolean isTemporarilyLocked() {
        if (lockedUntil == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(lockedUntil)) {
            lockedUntil = null;
            return false;
        }
        return true;
    }

    /**
     * Validate a password reset token.
     * @param token the token to validate
     * @return true if token exists, matches hash, and has not expired, false otherwise
     */
    public boolean isResetTokenValid(String token) {
        if (token == null || resetTokenHash == null || resetTokenExpiry == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(resetTokenExpiry)) {
            resetTokenHash = null;
            resetTokenExpiry = null;
            return false;
        }
        // Actual token hash comparison deferred to planning phase
        return false;  // placeholder
    }

    /**
     * Update password hash (called on password change).
     * @param newHash the new salted password hash
     */
    public void changePasswordHash(String newHash) {
        if (newHash != null && !newHash.isBlank()) {
            this.passwordHash = newHash;
            this.passwordUpdatedAt = LocalDateTime.now();
            this.failedAttempts = 0;
            this.lockedUntil = null;
        }
    }

    public void changePassword(char[] newPassword) {
        List<String> errors = validateNewPassword(newPassword);
        if (!errors.isEmpty()) throw new IllegalArgumentException(errors.get(0));
        changePasswordHash(passwordHasher.hash(newPassword));
    }

    public static List<String> validateNewPassword(char[] password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            errors.add("password must contain at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        return errors;
    }

    private static String hashValidated(char[] password, PasswordHasher hasher) {
        List<String> errors = validateNewPassword(password);
        if (!errors.isEmpty()) throw new IllegalArgumentException(errors.get(0));
        return hasher.hash(password);
    }

    /**
     * Set password reset token (issued by AuthenticateAuthoriseController on reset request).
     * Token expires in 24 hours.
     * @param tokenHash the hashed reset token
     */
    public void setResetToken(String tokenHash) {
        this.resetTokenHash = tokenHash;
        this.resetTokenExpiry = LocalDateTime.now().plusHours(24);
    }

    /**
     * Clear password reset token (after successful reset or expiry).
     */
    public void clearResetToken() {
        this.resetTokenHash = null;
        this.resetTokenExpiry = null;
    }

    @Override
    public String toString() {
        return "Credential{" +
                "credentialId=" + credentialId +
                ", userId=" + userId +
                ", failedAttempts=" + failedAttempts +
                ", isLocked=" + isTemporarilyLocked() +
                ", passwordUpdatedAt=" + passwordUpdatedAt +
                '}';
    }
}
