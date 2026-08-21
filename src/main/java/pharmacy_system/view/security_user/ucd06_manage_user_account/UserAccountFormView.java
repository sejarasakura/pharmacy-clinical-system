package pharmacy_system.view.security_user.ucd06_manage_user_account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A component View that collects administrative account creation and
 * modification data.
 *
 * <p>This View is responsible for:
 * <ul>
 *   <li>Collecting account creation data (username, email, role).</li>
 *   <li>Collecting account modification data (role assignment, disable reason, etc.).</li>
 *   <li>Validating field types and formats before submission.</li>
 *   <li>Displaying validation errors if inputs are invalid.</li>
 *   <li>Displaying specific error messages (duplicate account, invalid role, etc.).</li>
 * </ul>
 *
 * <p>Validation performed:
 * <ul>
 *   <li>username: required, 1-255 chars, no duplicate (checked server-side).</li>
 *   <li>email: required, local@domain format, no duplicate (checked server-side).</li>
 *   <li>roleName: required, must be one of: Patient, Doctor, Pharmacist, Administrator.</li>
 * </ul>
 *
 * <p>Requirements:
 * <ul>
 *   <li>Requirement 2.1: Create and manage User_Accounts with role assignment.</li>
 *   <li>Requirement 2.2: Disable/enable accounts with required reason for disable.</li>
 * </ul>
 */
public class UserAccountFormView {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+$");

    private static final int MAX_TEXT_LENGTH = 255;

    private static final List<String> VALID_ROLES = List.of(
            "Patient", "Doctor", "Pharmacist", "Administrator");

    // Form state for account creation/modification
    private String usernameInput;
    private String emailInput;
    private long selectedRoleId;
    private String selectedRoleName;
    private String disableReasonInput;

    private String validationErrorMessage;
    private String duplicateAccountErrorMessage;
    private String invalidRoleErrorMessage;
    private String accountNotFoundErrorMessage;
    private String concurrentUpdateErrorMessage;

    /**
     * Constructs a UserAccountFormView.
     */
    public UserAccountFormView() {
        resetForm();
    }

    /**
     * Resets the form to empty state, clearing all inputs and error messages.
     * Called after successful submission or when opening a new form.
     */
    public void resetForm() {
        usernameInput = "";
        emailInput = "";
        selectedRoleId = -1;
        selectedRoleName = "";
        disableReasonInput = "";
        validationErrorMessage = null;
        duplicateAccountErrorMessage = null;
        invalidRoleErrorMessage = null;
        accountNotFoundErrorMessage = null;
        concurrentUpdateErrorMessage = null;
    }

    /**
     * Sets the username input field.
     *
     * @param username the username to set
     */
    public void setUsernameInput(String username) {
        this.usernameInput = username != null ? username : "";
    }

    /**
     * Returns the current username input.
     *
     * @return the username
     */
    public String getUsernameInput() {
        return usernameInput;
    }

    /**
     * Sets the email input field.
     *
     * @param email the email to set
     */
    public void setEmailInput(String email) {
        this.emailInput = email != null ? email : "";
    }

    /**
     * Returns the current email input.
     *
     * @return the email
     */
    public String getEmailInput() {
        return emailInput;
    }

    /**
     * Sets the selected role ID and name.
     *
     * @param roleId the role ID to select
     * @param roleName the role name (must be one of Patient, Doctor, Pharmacist, Administrator)
     */
    public void setSelectedRole(long roleId, String roleName) {
        this.selectedRoleId = roleId;
        this.selectedRoleName = roleName != null ? roleName : "";
    }

    /**
     * Returns the selected role ID.
     *
     * @return the role ID, or -1 if none selected
     */
    public long getSelectedRoleId() {
        return selectedRoleId;
    }

    /**
     * Returns the selected role name.
     *
     * @return the role name
     */
    public String getSelectedRoleName() {
        return selectedRoleName;
    }

    /**
     * Sets the disable reason input field.
     *
     * @param reason the reason for disabling the account
     */
    public void setDisableReason(String reason) {
        this.disableReasonInput = reason != null ? reason : "";
    }

    /**
     * Returns the current disable reason input.
     *
     * @return the disable reason
     */
    public String getDisableReason() {
        return disableReasonInput;
    }

