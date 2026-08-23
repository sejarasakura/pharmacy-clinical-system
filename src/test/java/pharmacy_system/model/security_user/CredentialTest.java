package pharmacy_system.model.security_user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Credential} password verification, lockout, and policy.
 *
 * <p>Validates: Requirements 1.2, 1.5</p>
 */
class CredentialTest {

    private PasswordHasher passwordHasher;
    private Credential credential;

    @BeforeEach
    void setUp() {
        passwordHasher = new Sha256PasswordHasher();
        credential = new Credential(1L, 100L, "password123".toCharArray(), passwordHasher);
    }

    @Test
    void verifyPasswordReturnsTrueForCorrectPassword() {
        char[] correctPassword = "password123".toCharArray();
        assertTrue(credential.verifyPassword(correctPassword));
    }

    @Test
    void verifyPasswordReturnsFalseForIncorrectPassword() {
        char[] wrongPassword = "wrongpassword".toCharArray();
        assertFalse(credential.verifyPassword(wrongPassword));
    }

    @Test
    void verifyPasswordReturnsFalseForNullPassword() {
        assertFalse(credential.verifyPassword(null));
    }

    @Test
    void verifyPasswordFailsWhenTemporarilyLocked() {
        Credential lockedCredential = new Credential(
                1L, 100L, credential.getPasswordHash(), 5, LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now(), passwordHasher);

        char[] password = "password123".toCharArray();
        assertFalse(lockedCredential.verifyPassword(password));
        assertTrue(lockedCredential.isTemporarilyLocked());
    }

    @Test
    void recordFailedAttemptIncrementsCounter() {
        assertEquals(0, credential.getFailedAttempts());

        credential.recordFailedAttempt();
        assertEquals(1, credential.getFailedAttempts());

        credential.recordFailedAttempt();
        credential.recordFailedAttempt();
        assertEquals(3, credential.getFailedAttempts());
    }

    @Test
    void recordFailedAttemptLocksAfterMaxAttempts() {
        for (int i = 0; i < Credential.MAX_FAILED_ATTEMPTS; i++) {
            credential.recordFailedAttempt();
        }

        assertEquals(Credential.MAX_FAILED_ATTEMPTS, credential.getFailedAttempts());
        assertNotNull(credential.getLockedUntil());
        assertTrue(credential.isTemporarilyLocked());
    }

    @Test
    void resetFailedAttemptsClearsLockout() {
        for (int i = 0; i < Credential.MAX_FAILED_ATTEMPTS; i++) {
            credential.recordFailedAttempt();
        }
        assertTrue(credential.isTemporarilyLocked());

        credential.resetFailedAttempts();

        assertEquals(0, credential.getFailedAttempts());
        assertNull(credential.getLockedUntil());
        assertFalse(credential.isTemporarilyLocked());
    }

    @Test
    void changePasswordEnforcesPasswordPolicy() {
        // Too short password should fail
        char[] shortPassword = "1234567".toCharArray(); // 7 characters
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> credential.changePassword(shortPassword));
        assertTrue(exception.getMessage().contains("password must contain at least " + Credential.MIN_PASSWORD_LENGTH + " characters"));

        // Valid password should succeed
        char[] validPassword = "newpassword123".toCharArray();
        credential.changePassword(validPassword);

        // Verify new password works
        assertTrue(credential.verifyPassword(validPassword));
        assertFalse(credential.verifyPassword("password123".toCharArray()));
    }

    @Test
    void changePasswordClearsFailedAttempts() {
        credential.recordFailedAttempt();
        credential.recordFailedAttempt();
        assertEquals(2, credential.getFailedAttempts());

        char[] newPassword = "newpassword123".toCharArray();
        credential.changePassword(newPassword);

        assertEquals(0, credential.getFailedAttempts());
        assertNull(credential.getLockedUntil());
    }

    @Test
    void validateNewPasswordReturnsErrorForShortPassword() {
        char[] shortPassword = "123".toCharArray();
        List<String> errors = Credential.validateNewPassword(shortPassword);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("password must contain at least " + Credential.MIN_PASSWORD_LENGTH + " characters"));
    }

    @Test
    void validateNewPasswordReturnsEmptyListForValidPassword() {
        char[] validPassword = "password123".toCharArray();
        List<String> errors = Credential.validateNewPassword(validPassword);
        assertTrue(errors.isEmpty());
    }

    @Test
    void isTemporarilyLockedReturnsTrueWhenLocked() {
        LocalDateTime futureTime = LocalDateTime.now().plus(Duration.ofMinutes(5));
        Credential lockedCredential = new Credential(
                1L, 100L, "hash", 5, futureTime, LocalDateTime.now(), passwordHasher);
        assertTrue(lockedCredential.isTemporarilyLocked());
    }

    @Test
    void isTemporarilyLockedReturnsFalseWhenNotLocked() {
        assertFalse(credential.isTemporarilyLocked());
    }

    @Test
    void isTemporarilyLockedReturnsFalseWhenLockExpired() {
        LocalDateTime pastTime = LocalDateTime.now().minus(Duration.ofMinutes(5));
        Credential expiredLockCredential = new Credential(
                1L, 100L, "hash", 5, pastTime, LocalDateTime.now(), passwordHasher);
        assertFalse(expiredLockCredential.isTemporarilyLocked());
    }

    @Test
    void constructorValidatesPasswordPolicy() {
        char[] shortPassword = "123".toCharArray();
        assertThrows(IllegalArgumentException.class,
                () -> new Credential(1L, 100L, shortPassword, passwordHasher));
    }

    @Test
    void changePasswordHashDirectlyUpdatesHash() {
        String newHash = "new-hash-value";
        credential.changePasswordHash(newHash);

        assertEquals(newHash, credential.getPasswordHash());
        assertNotNull(credential.getPasswordUpdatedAt());
        assertEquals(0, credential.getFailedAttempts());
        assertNull(credential.getLockedUntil());
    }

    @Test
    void credentialPropertiesAreCorrectlySet() {
        LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(10);
        LocalDateTime passwordUpdatedAt = LocalDateTime.now().minusDays(1);
        Credential credential = new Credential(
                42L, 99L, "test-hash", 3, lockedUntil, passwordUpdatedAt, passwordHasher);

        assertEquals(42L, credential.getCredentialId());
        assertEquals(99L, credential.getUserId());
        assertEquals("test-hash", credential.getPasswordHash());
        assertEquals(3, credential.getFailedAttempts());
        assertEquals(lockedUntil, credential.getLockedUntil());
        assertEquals(passwordUpdatedAt, credential.getPasswordUpdatedAt());
    }

    @Test
    void resetTokenIsHashedValidatedAndCleared() {
        String token = "100.secure-one-time-token";
        credential.setResetToken(passwordHasher.hash(token.toCharArray()));

        assertTrue(credential.isResetTokenValid(token));
        assertFalse(credential.isResetTokenValid("100.wrong-token"));

        credential.clearResetToken();
        assertFalse(credential.isResetTokenValid(token));
    }

    @Test
    void expiredResetTokenIsRejected() {
        String token = "100.expired-token";
        Credential expired = new Credential(
                1L, 100L, credential.getPasswordHash(), 0, null, LocalDateTime.now(),
                LocalDateTime.now().minusMinutes(1), passwordHasher.hash(token.toCharArray()),
                passwordHasher, 1L);

        assertFalse(expired.isResetTokenValid(token));
    }
}
