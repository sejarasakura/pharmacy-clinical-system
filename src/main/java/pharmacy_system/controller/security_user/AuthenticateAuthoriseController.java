package pharmacy_system.controller.security_user;

import pharmacy_system.controller.common.NavigationController;
import pharmacy_system.controller.common.SessionController;
import pharmacy_system.model.security_user.Credential;
import pharmacy_system.model.security_user.PasswordHasher;
import pharmacy_system.model.security_user.RolePermission;
import pharmacy_system.model.security_user.UserAccount;
import pharmacy_system.storage.security_user.CredentialStorage;
import pharmacy_system.storage.security_user.RolePermissionStorage;
import pharmacy_system.storage.security_user.UserAccountStorage;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pharmacy_system.view.common.Flash;

/**
 * Controls authentication, authorisation, and password reset workflows.
 *
 * <p>Implements UCD-04 (Authenticate & Authorise) per the design document.
 * Owns: authentication, session establishment, identity verification, RBAC permission
 * evaluation. Must not own: user creation, role assignment, profile editing.
 *
 * <p>Requirements:
 * <ul>
 *   <li>FR-003/FR-004: Authenticate and authorise role-based access.</li>
 *   <li>FR-005: Enforce password >= 8 chars policy (Requirement 1.5, Property 9).</li>
 *   <li>FR-006: Session expires one hour after establishment.</li>
 *   <li>SC-001..SC-018: Success criteria for authentication workflow.</li>
 * </ul>
 *
 * <p>Key algorithms:
 * <ul>
 *   <li>Authentication: verify credentials against stored hash, track failed attempts,
 *       enforce lockout, establish session on success.</li>
 *   <li>Password reset: validate new password against policy, issue/verify reset token
 *       (token management is deferred to planning; placeholder implementation used).</li>
 *   <li>Session management: delegated to {@link SessionController} and
 *       {@link NavigationController}.</li>
 * </ul>
 */
@Controller
public class AuthenticateAuthoriseController {
    private final SessionController sessionController;
    private final NavigationController navigationController;
    private final UserAccountStorage userAccountStorage;
    private final CredentialStorage credentialStorage;
    private final RolePermissionStorage rolePermissionStorage;
    private final PasswordHasher passwordHasher;