    /**
     * Collects new account data from the form inputs. Validates all inputs
     * for type and format. On validation error, displays error and returns
     * null without submitting to the controller.
     *
     * <p>Client-side validation:
     * <ul>
     *   <li>username: required, 1-255 chars</li>
     *   <li>email: required, local@domain format, 1-255 chars</li>
     *   <li>roleName: required, must be valid (Patient, Doctor, Pharmacist, Administrator)</li>
     * </ul>
     *
     * <p>Server-side validation:
     * <ul>
     *   <li>Duplicate username or email is checked by the controller.</li>
     * </ul>
     *
     * @return a map containing "username", "email", and "roleName" keys with
     *         their values; or null if validation failed
     */
    public Map<String, Object> collectNewAccountData() {
        // Clear previous error messages
        validationErrorMessage = null;

        // Perform client-side validation
        List<String> errors = validateNewAccountInputs();
        if (!errors.isEmpty()) {
            showValidationErrors(errors);
            return null;
        }

        // Build the account data map
        Map<String, Object> accountData = new HashMap<>();
        accountData.put("username", usernameInput.trim());
        accountData.put("email", emailInput.trim());
        accountData.put("roleName", selectedRoleName);

        return accountData;
    }

    /**
     * Validates inputs for new account creation.
     *
     * @return a list of validation error messages; empty if all valid
     */
    private List<String> validateNewAccountInputs() {
        List<String> errors = new ArrayList<>();

        // username: required, 1-255 chars
        if (usernameInput == null || usernameInput.isBlank()) {
            errors.add("Username is required.");
        } else if (usernameInput.length() > MAX_TEXT_LENGTH) {
            errors.add("Username must not exceed " + MAX_TEXT_LENGTH + " characters.");
        }

        // email: required, local@domain format, 1-255 chars
        if (emailInput == null || emailInput.isBlank()) {
            errors.add("Email is required.");
        } else if (emailInput.length() > MAX_TEXT_LENGTH) {
            errors.add("Email must not exceed " + MAX_TEXT_LENGTH + " characters.");
        } else if (!EMAIL_PATTERN.matcher(emailInput).matches()) {
            errors.add("Email must be of the form local@domain.");
        }

        // roleName: required, must be valid
        if (selectedRoleName == null || selectedRoleName.isBlank()) {
            errors.add("Role is required.");
        } else if (!VALID_ROLES.contains(selectedRoleName)) {
            errors.add("Role must be one of: Patient, Doctor, Pharmacist, Administrator.");
        }

        return errors;
    }

    /**
     * Displays validation error messages to the user. Called when form
     * validation fails before submission to the controller.
     *
     * @param errors field-specific validation error messages
     */
    public void showValidationErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            validationErrorMessage = null;
        } else {
            validationErrorMessage = String.join("; ", errors);
        }
        // In a real implementation, this would update the UI error display
    }

    /**
     * Displays an error indicating that the submitted username or email
     * already exists (Requirement 2.1: unique identifier).
     */
    public void showDuplicateAccountError() {
        duplicateAccountErrorMessage = "An account with this username or email already exists.";
        // In a real implementation, this would update the UI error display
    }

    /**
     * Displays an error indicating that the selected role is invalid or
     * not available.
     */
    public void showInvalidRoleError() {
        invalidRoleErrorMessage = "The selected role is invalid. Please choose a valid role.";
        // In a real implementation, this would update the UI error display
    }

    /**
     * Displays an error indicating that the account was not found (typically
     * after an account selection that is no longer valid).
     */
    public void showAccountNotFound() {
        accountNotFoundErrorMessage = "The selected account was not found. Please reload and try again.";
        // In a real implementation, this would update the UI error display
    }

    /**
     * Displays an error indicating a concurrent update conflict (version mismatch).
     */
    public void showConcurrentUpdateError() {
        concurrentUpdateErrorMessage = "The account was modified by another administrator. "
                + "Please reload and try again.";
        // In a real implementation, this would update the UI error display
    }

    /**
     * Returns the current validation error message, if any.
     *
     * @return the error message or null
     */
    public String getValidationErrorMessage() {
        return validationErrorMessage;
    }

    /**
     * Returns the current duplicate account error message, if any.
     *
     * @return the error message or null
     */
    public String getDuplicateAccountErrorMessage() {
        return duplicateAccountErrorMessage;
    }

    /**
     * Returns the current invalid role error message, if any.
     *
     * @return the error message or null
     */
    public String getInvalidRoleErrorMessage() {
        return invalidRoleErrorMessage;
    }

    /**
     * Returns the current account not found error message, if any.
     *
     * @return the error message or null
     */
    public String getAccountNotFoundErrorMessage() {
        return accountNotFoundErrorMessage;
    }

    /**
     * Returns the current concurrent update error message, if any.
     *
     * @return the error message or null
     */
    public String getConcurrentUpdateErrorMessage() {
        return concurrentUpdateErrorMessage;
    }
}
