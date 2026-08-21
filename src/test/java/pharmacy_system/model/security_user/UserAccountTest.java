package pharmacy_system.model.security_user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests for {@link UserAccount} lifecycle behaviour.
 *
 * <p>Validates: Requirements 1.1, 2.1, 2.2</p>
 */
class UserAccountTest {

    @Test
    void newAccountStartsPendingAndCannotAuthenticate() {
        UserAccount account = new UserAccount(1L, "dr.smith", "dr.smith@example.com");

        assertEquals(AccountStatus.PENDING, account.getStatus());
        assertFalse(account.isRegistrationApproved());
        assertFalse(account.isActive());
        assertFalse(account.canAuthenticate());
        assertEquals(0L, account.getVersion());
    }

    @Test
    void approveRegistrationOnPendingAccountActivatesAndAllowsAuthentication() {
        UserAccount account = new UserAccount(1L, "dr.smith", "dr.smith@example.com");

        account.approveRegistration();

        assertTrue(account.isRegistrationApproved());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertTrue(account.isActive());
        assertTrue(account.canAuthenticate());
        assertEquals(1L, account.getVersion());
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void canAuthenticateRequiresActiveStatusAndApproval(AccountStatus status) {
        UserAccount account = accountWithStatus(status, true);

        boolean expected = status == AccountStatus.ACTIVE;
        assertEquals(expected, account.canAuthenticate());
    }

    @Test
    void canAuthenticateIsFalseWhenActiveButNotApproved() {
        UserAccount account = accountWithStatus(AccountStatus.ACTIVE, false);

        assertFalse(account.canAuthenticate());
    }

    @Test
    void disableRejectsAuthenticationUntilReEnabled() {
        UserAccount account = accountWithStatus(AccountStatus.ACTIVE, true);
        long versionBeforeDisable = account.getVersion();

        account.disable("policy violation");

        assertEquals(AccountStatus.DISABLED, account.getStatus());
        assertFalse(account.canAuthenticate());
        assertEquals("policy violation", account.getDisabledReason());
        assertTrue(account.getDisabledAt() != null);
        assertTrue(account.getVersion() > versionBeforeDisable);

        account.enable();

        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertTrue(account.canAuthenticate());
        assertEquals(null, account.getDisabledReason());
        assertEquals(null, account.getDisabledAt());
    }

    @Test
    void lockPreventsAuthenticationAndUnlockRestoresIt() {
        UserAccount account = accountWithStatus(AccountStatus.ACTIVE, true);

        account.lock();

        assertEquals(AccountStatus.LOCKED, account.getStatus());
        assertFalse(account.canAuthenticate());

        account.unlock();

        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertTrue(account.canAuthenticate());
    }

    @Test
    void validateRequiredFieldsRejectsMissingUsernameAndInvalidEmail() {
        UserAccount missingUsername = new UserAccount(1L, null, "user@example.com");
        UserAccount invalidEmail = new UserAccount(1L, "user", "not-an-email");

        List<String> missingUsernameErrors = missingUsername.validateRequiredFields();
        List<String> invalidEmailErrors = invalidEmail.validateRequiredFields();

        assertTrue(missingUsernameErrors.contains("username is required"));
        assertTrue(invalidEmailErrors.contains("email must be of the form local@domain"));
        assertFalse(missingUsername.hasRequiredFields());
        assertFalse(invalidEmail.hasRequiredFields());
    }

    @Test
    void validateRequiredFieldsAcceptsCompleteAccount() {
        UserAccount account = new UserAccount(1L, "dr.smith", "dr.smith@example.com");

        assertTrue(account.validateRequiredFields().isEmpty());
        assertTrue(account.hasRequiredFields());
    }

    @Test
    void hasExactlyOneOperationalRoleEnforcesSingleRoleInvariant() {
        RolePermission doctorRole = new RolePermission("Doctor", Set.of("PRESCRIPTION_MANAGE"), true);
        RolePermission pharmacistRole = new RolePermission("Pharmacist", Set.of("DISPENSE_MEDICATION"), true);

        assertFalse(UserAccount.hasExactlyOneOperationalRole(List.of()));
        assertTrue(UserAccount.hasExactlyOneOperationalRole(List.of(doctorRole)));
        assertFalse(UserAccount.hasExactlyOneOperationalRole(List.of(doctorRole, pharmacistRole)));
    }

    private static UserAccount accountWithStatus(AccountStatus status, boolean registrationApproved) {
        return new UserAccount(
                1L,
                "dr.smith",
                "dr.smith@example.com",
                status,
                registrationApproved,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null,
                0L);
    }
}
