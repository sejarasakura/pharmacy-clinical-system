package pharmacy_system.controller.security_user;

import pharmacy_system.model.security_user.RolePermission;
import pharmacy_system.model.security_user.AccountStatus;
import pharmacy_system.model.security_user.UserAccount;
import pharmacy_system.model.security_user.Credential;
import pharmacy_system.model.security_user.PasswordHasher;
import pharmacy_system.model.security_user.profile.PatientProfile;
import pharmacy_system.model.security_user.profile.UserProfile;
import pharmacy_system.storage.security_user.RolePermissionStorage;
import pharmacy_system.storage.security_user.UserAccountStorage;
import pharmacy_system.storage.security_user.UserProfileStorage;
import pharmacy_system.storage.security_user.CredentialStorage;
import pharmacy_system.controller.common.SessionController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pharmacy_system.view.common.Flash;
import pharmacy_system.view.security_user.ucd06_manage_user_account.UserAccountFormView;
import pharmacy_system.view.security_user.ucd06_manage_user_account.UserAccountListView;

/**
 * Controller for administrative account lifecycle and role management.
 *
 * <p>Implements UCD-06: Manage User Account. Responsible for account creation,
 * registration approval, role assignment/removal, account enable/disable/unlock,
 * and provisioning login access for existing unlinked Patient business records.
 *
 * <p>This controller does NOT handle self-service profile editing (UCD-05) or
 * authentication execution (UCD-04). It enforces the single-operational-role
 * invariant and prevents Doctors from administering credentials (Requirement 3.3).
 *
 * <p>Requirements:
 * <ul>
 *   <li>FR-002, FR-007: Account lifecycle and role administration.</li>
 *   <li>FR-013: Administrative operations gated to the Administrator role.</li>
 *   <li>Requirement 2.1: Account creation with unique ID and mandatory fields.</li>
 *   <li>Requirement 2.2: Account disabling and re-enabling.</li>
 *   <li>Requirement 2.5: Provisioning login for unlinked Patient business records.</li>
 *   <li>Requirement 3.3: Reject Doctor credential administration attempts.</li>
 * </ul>
 */
@Controller
public class ManageUserAccountController {
    private final UserAccountStorage userAccountStorage;
    private final UserProfileStorage userProfileStorage;
    private final RolePermissionStorage rolePermissionStorage;
    private final SessionController sessionController;
    private final CredentialStorage credentialStorage;
    private final PasswordHasher passwordHasher;

