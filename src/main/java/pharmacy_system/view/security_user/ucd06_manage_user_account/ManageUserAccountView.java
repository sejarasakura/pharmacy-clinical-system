package pharmacy_system.view.security_user.ucd06_manage_user_account;

import pharmacy_system.controller.common.SessionController;
import pharmacy_system.controller.security_user.ManageUserAccountController;
import pharmacy_system.model.security_user.UserAccount;

import java.util.List;
import java.util.Objects;

/**
 * The primary boundary View for administrative user account management (UCD-06).
 *
 * <p>Orchestrates the loading, display, and modification of system user
 * accounts. Composes UserAccountListView (account list and selection) and
 * UserAccountFormView (account creation and role assignment).
 *
 * <p>Requirements:
 * <ul>
 *   <li>Requirement 2.1: Create and manage User_Accounts with role assignment.</li>
 *   <li>Requirement 2.2: Disable/enable accounts; reject auth when disabled.</li>
 *   <li>Requirement 2.5: Link login access to existing Patient_Business_Record.</li>
 * </ul>
 *
 * <p>This View implements the UCD-06 flow:
 * <ul>
 *   <li>On open: load all user accounts and display in list.</li>
 *   <li>On account select: display account details and available actions.</li>
 *   <li>On create: collect new account data (username, email, role).</li>
 *   <li>On role assignment: assign a role to an existing account.</li>
 *   <li>On account disable: disable the account with a reason.</li>
 *   <li>On account enable: re-enable a disabled account.</li>
 *   <li>On success: refresh account list and show confirmation message.</li>
 *   <li>On error: display specific error message (duplicate, invalid, etc.).</li>
 * </ul>
 *
 * <p>Access control: All administrative operations require Administrator
 * permission, checked at the controller level. This View does not enforce
 * permissions; it relies on the controller to deny unpermitted operations.
 */
public class ManageUserAccountView {
    private final ManageUserAccountController accountController;
    private final SessionController sessionController;
    private final UserAccountListView listView;
    private final UserAccountFormView formView;

    private List<UserAccount> loadedAccounts;
    private boolean isVisible;

    /**
     * Constructs a ManageUserAccountView with required dependencies.
     *
     * @param accountController the user account management controller
     * @param sessionController the session manager (provides permission checks)
     * @param listView the account list display component
     * @param formView the account form component
     * @throws NullPointerException if any parameter is null
     */
    public ManageUserAccountView(
            ManageUserAccountController accountController,
            SessionController sessionController,
            UserAccountListView listView,
            UserAccountFormView formView) {
        this.accountController = Objects.requireNonNull(accountController, "accountController must not be null");
        this.sessionController = Objects.requireNonNull(sessionController, "sessionController must not be null");
        this.listView = Objects.requireNonNull(listView, "listView must not be null");
        this.formView = Objects.requireNonNull(formView, "formView must not be null");

        this.loadedAccounts = List.of();
        this.isVisible = false;
    }

    /**
     * Opens the user account management interface. Loads all accounts and
     * displays them in the list view.
     *
     * <p>Flow:
     * <ol>
     *   <li>Verify session is valid and user has Administrator permission.</li>
     *   <li>Load all user accounts via accountController.loadUserAccounts().</li>
     *   <li>Display the account list.</li>
     *   <li>If load fails, display error.</li>
     * </ol>
     *
     * @return {@code true} if accounts were loaded and displayed successfully;
     *         {@code false} if load failed or permission denied
     */
    public boolean openUserManagement() {
        // Verify session and permission
        if (!sessionController.isAuthenticated()) {
            showOperationFailure("Session is not established. Please log in.");
            return false;
        }

        try {
            sessionController.requirePermission("MANAGE_USER_ACCOUNTS");
        } catch (pharmacy_system.controller.common.SessionController.SessionExpiredException e) {
            showOperationFailure("Session has expired. Please log in again.");
            return false;
        } catch (pharmacy_system.controller.common.SessionController.InsufficientPermissionException e) {
            showOperationFailure("You do not have permission to manage user accounts.");
            return false;
        }

        // Load all accounts
        List<UserAccount> accounts = accountController.loadUserAccounts();
        if (accounts == null) {
            showOperationFailure("Failed to load user accounts.");
            return false;
        }

        // Store and display
        this.loadedAccounts = accounts;
        showAccounts(accounts);
        isVisible = true;
        return true;
    }

    /**
     * Displays the list of user accounts.
     *
     * @param accounts the list of UserAccounts to display
     */
    public void showAccounts(List<UserAccount> accounts) {
        if (accounts != null) {
            this.loadedAccounts = accounts;
            listView.displayAccounts(accounts);
            formView.resetForm();
        }
    }

    /**
     * Shows a success message for an administrative operation.
     *
     * @param message the success message to display
     */
    public void showOperationSuccess(String message) {
        // In a real implementation, this would show a success dialog or toast
    }

    /**
     * Shows an error message for a failed operation.
     *
     * @param message the error message to display
     */
    public void showOperationFailure(String message) {
        // In a real implementation, this would show an error dialog or alert
    }

    /**
     * Returns whether the view is currently visible.
     *
     * @return {@code true} if displayed; {@code false} otherwise
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Returns the currently loaded accounts.
     *
     * @return the list of UserAccounts or empty list if none loaded
     */
    public List<UserAccount> getLoadedAccounts() {
        return loadedAccounts;
    }