    /**
     * Creates an authentication controller with the required storage and session
     * management dependencies.
     *
     * @param sessionController the session manager (handles session establishment
     *        and expiry)
     * @param navigationController the navigation router (handles role-based view
     *        routing)
     * @param userAccountStorage storage contract for user accounts
     * @param credentialStorage storage contract for credentials
     * @param rolePermissionStorage storage contract for roles and permissions
     * @param passwordHasher the password hashing implementation
     * @throws NullPointerException if any parameter is null
     */
    public AuthenticateAuthoriseController(
            SessionController sessionController,
            NavigationController navigationController,
            UserAccountStorage userAccountStorage,
            CredentialStorage credentialStorage,
            RolePermissionStorage rolePermissionStorage,
            PasswordHasher passwordHasher) {
        this.sessionController = Objects.requireNonNull(sessionController, "sessionController must not be null");
        this.navigationController = Objects.requireNonNull(navigationController, "navigationController must not be null");
        this.userAccountStorage = Objects.requireNonNull(userAccountStorage, "userAccountStorage must not be null");
        this.credentialStorage = Objects.requireNonNull(credentialStorage, "credentialStorage must not be null");
        this.rolePermissionStorage = Objects.requireNonNull(rolePermissionStorage, "rolePermissionStorage must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
    }

    /**
     * Authenticates a user by username and password.
     *
     * <p>Flow (Requirements 1.1, 1.2):
     * <ol>
     *   <li>Look up UserAccount by username.</li>
     *   <li>If not found, return generic failure (do not reveal whether username
     *       or password was wrong).</li>
     *   <li>Check if account can authenticate (status ACTIVE, registration approved).</li>
     *   <li>Load Credential and verify password hash.</li>
     *   <li>If password mismatch, record failed attempt, apply lockout if threshold
     *       exceeded, and return generic failure.</li>
     *   <li>If password correct, reset failed attempts, load role permissions,
     *       establish session, and return success.</li>
     * </ol>
     *
     * @param username the username to authenticate
     * @param password the raw password characters submitted for authentication
     * @return {@code true} if authentication succeeded and session is established;
     *         {@code false} if credentials do not match or account cannot authenticate
     */
    public boolean authenticate(String username, char[] password) {
        // Input validation (Requirements 1.2: generic failure on any mismatch)
        if (username == null || username.isBlank() || password == null) {
            return false;
        }

        // Look up account by username (Requirement 1.2: generic failure if not found)
        Optional<UserAccount> accountOpt = userAccountStorage.findByUsername(username);
        if (accountOpt.isEmpty()) {
            return false;
        }
        UserAccount account = accountOpt.get();

        // Check if account can authenticate (must be ACTIVE + approved)
        // (Requirement 1.2: generic failure if account cannot authenticate)
        if (!account.canAuthenticate()) {
            return false;
        }

        // Load and verify credential
        Optional<Credential> credentialOpt = credentialStorage.findByUserId(account.getUserId());
        if (credentialOpt.isEmpty()) {
            // This should not happen in normal operation; treat as authentication failure
            return false;
        }
        Credential credential = credentialOpt.get();

        // Verify password (returns false if credential is temporarily locked or
        // password does not match)
        if (!credential.verifyPassword(password)) {
            // Record failed attempt and apply lockout if threshold exceeded
            credential.recordFailedAttempt();
            // Update credential in storage, checking version
            credentialStorage.update(credential, credential.getVersion());
            return false;
        }

        // Password verified: reset failed attempts
        credential.resetFailedAttempts();
        credentialStorage.update(credential, credential.getVersion());

        // Load roles for the authenticated user
        List<RolePermission> roles = rolePermissionStorage.findRolesByUserId(account.getUserId());

        // Establish session (FR-006: session expires one hour after establishment)
        sessionController.establishSession(account, roles);

        return true;
    }

    /**
     * Checks whether a user with the given user ID holds the specified permission.
     *
     * <p>This method performs the authorisation check at the time of call; it is
     * used by controllers to verify permission before allowing protected operations.
     * The session-level check is separate (via {@code SessionController.requirePermission()}).
     *
     * <p>Requirement 1.3: If an authenticated user attempts to access a protected
     * function for which the account's assigned Operational_Role lacks permission,
     * the Authorisation_Service SHALL deny execution.
     *
     * @param userId the user ID to authorise
     * @param permissionCode the permission code to check
     * @return {@code true} if the user holds the permission; {@code false} otherwise
     */
    public boolean authorise(long userId, String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }

        // Load roles for the user
        List<RolePermission> roles = rolePermissionStorage.findRolesByUserId(userId);
        if (roles.isEmpty()) {
            return false;
        }

        // Check if any active role grants the permission
        for (RolePermission role : roles) {
            if (role.hasPermission(permissionCode)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Requests a password reset by email address.
     *
     * <p>Flow:
     * <ol>
     *   <li>Look up UserAccount by email.</li>
     *   <li>If found, generate a reset token and store it in the Credential.</li>
     *   <li>Return success if the account exists (to avoid username enumeration);
     *       actual token delivery is deferred to the View layer or external
     *       notification service.</li>
     * </ol>
     *
     * <p>Note: Token generation and delivery are deferred to planning. This
     * implementation stores a placeholder reset token; production implementations
     * should use cryptographically secure token generation and out-of-band delivery
     * (email, SMS, etc.).
     *
     * @param email the email address associated with the account
     * @return {@code true} if an account with this email exists (whether or not
     *         token delivery succeeds); {@code false} if no account is found
     */
    public boolean requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        Optional<UserAccount> accountOpt = userAccountStorage.findByEmail(email);
        if (accountOpt.isEmpty()) {
            return false; // No account with this email
        }

        UserAccount account = accountOpt.get();
        Optional<Credential> credentialOpt = credentialStorage.findByUserId(account.getUserId());
        if (credentialOpt.isEmpty()) {
            return false; // Should not happen; no credential for this account
        }

        Credential credential = credentialOpt.get();

        // Generate a placeholder reset token (deferred to planning for real implementation)
        // In a real system, this would be a cryptographically secure token with expiry
        String resetToken = generateResetToken(account.getUserId(), email);

        // Store the reset token in the credential (placeholder implementation)
        // Token expiry would be stored in a dedicated field (deferred)
        credential.changePasswordHash(resetToken); // Temporary storage (deferred to planning)

        // Update credential in storage
        credentialStorage.update(credential, credential.getVersion());

        return true;
    }

    /**
     * Resets a user's password using a reset token.
     *
     * <p>Flow:
     * <ol>
     *   <li>Validate the reset token format (placeholder).</li>
     *   <li>Validate the new password against the password policy (>= 8 chars).</li>
     *   <li>If invalid, return false (Requirement 1.5).</li>
     *   <li>Find the account associated with the token (placeholder logic).</li>
     *   <li>Update the credential with the new password hash.</li>
     *   <li>Return success.</li>
     * </ol>
     *
     * <p>Requirement 1.5: IF a submitted new or changed password contains fewer than
     * eight characters, THE System SHALL reject the password.
     *
     * @param token the reset token (placeholder implementation; real implementation
     *        would verify cryptographic integrity and expiry)
     * @param newPassword the raw new password characters
     * @return {@code true} if the password reset succeeded; {@code false} if the
     *         token is invalid, the password fails validation, or no account is found
     */
    public boolean resetPassword(String token, char[] newPassword) {
        // Validate token (placeholder; real implementation would verify signature
        // and expiry)
        if (token == null || token.isBlank()) {
            return false;
        }

        // Validate new password (Requirement 1.5: >= 8 chars, Property 9)
        List<String> validationErrors = Credential.validateNewPassword(newPassword);
        if (!validationErrors.isEmpty()) {
            return false;
        }

        // Placeholder: extract user ID from token (real implementation would verify
        // cryptographic integrity)
        // For now, assume token contains user ID encoded (very simplified)
        long userId = decodeUserIdFromToken(token);
        if (userId <= 0) {
            return false; // Invalid token
        }

        // Load credential and update password
        Optional<Credential> credentialOpt = credentialStorage.findByUserId(userId);
        if (credentialOpt.isEmpty()) {
            return false;
        }

        Credential credential = credentialOpt.get();
        try {
            credential.changePassword(newPassword);
        } catch (IllegalArgumentException e) {
            // Password validation failed (Requirement 1.5)
            return false;
        }

        // Update credential in storage
        credentialStorage.update(credential, credential.getVersion());
        return true;
    }

    /**
     * Logs out the current authenticated user by invalidating the session.
     *
     * <p>After logout, all protected operations require re-authentication.
     */
    public void logout() {
        sessionController.invalidateSession();
        navigationController.navigateToLogin();
    }

    @GetMapping("/login")
    public String login(Model model, @RequestParam(required = false) String returnTo) {
        model.addAttribute("title", "Sign in");
        model.addAttribute("returnTo", safeReturnTo(returnTo));
        return "auth/login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam(defaultValue = "") String identifier,
                              @RequestParam(defaultValue = "") String password,
                              @RequestParam(required = false) String returnTo,
                              Model model) {
        boolean invalid = false;
        if (identifier.isBlank()) {
            model.addAttribute("identifierError", "Identifier is required.");
            invalid = true;
        }
        if (password.isBlank()) {
            model.addAttribute("passwordError", "Password is required.");
            invalid = true;
        }
        if (invalid) {
            model.addAttribute("identifier", identifier);
            model.addAttribute("title", "Sign in");
            model.addAttribute("returnTo", safeReturnTo(returnTo));
            return "auth/login";
        }
        if (!authenticate(identifier, password.toCharArray())) {
            model.addAttribute("identifier", identifier);
            model.addAttribute("authenticationError", "Sign-in failed. Check your details and try again.");
            model.addAttribute("title", "Sign in");
            model.addAttribute("returnTo", safeReturnTo(returnTo));
            return "auth/login";
        }
        String target = safeReturnTo(returnTo);
        return "redirect:" + (target == null
                ? navigationController.homeFor(sessionController.getCurrentRole()) : target);
    }

    @GetMapping("/password/recovery")
    public String passwordRecovery(Model model) {
        model.addAttribute("title", "Recover password");
        return "auth/password-recovery";
    }

    @PostMapping("/password/recovery")
    public String passwordRecoverySubmit(@RequestParam(defaultValue = "") String identity, Model model) {
        requestPasswordReset(identity);
        model.addAttribute("identity", identity);
        model.addAttribute("title", "Recover password");
        model.addAttribute("recoveryMessage",
                "If an account matches those details, password-reset instructions will be sent.");
        return "auth/password-recovery";
    }

    @GetMapping("/password/reset")
    public String passwordReset(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("title", "Reset password");
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/password/reset")
    public String passwordResetSubmit(@RequestParam(defaultValue = "") String token,
                                      @RequestParam(defaultValue = "") String newPassword,
                                      @RequestParam(defaultValue = "") String confirmPassword,
                                      Model model, RedirectAttributes redirect) {
        boolean invalid = false;
        if (newPassword.length() < Credential.MIN_PASSWORD_LENGTH) {
            model.addAttribute("newPasswordError", "Password must contain at least 8 characters.");
            invalid = true;
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("confirmPasswordError", "Password confirmation does not match.");
            invalid = true;
        }
        if (invalid || !resetPassword(token, newPassword.toCharArray())) {
            model.addAttribute("title", "Reset password");
            model.addAttribute("token", token);
            if (!invalid) model.addAttribute("resetError", "The reset link is invalid or has expired.");
            return "auth/reset-password";
        }
        redirect.addFlashAttribute("flash", new Flash("success", "Password reset. You can now sign in."));
        return "redirect:/login";
    }

    @GetMapping("/profile/password")
    public String changePassword(Model model) {
        sessionController.requirePermission("MANAGE_PROFILE");
        model.addAttribute("title", "Change password");
        model.addAttribute("breadcrumb", "My Profile / Change password");
        return "profile/change-password";
    }

    @PostMapping("/profile/password")
    public String changePasswordSubmit(@RequestParam(defaultValue = "") String currentPassword,
                                       @RequestParam(defaultValue = "") String newPassword,
                                       @RequestParam(defaultValue = "") String confirmPassword,
                                       Model model, RedirectAttributes redirect) {
        sessionController.requirePermission("MANAGE_PROFILE");
        Optional<Credential> credential = credentialStorage.findByUserId(sessionController.getCurrentUserId());
        if (newPassword.length() < 8) model.addAttribute("newPasswordError", "Password must contain at least 8 characters.");
        if (!newPassword.equals(confirmPassword)) model.addAttribute("confirmPasswordError", "Password confirmation does not match.");
        if (credential.isEmpty() || !credential.get().verifyPassword(currentPassword.toCharArray())) {
            model.addAttribute("passwordError", "Password change failed. Check your details and try again.");
        }
        if (model.containsAttribute("newPasswordError") || model.containsAttribute("confirmPasswordError")
                || model.containsAttribute("passwordError")) {
            model.addAttribute("title", "Change password");
            model.addAttribute("breadcrumb", "My Profile / Change password");
            return "profile/change-password";
        }
        Credential value = credential.get();
        value.changePassword(newPassword.toCharArray());
        credentialStorage.update(value, value.getVersion());
        redirect.addFlashAttribute("flash", new Flash("success", "Password changed."));
        return "redirect:/profile";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("title", "Access denied");
        model.addAttribute("breadcrumb", "Access denied");
        model.addAttribute("homeHref", navigationController.homeFor(sessionController.getCurrentRole()));
        return "auth/access-denied";
    }

    private String safeReturnTo(String returnTo) {
        return returnTo != null && returnTo.startsWith("/") && !returnTo.startsWith("//")
                && !returnTo.contains("://") ? returnTo : null;
    }

    // ===== Private helpers =====

    /**
     * Generates a placeholder reset token. In a production system, this would
     * be a cryptographically secure token with proper expiry and integrity
     * verification. This placeholder is for demonstration only.
     *
     * @param userId the user ID to encode in the token
     * @param email the email address for cross-reference
     * @return a placeholder reset token
     */
    private String generateResetToken(long userId, String email) {
        // Placeholder: simple concatenation (NOT secure; deferred to planning)
        return "reset_" + userId + "_" + email.hashCode() + "_" + System.currentTimeMillis();
    }

    /**
     * Decodes a user ID from a reset token. In a production system, this would
     * verify cryptographic integrity and expiry. This placeholder extracts the
     * user ID from the token format generated by {@link #generateResetToken}.
     *
     * @param token the reset token
     * @return the extracted user ID, or -1 if invalid
     */
    private long decodeUserIdFromToken(String token) {
        // Placeholder: extract user ID from token format "reset_<userId>_..."
        try {
            if (token.startsWith("reset_")) {
                String[] parts = token.split("_");
                if (parts.length >= 2) {
                    return Long.parseLong(parts[1]);
                }
            }
        } catch (NumberFormatException e) {
            // Invalid token format
        }
        return -1;
    }
}