    /**
     * Constructs a ManageUserAccountController with the supplied storage and
     * session dependencies.
     *
     * @param userAccountStorage the UserAccount storage (must not be null)
     * @param userProfileStorage the UserProfile storage (must not be null)
     * @param rolePermissionStorage the RolePermission storage (must not be null)
     * @param sessionController the session controller for permission checks (must not be null)
     */
    public ManageUserAccountController(
            UserAccountStorage userAccountStorage,
            UserProfileStorage userProfileStorage,
            RolePermissionStorage rolePermissionStorage,
            SessionController sessionController,
            CredentialStorage credentialStorage,
            PasswordHasher passwordHasher) {
        if (userAccountStorage == null) {
            throw new IllegalArgumentException("userAccountStorage must not be null");
        }
        if (userProfileStorage == null) {
            throw new IllegalArgumentException("userProfileStorage must not be null");
        }
        if (rolePermissionStorage == null) {
            throw new IllegalArgumentException("rolePermissionStorage must not be null");
        }
        if (sessionController == null) {
            throw new IllegalArgumentException("sessionController must not be null");
        }
        if (credentialStorage == null || passwordHasher == null) {
            throw new IllegalArgumentException("credential storage and password hasher must not be null");
        }
        this.userAccountStorage = userAccountStorage;
        this.userProfileStorage = userProfileStorage;
        this.rolePermissionStorage = rolePermissionStorage;
        this.sessionController = sessionController;
        this.credentialStorage = credentialStorage;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Retrieves all user accounts in the system.
     *
     * <p>Requires MANAGE_USER_ACCOUNT permission (Administrator role).
     *
     * @return a list of all UserAccounts
     * @throws SessionController.SessionExpiredException if the session has expired
     * @throws SessionController.InsufficientPermissionException if the session lacks permission
     */
    public List<UserAccount> loadUserAccounts() {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");
        return userAccountStorage.listAll();
    }

    /**
     * Creates a new user account with a unique username, email, and exactly one
     * assigned operational role.
     *
     * <p>Requires MANAGE_USER_ACCOUNT permission (Administrator role).
     *
     * <p>Validation:
     * <ul>
     *   <li>username must be non-empty and unique</li>
     *   <li>email must be valid (local@domain format) and unique</li>
     *   <li>roleName must correspond to an existing, active RolePermission</li>
     *   <li>exactly one role must be assigned (single-operational-role invariant)</li>
     * </ul>
     *
     * <p>Returns null if validation fails; the caller should report the validation
     * error messages via the UI. On success, the account is created in PENDING
     * status and must be approved before authentication is permitted
     * (Requirement 2.1).
     *
     * @param username the unique username
     * @param email the unique email address
     * @param roleName the operational role name (e.g., "Doctor", "Pharmacist")
     * @return the created UserAccount, or null if validation fails
     * @throws SessionController.SessionExpiredException if the session has expired
     * @throws SessionController.InsufficientPermissionException if the session lacks permission
     */
    public UserAccount createUserAccount(String username, String email, String roleName) {
        return createUserAccount(username, email, roleName, null);
    }

    public UserAccount createUserAccount(String username, String email, String roleName, char[] initialPassword) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");

        // Validate mandatory fields
        if (username == null || username.isBlank()) {
            return null; // Validation failed; caller reports error
        }
        if (email == null || email.isBlank()) {
            return null;
        }
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        if (!Credential.validateNewPassword(initialPassword).isEmpty()) return null;

        // Check for duplicate username
        if (userAccountStorage.findByUsername(username).isPresent()) {
            return null; // Username already exists
        }

        // Check for duplicate email
        if (userAccountStorage.findByEmail(email).isPresent()) {
            return null; // Email already exists
        }

        // Verify the role exists and is active
        Optional<RolePermission> roleOpt = rolePermissionStorage.findByRoleName(roleName);
        if (roleOpt.isEmpty() || !roleOpt.get().isActive()) {
            return null; // Role does not exist or is inactive
        }

        // Create the account in PENDING status
        UserAccount account = new UserAccount(0L, username, email);
        UserAccount created = userAccountStorage.create(account);

        // Assign the role
        rolePermissionStorage.assignRoleToUser(created.getUserId(), roleOpt.get().getRoleId());
        try {
            credentialStorage.create(new Credential(0L, created.getUserId(), initialPassword, passwordHasher));
        } catch (RuntimeException exception) {
            rolePermissionStorage.removeRoleFromUser(created.getUserId(), roleOpt.get().getRoleId());
            userAccountStorage.delete(created.getUserId());
            throw exception;
        }

        return created;
    }