    /**
     * Handles the create account action. Collects new account data from the
     * form, submits to the controller, and handles the response.
     *
     * <p>Flow:
     * <ol>
     *   <li>Collect account data (username, email, role) from the form.</li>
     *   <li>Validate inputs on the View side (duplicate check is server-side).</li>
     *   <li>Submit to accountController.createUserAccount(username, email, roleName).</li>
     *   <li>On success: reload account list; show success message; reset form.</li>
     *   <li>On duplicate: show duplicate error.</li>
     *   <li>On invalid: show validation error.</li>
     *   <li>On permission denied: show permission error.</li>
     * </ol>
     *
     * @return {@code true} if account was created successfully; {@code false} otherwise
     */
    public boolean onCreateAccount() {
        // Collect data from the form
        var accountData = formView.collectNewAccountData();
        if (accountData == null) {
            formView.showValidationErrors(List.of("Account data is incomplete."));
            return false;
        }

        String username = (String) accountData.get("username");
        String email = (String) accountData.get("email");
        String roleName = (String) accountData.get("roleName");

        // Submit to controller
        try {
            UserAccount createdAccount = accountController.createUserAccount(username, email, roleName);
            if (createdAccount != null) {
                showOperationSuccess("Account created successfully.");
                refreshAccountList();
                formView.resetForm();
                return true;
            } else {
                showOperationFailure("Account creation failed.");
                return false;
            }
        } catch (IllegalArgumentException e) {
            // Likely a duplicate or invalid role
            if (e.getMessage().contains("duplicate") || e.getMessage().contains("already exists")) {
                formView.showDuplicateAccountError();
            } else if (e.getMessage().contains("role")) {
                formView.showInvalidRoleError();
            } else {
                formView.showValidationErrors(List.of(e.getMessage()));
            }
            return false;
        }
    }

    /**
     * Handles the assign role action for a selected account.
     *
     * <p>Flow:
     * <ol>
     *   <li>Get the selected account ID from the list view.</li>
     *   <li>Get the role ID from the form view.</li>
     *   <li>Submit to accountController.assignRole(userId, roleId).</li>
     *   <li>On success: reload account list; show success message.</li>
     *   <li>On failure: show error message.</li>
     * </ol>
     *
     * @return {@code true} if role was assigned successfully; {@code false} otherwise
     */
    public boolean onAssignRole() {
        long selectedUserId = listView.getSelectedUserId();
        if (selectedUserId <= 0) {
            showOperationFailure("Please select an account first.");
            return false;
        }

        long selectedRoleId = formView.getSelectedRoleId();
        if (selectedRoleId <= 0) {
            formView.showInvalidRoleError();
            return false;
        }

        boolean succeeded = accountController.assignRole(selectedUserId, selectedRoleId);
        if (succeeded) {
            showOperationSuccess("Role assigned successfully.");
            refreshAccountList();
            return true;
        } else {
            showOperationFailure("Failed to assign role.");
            return false;
        }
    }

    /**
     * Handles the disable account action for a selected account.
     *
     * <p>Flow:
     * <ol>
     *   <li>Get the selected account ID from the list view.</li>
     *   <li>Get the disable reason from the form view.</li>
     *   <li>Submit to accountController.disableAccount(userId, reason).</li>
     *   <li>On success: reload account list; show success message.</li>
     *   <li>On failure: show error message.</li>
     * </ol>
     *
     * @return {@code true} if account was disabled successfully; {@code false} otherwise
     */
    public boolean onDisableAccount() {
        long selectedUserId = listView.getSelectedUserId();
        if (selectedUserId <= 0) {
            showOperationFailure("Please select an account first.");
            return false;
        }

        String reason = formView.getDisableReason();
        if (reason == null || reason.isBlank()) {
            formView.showValidationErrors(List.of("Disable reason is required."));
            return false;
        }

        boolean succeeded = accountController.disableAccount(selectedUserId, reason);
        if (succeeded) {
            showOperationSuccess("Account disabled successfully.");
            refreshAccountList();
            return true;
        } else {
            showOperationFailure("Failed to disable account.");
            return false;
        }
    }

    /**
     * Handles the enable account action for a selected account.
     *
     * <p>Flow:
     * <ol>
     *   <li>Get the selected account ID from the list view.</li>
     *   <li>Submit to accountController.enableAccount(userId).</li>
     *   <li>On success: reload account list; show success message.</li>
     *   <li>On failure: show error message.</li>
     * </ol>
     *
     * @return {@code true} if account was enabled successfully; {@code false} otherwise
     */
    public boolean onEnableAccount() {
        long selectedUserId = listView.getSelectedUserId();
        if (selectedUserId <= 0) {
            showOperationFailure("Please select an account first.");
            return false;
        }

        boolean succeeded = accountController.enableAccount(selectedUserId);
        if (succeeded) {
            showOperationSuccess("Account enabled successfully.");
            refreshAccountList();
            return true;
        } else {
            showOperationFailure("Failed to enable account.");
            return false;
        }
    }

    /**
     * Handles the unlock account action for a selected locked account.
     *
     * @return {@code true} if account was unlocked successfully; {@code false} otherwise
     */
    public boolean onUnlockAccount() {
        long selectedUserId = listView.getSelectedUserId();
        if (selectedUserId <= 0) {
            showOperationFailure("Please select an account first.");
            return false;
        }

        boolean succeeded = accountController.unlockAccount(selectedUserId);
        if (succeeded) {
            showOperationSuccess("Account unlocked successfully.");
            refreshAccountList();
            return true;
        } else {
            showOperationFailure("Failed to unlock account.");
            return false;
        }
    }

    /**
     * Refreshes the account list by reloading all accounts from the controller.
     */
    private void refreshAccountList() {
        List<UserAccount> accounts = accountController.loadUserAccounts();
        if (accounts != null) {
            showAccounts(accounts);
        }
    }
}
