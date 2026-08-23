package pharmacy_system.controller.security_user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pharmacy_system.controller.common.NavigationController;
import pharmacy_system.controller.common.SessionController;
import pharmacy_system.model.security_user.PasswordHasher;
import pharmacy_system.model.security_user.RolePermission;
import pharmacy_system.model.security_user.Sha256PasswordHasher;
import pharmacy_system.model.security_user.UserAccount;
import pharmacy_system.storage.security_user.InMemoryCredentialStorage;
import pharmacy_system.storage.security_user.InMemoryRolePermissionStorage;
import pharmacy_system.storage.security_user.InMemoryUserAccountStorage;
import pharmacy_system.storage.security_user.InMemoryUserProfileStorage;

class AuthenticationWorkflowTest {
    private InMemoryUserAccountStorage accounts;
    private InMemoryCredentialStorage credentials;
    private InMemoryRolePermissionStorage roles;
    private ManageUserAccountController accountController;
    private AuthenticateAuthoriseController authenticationController;

    @BeforeEach
    void setUp() {
        PasswordHasher hasher = new Sha256PasswordHasher();
        accounts = new InMemoryUserAccountStorage();
        credentials = new InMemoryCredentialStorage(hasher);
        roles = new InMemoryRolePermissionStorage();
        SessionController session = new SessionController(60);
        NavigationController navigation = new NavigationController(session);

        RolePermission administrator = roles.create(new RolePermission(
                "Administrator", Set.of("MANAGE_USER_ACCOUNT"), true));
        roles.create(new RolePermission("Doctor", Set.of("MANAGE_PROFILE"), true));
        UserAccount admin = new UserAccount(0L, "admin", "admin@example.test");
        admin.approveRegistration();
        admin = accounts.create(admin);
        roles.assignRoleToUser(admin.getUserId(), administrator.getRoleId());
        session.establishSession(admin, roles.findRolesByUserId(admin.getUserId()));

        accountController = new ManageUserAccountController(
                accounts, new InMemoryUserProfileStorage(), roles, session, credentials, hasher);
        authenticationController = new AuthenticateAuthoriseController(
                session, navigation, accounts, credentials, roles, hasher);
    }

    @Test
    void doctorCanUseInitialPasswordAfterApproval() {
        UserAccount doctor = accountController.createUserAccount(
                "doctor.one", "doctor.one@example.test", "Doctor", "Doctor@123".toCharArray());

        assertFalse(authenticationController.authenticate("doctor.one", "Doctor@123".toCharArray()));
        assertTrue(accountController.approveRegistration(doctor.getUserId()));
        assertTrue(authenticationController.authenticate("doctor.one", "Doctor@123".toCharArray()));
    }

    @Test
    void issuedRecoveryTokenChangesPasswordOnlyOnce() {
        UserAccount doctor = accountController.createUserAccount(
                "doctor.two", "doctor.two@example.test", "Doctor", "Doctor@123".toCharArray());
        assertTrue(accountController.approveRegistration(doctor.getUserId()));

        String token = authenticationController.issuePasswordReset("doctor.two").orElseThrow();
        assertTrue(authenticationController.resetPassword(token, "Replacement@123".toCharArray()));
        assertFalse(authenticationController.resetPassword(token, "AnotherPassword@123".toCharArray()));
        assertFalse(authenticationController.authenticate("doctor.two", "Doctor@123".toCharArray()));
        assertTrue(authenticationController.authenticate("doctor.two", "Replacement@123".toCharArray()));
    }
}