    /** Creates the missing first-login credential for an existing account. */
    public boolean setInitialPassword(long userId, char[] initialPassword) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");
        if (!Credential.validateNewPassword(initialPassword).isEmpty()
                || userAccountStorage.findById(userId).isEmpty()
                || credentialStorage.findByUserId(userId).isPresent()) {
            return false;
        }
        credentialStorage.create(new Credential(0L, userId, initialPassword, passwordHasher));
        return true;
    }

    /**
     * Approves a pending user account registration, making it eligible for
     * authentication once the account status is ACTIVE.
     *
     * <p>Requires MANAGE_USER_ACCOUNT permission (Administrator role).
     *
     * <p>Transitions a PENDING account to ACTIVE and sets the approval flag.
     * Other account statuses only have the approval flag recorded without
     * changing status.
     *
     * @param userId the ID of the account to approve
     * @return true if approval succeeds, false if the account does not exist or
     *         version conflict occurs
     * @throws SessionController.SessionExpiredException if the session has expired
     * @throws SessionController.InsufficientPermissionException if the session lacks permission
     */
    public boolean approveRegistration(long userId) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");

        Optional<UserAccount> accountOpt = userAccountStorage.findById(userId);
        if (accountOpt.isEmpty()) {
            return false; // Account not found
        }

        UserAccount account = accountOpt.get();
        account.approveRegistration();

        return userAccountStorage.update(account, account.getVersion() - 1); // version was incremented by approveRegistration
    }

    /**
     * Assigns an operational role to a user account.
     *
     * <p>Requires MANAGE_USER_ACCOUNT permission (Administrator role).
     *
     * <p>Enforces the single-operational-role invariant: if the user already has
     * a role assigned, this method does not allow a second role. Returns false
     * if the user already has a role or if the account/role does not exist.
     *
     * @param userId the ID of the user
     * @param roleId the ID of the role to assign
     * @return true if assignment succeeds, false if user already has a role,
     *         if account/role does not exist, or if assignment already exists
     * @throws SessionController.SessionExpiredException if the session has expired
     * @throws SessionController.InsufficientPermissionException if the session lacks permission
     */
    public boolean assignRole(long userId, long roleId) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");

        // Verify the user account exists
        if (userAccountStorage.findById(userId).isEmpty()) {
            return false; // User not found
        }

        // Verify the role exists
        if (rolePermissionStorage.findById(roleId).isEmpty()) {
            return false; // Role not found
        }

        // Check single-operational-role invariant: user must not already have a role
        List<RolePermission> currentRoles = rolePermissionStorage.findRolesByUserId(userId);
        if (!currentRoles.isEmpty()) {
            return false; // User already has a role
        }

        // Assign the role
        return rolePermissionStorage.assignRoleToUser(userId, roleId);
    }

    /**
     * Removes an operational role from a user account.
     *
     * <p>Requires MANAGE_USER_ACCOUNT permission (Administrator role).
     *
     * <p>Removes the user-role association. Returns false if the user does not
     * have the role assigned.
     *
     * @param userId the ID of the user
     * @param roleId the ID of the role to remove
     * @return true if removal succeeds, false if the user-role assignment does
     *         not exist or user/role does not exist
     * @throws SessionController.SessionExpiredException if the session has expired
     * @throws SessionController.InsufficientPermissionException if the session lacks permission
     */
    public boolean removeRole(long userId, long roleId) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");

        // Verify the user account exists
        if (userAccountStorage.findById(userId).isEmpty()) {
            return false; // User not found
        }

        // Verify the role exists
        if (rolePermissionStorage.findById(roleId).isEmpty()) {
            return false; // Role not found
        }

        // Remove the role
        return rolePermissionStorage.removeRoleFromUser(userId, roleId);
    }

    /**
     * Enables a disabled user account, restoring it to ACTIVE status and
     * allowing subsequent authentication attempts.
     *
     * <p>Requires MANAGE_USER_ACCOUNT permission (Administrator role).
     *
     * <p>Clears the disabled reason and sets the account to ACTIVE
     * (Requirement 2.2).
     *
     * @param userId the ID of the account to enable
     * @return true if enabling succeeds, false if the account does not exist
     *         or version conflict occurs
     * @throws SessionController.SessionExpiredException if the session has expired
     * @throws SessionController.InsufficientPermissionException if the session lacks permission
     */
    public boolean enableAccount(long userId) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");

        Optional<UserAccount> accountOpt = userAccountStorage.findById(userId);
        if (accountOpt.isEmpty()) {
            return false; // Account not found
        }

        UserAccount account = accountOpt.get();
        account.enable();

        return userAccountStorage.update(account, account.getVersion() - 1); // version was incremented by enable()
    }

    /**
     * Disables a user account, preventing all subsequent authentication attempts
     * until the account is re-enabled.
     *
     * <p>Requires MANAGE_USER_ACCOUNT permission (Administrator role).
     *
     * <p>Sets the account status to DISABLED and records the administrative
     * reason for disabling (Requirement 2.2).
     *
     * @param userId the ID of the account to disable
     * @param reason the administrative reason for disabling (must not be empty)
     * @return true if disabling succeeds, false if the account does not exist,
     *         reason is empty, or version conflict occurs
     * @throws SessionController.SessionExpiredException if the session has expired
     * @throws SessionController.InsufficientPermissionException if the session lacks permission
     */
    public boolean disableAccount(long userId, String reason) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");

        if (reason == null || reason.isBlank()) {
            return false; // Reason is required
        }

        Optional<UserAccount> accountOpt = userAccountStorage.findById(userId);
        if (accountOpt.isEmpty()) {
            return false; // Account not found
        }

        UserAccount account = accountOpt.get();
        account.disable(reason);

        return userAccountStorage.update(account, account.getVersion() - 1); // version was incremented by disable()
    }

    /**
     * Unlocks a locked user account, restoring it to ACTIVE status and allowing
     * subsequent authentication attempts.
     *
     * <p>Requires MANAGE_USER_ACCOUNT permission (Administrator role).
     *
     * <p>Clears the locked status and sets the account to ACTIVE. This is
     * typically used after a security incident or after the account has been
     * locked due to repeated failed authentication attempts.
     *
     * @param userId the ID of the account to unlock
     * @return true if unlocking succeeds, false if the account does not exist
     *         or version conflict occurs
     * @throws SessionController.SessionExpiredException if the session has expired
     * @throws SessionController.InsufficientPermissionException if the session lacks permission
     */
    public boolean unlockAccount(long userId) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");

        Optional<UserAccount> accountOpt = userAccountStorage.findById(userId);
        if (accountOpt.isEmpty()) {
            return false; // Account not found
        }

        UserAccount account = accountOpt.get();
        account.unlock();

        return userAccountStorage.update(account, account.getVersion() - 1); // version was incremented by unlock()
    }

    /**
     * Provisions login access for an existing unlinked Patient business record
     * without creating a duplicate Patient record.
     *
     * <p>Requires MANAGE_USER_ACCOUNT permission (Administrator role).
     *
     * <p>This method finds a PatientProfile with no linked UserAccount (userId
     * is null), creates a new UserAccount with the supplied credentials, links
     * it to the existing PatientProfile by updating the profile's userId, and
     * assigns the Patient role to the new account (Requirement 2.5).
     *
     * <p>Validation:
     * <ul>
     *   <li>patientIdentifier must match an existing PatientProfile with no linked account</li>
     *   <li>username must be unique</li>
     *   <li>email must be valid and unique</li>
     *   <li>the Patient role must exist and be active</li>
     * </ul>
     *
     * <p>Returns null if validation fails; the caller should report the validation
     * error messages via the UI. On success, the account is created in PENDING
     * status and must be approved before authentication is permitted.
     *
     * @param patientIdentifier the business identifier of the unlinked Patient record
     * @param username the unique username for the new login account
     * @param email the unique email address
     * @return the created and linked UserAccount, or null if validation fails
     * @throws SessionController.SessionExpiredException if the session has expired
     * @throws SessionController.InsufficientPermissionException if the session lacks permission
     */
    public UserAccount provisionLoginForPatientRecord(
            String patientIdentifier,
            String username,
            String email) {
        return provisionLoginForPatientRecord(patientIdentifier, username, email, null);
    }

    public UserAccount provisionLoginForPatientRecord(
            String patientIdentifier,
            String username,
            String email,
            char[] initialPassword) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");

        // Validate mandatory fields
        if (patientIdentifier == null || patientIdentifier.isBlank()) {
            return null; // Validation failed
        }
        if (username == null || username.isBlank()) {
            return null;
        }
        if (email == null || email.isBlank()) {
            return null;
        }
        if (!Credential.validateNewPassword(initialPassword).isEmpty()) return null;

        // Find the unlinked Patient business record
        List<PatientProfile> allPatients = userProfileStorage.listByType(PatientProfile.class);
        PatientProfile unlinkedPatient = null;
        for (PatientProfile patient : allPatients) {
            if (patientIdentifier.equals(patient.getPatientIdentifier())) {
                // Check if this patient is unlinked (no UserAccount)
                if (!patient.hasLinkedAccount()) {
                    unlinkedPatient = patient;
                    break;
                } else {
                    // Patient already has a linked account
                    return null;
                }
            }
        }

        if (unlinkedPatient == null) {
            return null; // No unlinked patient with this identifier
        }

        // Check for duplicate username
        if (userAccountStorage.findByUsername(username).isPresent()) {
            return null; // Username already exists
        }

        // Check for duplicate email
        if (userAccountStorage.findByEmail(email).isPresent()) {
            return null; // Email already exists
        }

        // Verify the Patient role exists and is active
        Optional<RolePermission> patientRoleOpt = rolePermissionStorage.findByRoleName("Patient");
        if (patientRoleOpt.isEmpty() || !patientRoleOpt.get().isActive()) {
            return null; // Patient role does not exist or is inactive
        }

        // Create the account in PENDING status
        UserAccount account = new UserAccount(0L, username, email);
        UserAccount created = userAccountStorage.create(account);

        // Link the account to the patient profile by updating the profile's userId
        // Create a new PatientProfile with the userId set to the new account's ID
        PatientProfile linkedPatient = new PatientProfile(
                unlinkedPatient.getProfileId(),
                created.getUserId(),
                unlinkedPatient.getPatientIdentifier(),
                unlinkedPatient.getFullName(),
                unlinkedPatient.getPhoneNumber(),
                unlinkedPatient.getContactEmail(),
                unlinkedPatient.getAddress(),
                unlinkedPatient.getPreferences(),
                unlinkedPatient.getVersion(),
                unlinkedPatient.getCreatedAt(),
                unlinkedPatient.getUpdatedAt(),
                unlinkedPatient.getDateOfBirth(),
                unlinkedPatient.getEmergencyContact());

        userProfileStorage.update(linkedPatient, unlinkedPatient.getVersion());

        // Assign the Patient role to the new account
        rolePermissionStorage.assignRoleToUser(created.getUserId(), patientRoleOpt.get().getRoleId());
        credentialStorage.create(new Credential(0L, created.getUserId(), initialPassword, passwordHasher));

        return created;
    }

    @GetMapping("/admin/users")
    public String users(@RequestParam(defaultValue = "") String search,
                        @RequestParam(defaultValue = "") String role,
                        @RequestParam(defaultValue = "") String state, Model model) {
        List<UserAccount> accounts = loadUserAccounts().stream()
                .filter(account -> search.isBlank() || account.getUsername().toLowerCase().contains(search.toLowerCase())
                        || account.getEmail().toLowerCase().contains(search.toLowerCase()))
                .filter(account -> state.isBlank() || account.getStatus().name().equalsIgnoreCase(state))
                .toList();
        UserAccountListView view = new UserAccountListView();
        view.displayAccounts(accounts);
        Map<Long, String> rolesByAccount = new HashMap<>();
        accounts.forEach(account -> rolesByAccount.put(account.getUserId(),
                rolePermissionStorage.findRolesByUserId(account.getUserId()).stream()
                        .findFirst().map(RolePermission::getRoleName).orElse("Unassigned")));
        model.addAttribute("view", view);
        model.addAttribute("accounts", accounts);
        model.addAttribute("rolesByAccount", rolesByAccount);
        model.addAttribute("search", search);
        model.addAttribute("roleFilter", role);
        model.addAttribute("stateFilter", state);
        model.addAttribute("filtersActive", !search.isBlank() || !role.isBlank() || !state.isBlank());
        adminPage(model, "User accounts");
        return "admin/users/list";
    }

    @GetMapping("/admin/users/new")
    public String newUser(Model model) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");
        model.addAttribute("form", new UserAccountFormView());
        model.addAttribute("roles", rolePermissionStorage.listAll());
        model.addAttribute("patientRecords", userProfileStorage.listByType(PatientProfile.class));
        adminPage(model, "Add account");
        return "admin/users/form";
    }

    @GetMapping("/admin/users/{id}/edit")
    public String editUser(@PathVariable long id, Model model) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");
        Optional<UserAccount> account = userAccountStorage.findById(id);
        if (account.isEmpty()) return "errors/not-found";
        UserAccountFormView form = new UserAccountFormView();
        form.setUsernameInput(account.get().getUsername());
        form.setEmailInput(account.get().getEmail());
        model.addAttribute("form", form);
        model.addAttribute("account", account.get());
        model.addAttribute("roles", rolePermissionStorage.listAll());
        model.addAttribute("expectedVersion", account.get().getVersion());
        adminPage(model, "Edit account");
        return "admin/users/form";
    }

    @PostMapping("/admin/users")
    public String createUser(@RequestParam(defaultValue = "") String username,
                             @RequestParam(defaultValue = "") String email,
                             @RequestParam(defaultValue = "") String role,
                             @RequestParam(defaultValue = "") String initialPassword,
                             @RequestParam(defaultValue = "") String confirmPassword,
                             @RequestParam(required = false) String patientIdentifier,
                             Model model, RedirectAttributes redirect) {
        boolean passwordInvalid = initialPassword.length() < Credential.MIN_PASSWORD_LENGTH;
        boolean confirmationInvalid = !initialPassword.equals(confirmPassword);
        if (passwordInvalid || confirmationInvalid) {
            UserAccountFormView form = new UserAccountFormView();
            form.setUsernameInput(username);
            form.setEmailInput(email);
            model.addAttribute("form", form);
            model.addAttribute("selectedRole", role);
            if (passwordInvalid) model.addAttribute("initialPasswordError", "Password must contain at least 8 characters.");
            if (confirmationInvalid) model.addAttribute("confirmPasswordError", "Password confirmation does not match.");
            model.addAttribute("roles", rolePermissionStorage.listAll());
            model.addAttribute("patientRecords", userProfileStorage.listByType(PatientProfile.class));
            adminPage(model, "Add account");
            return "admin/users/form";
        }
        UserAccount created = "Patient".equalsIgnoreCase(role) && patientIdentifier != null && !patientIdentifier.isBlank()
                ? provisionLoginForPatientRecord(patientIdentifier, username, email, initialPassword.toCharArray())
                : createUserAccount(username, email, role, initialPassword.toCharArray());
        if (created == null) {
            UserAccountFormView form = new UserAccountFormView();
            form.setUsernameInput(username);
            form.setEmailInput(email);
            model.addAttribute("form", form);
            model.addAttribute("selectedRole", role);
            model.addAttribute("duplicateError", "The username, email or role is not available.");
            model.addAttribute("roles", rolePermissionStorage.listAll());
            model.addAttribute("patientRecords", userProfileStorage.listByType(PatientProfile.class));
            adminPage(model, "Add account");
            return "admin/users/form";
        }
        redirect.addFlashAttribute("flash", new Flash("success", "Account created with an initial password. Approve it before first sign-in."));
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}")
    public String updateUser(@PathVariable long id, @RequestParam String email,
                             @RequestParam long expectedVersion, Model model,
                             RedirectAttributes redirect) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");
        Optional<UserAccount> account = userAccountStorage.findById(id);
        if (account.isEmpty()) return "errors/not-found";
        account.get().setEmail(email);
        if (!userAccountStorage.update(account.get(), expectedVersion)) {
            model.addAttribute("account", account.get());
            model.addAttribute("stale", true);
            adminPage(model, "Edit account");
            return "admin/users/form";
        }
        redirect.addFlashAttribute("flash", new Flash("success", "Account updated."));
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/{id}/access")
    public String access(@PathVariable long id, Model model) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");
        Optional<UserAccount> account = userAccountStorage.findById(id);
        if (account.isEmpty()) return "errors/not-found";
        model.addAttribute("account", account.get());
        model.addAttribute("assignedRoles", rolePermissionStorage.findRolesByUserId(id));
        model.addAttribute("roles", rolePermissionStorage.listAll());
        model.addAttribute("hasCredential", credentialStorage.findByUserId(id).isPresent());
        model.addAttribute("form", new UserAccountFormView());
        adminPage(model, "Role and access");
        return "admin/users/access";
    }

    @PostMapping("/admin/users/{id}/initial-password")
    public String setInitialPassword(@PathVariable long id,
                                     @RequestParam(defaultValue = "") String initialPassword,
                                     @RequestParam(defaultValue = "") String confirmPassword,
                                     RedirectAttributes redirect) {
        if (initialPassword.length() < Credential.MIN_PASSWORD_LENGTH) {
            redirect.addFlashAttribute("flash", new Flash("danger", "Password must contain at least 8 characters."));
        } else if (!initialPassword.equals(confirmPassword)) {
            redirect.addFlashAttribute("flash", new Flash("danger", "Password confirmation does not match."));
        } else if (setInitialPassword(id, initialPassword.toCharArray())) {
            redirect.addFlashAttribute("flash", new Flash("success", "Initial password saved. Approve the account before first sign-in."));
        } else {
            redirect.addFlashAttribute("flash", new Flash("warning", "An initial password is already set or the account is unavailable."));
        }
        return "redirect:/admin/users/" + id + "/access";
    }

    @PostMapping("/admin/users/{id}/approve")
    public String approve(@PathVariable long id, RedirectAttributes redirect) {
        return stateResult(approveRegistration(id), "Account approved and activated.", redirect);
    }

    @PostMapping("/admin/users/{id}/roles")
    public String updateRole(@PathVariable long id, @RequestParam long roleId,
                             RedirectAttributes redirect) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");
        rolePermissionStorage.findRolesByUserId(id)
                .forEach(current -> rolePermissionStorage.removeRoleFromUser(id, current.getRoleId()));
        if (!assignRole(id, roleId)) {
            redirect.addFlashAttribute("flash", new Flash("danger", "Select one valid operational role."));
            return "redirect:/admin/users/" + id + "/access";
        }
        redirect.addFlashAttribute("flash", new Flash("success", "Role updated."));
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/{id}/state")
    public String state(@PathVariable long id, @RequestParam(defaultValue = "disable") String action,
                        Model model) {
        sessionController.requirePermission("MANAGE_USER_ACCOUNT");
        Optional<UserAccount> account = userAccountStorage.findById(id);
        if (account.isEmpty()) return "errors/not-found";
        if (!isAvailableStateAction(account.get().getStatus(), action)) {
            return "redirect:/admin/users/" + id + "/access";
        }
        model.addAttribute("account", account.get());
        model.addAttribute("action", action);
        model.addAttribute("form", new UserAccountFormView());
        adminPage(model, "Confirm account state");
        return "admin/users/state";
    }

    @PostMapping("/admin/users/{id}/enable")
    public String enable(@PathVariable long id, RedirectAttributes redirect) {
        return stateResult(enableAccount(id), "Account enabled.", redirect);
    }

    @PostMapping("/admin/users/{id}/disable")
    public String disable(@PathVariable long id, @RequestParam(defaultValue = "") String reason,
                          RedirectAttributes redirect) {
        return stateResult(disableAccount(id, reason), "Account disabled.", redirect);
    }

    @PostMapping("/admin/users/{id}/unlock")
    public String unlock(@PathVariable long id, RedirectAttributes redirect) {
        return stateResult(unlockAccount(id), "Account unlocked.", redirect);
    }

    private String stateResult(boolean success, String message, RedirectAttributes redirect) {
        redirect.addFlashAttribute("flash", new Flash(success ? "success" : "warning",
                success ? message : "The account changed. Reload the latest record."));
        return "redirect:/admin/users";
    }

    private boolean isAvailableStateAction(AccountStatus status, String action) {
        return (status == AccountStatus.PENDING && "approve".equals(action))
                || (status == AccountStatus.ACTIVE && "disable".equals(action))
                || (status == AccountStatus.DISABLED && "enable".equals(action))
                || (status == AccountStatus.LOCKED && "unlock".equals(action));
    }

    private void adminPage(Model model, String title) {
        model.addAttribute("title", title);
        model.addAttribute("breadcrumb", "Administration / " + title);
    }
}
