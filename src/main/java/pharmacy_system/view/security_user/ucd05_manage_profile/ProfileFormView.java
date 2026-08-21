package pharmacy_system.view.security_user.ucd05_manage_profile;

import pharmacy_system.model.security_user.profile.UserProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A component View that collects self-service profile edits from the user.
 *
 * <p>This View is responsible for:
 * <ul>
 *   <li>Collecting user input for permitted profile fields.</li>
 *   <li>Validating field types and formats before submission.</li>
 *   <li>Displaying validation errors if changes are invalid.</li>
 *   <li>Displaying restricted-field errors if the user attempts to modify
 *       non-editable fields (Requirement 2.4).</li>
 *   <li>Submitting valid changes to the controller via the parent View.</li>
 * </ul>
 *
 * <p>Permitted editable fields for all profile types:
 * <ul>
 *   <li>fullName (String, required, 1-255 chars)</li>
 *   <li>phoneNumber (String, optional, 7-15 digits if provided)</li>
 *   <li>contactEmail (String, optional, local@domain if provided)</li>
 *   <li>address (String, optional, 1-255 chars)</li>
 *   <li>preferences (Map&lt;String,String&gt;, optional)</li>
 * </ul>
 *
 * <p>Restricted fields that this View must reject include:
 * <ul>
 *   <li>role, roleId, roleName, operationalRole</li>
 *   <li>accountStatus, status</li>
 *   <li>privilegedPermissions, permissionCodes, permissions</li>
 *   <li>profileId, userId, id, accountId</li>
 *   <li>version, createdAt, updatedAt</li>
 * </ul>
 *
 * <p>Requirements:
 * <ul>
 *   <li>Requirement 2.3: Collect and submit permitted field changes.</li>
 *   <li>Requirement 2.4: Reject restricted-field modifications; display error.</li>
 *   <li>FR-009: Self-service field protection.</li>
 * </ul>
 */
public class ProfileFormView {
    private static final java.util.Set<String> RESTRICTED_FIELDS = java.util.Set.of(
            "role", "roleId", "roleName", "operationalRole",
            "accountStatus", "status",
            "privilegedPermissions", "permissionCodes", "permissions",
            "systemIdentifier",
            "profileId", "userId", "id", "accountId",
            "version", "createdAt", "updatedAt");

    private static final java.util.Set<String> EDITABLE_FIELDS = java.util.Set.of(
            "fullName", "phoneNumber", "contactEmail", "address", "preferences");

    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[^\\s@]+@[^\\s@]+$");

    private static final java.util.regex.Pattern PHONE_PATTERN =
            java.util.regex.Pattern.compile("^\\d{7,15}$");

    private static final int MAX_TEXT_LENGTH = 255;

    // Form state: current input values for editable fields
    private String fullNameInput;
    private String phoneNumberInput;
    private String contactEmailInput;
    private String addressInput;
    private Map<String, String> preferencesInput;

    private String validationErrorMessage;
    private String restrictedFieldErrorMessage;
    private String saveErrorMessage;

    /**
     * Constructs a ProfileFormView.
     */
    public ProfileFormView() {
        resetForm();
    }

    /**
     * Resets the form to empty state, clearing all inputs and error messages.
     * Called after successful submission or when the profile is reloaded.
     */
    public void resetForm() {
        fullNameInput = "";
        phoneNumberInput = "";
        contactEmailInput = "";
        addressInput = "";
        preferencesInput = new HashMap<>();
        validationErrorMessage = null;
        restrictedFieldErrorMessage = null;
        saveErrorMessage = null;
    }

    /**
     * Sets the full name input field.
     *
     * @param fullName the full name to set
     */
    public void setFullNameInput(String fullName) {
        this.fullNameInput = fullName != null ? fullName : "";
    }

    /**
     * Returns the current full name input.
     *
     * @return the full name
     */
    public String getFullNameInput() {
        return fullNameInput;
    }

    /**
     * Sets the phone number input field.
     *
     * @param phoneNumber the phone number to set
     */
    public void setPhoneNumberInput(String phoneNumber) {
        this.phoneNumberInput = phoneNumber != null ? phoneNumber : "";
    }

    /**
     * Returns the current phone number input.
     *
     * @return the phone number
     */
    public String getPhoneNumberInput() {
        return phoneNumberInput;
    }

    /**
     * Sets the contact email input field.
     *
     * @param contactEmail the contact email to set
     */
    public void setContactEmailInput(String contactEmail) {
        this.contactEmailInput = contactEmail != null ? contactEmail : "";
    }

    /**
     * Returns the current contact email input.
     *
     * @return the contact email
     */
    public String getContactEmailInput() {
        return contactEmailInput;
    }

    /**
     * Sets the address input field.
     *
     * @param address the address to set
     */
    public void setAddressInput(String address) {
        this.addressInput = address != null ? address : "";
    }

    /**
     * Returns the current address input.
     *
     * @return the address
     */
    public String getAddressInput() {
        return addressInput;
    }

    /**
     * Sets the preferences input map.
     *
     * @param preferences the preferences map to set
     */
    public void setPreferencesInput(Map<String, String> preferences) {
        this.preferencesInput = preferences != null ? new HashMap<>(preferences) : new HashMap<>();
    }

    /**
     * Returns the current preferences input.
     *
     * @return the preferences map (copy)
     */
    public Map<String, String> getPreferencesInput() {
        return new HashMap<>(preferencesInput);
    }

    /**
     * Collects all changes from the form inputs. Validates all inputs for
     * type and format. On validation error, displays error and returns null
     * or empty map without submitting to the controller.
     *
     * <p>Performs client-side validation:
     * <ul>
     *   <li>fullName: required if submitted, 1-255 chars</li>
     *   <li>phoneNumber: optional, if provided must be 7-15 digits</li>
     *   <li>contactEmail: optional, if provided must be local@domain</li>
     *   <li>address: optional, 1-255 chars</li>
     * </ul>
     *
     * <p>Only non-empty changes are included in the returned map.
     * Restricted fields are never included.
     *
     * @return a map of permitted field names to new values; or an empty map
     *         if no changes were made; or null if validation failed
     */
    public Map<String, Object> collectChanges() {
        // Clear previous error messages
        validationErrorMessage = null;

        // Perform client-side validation
        List<String> errors = validateInputs();
        if (!errors.isEmpty()) {
            showValidationErrors(errors);
            return null;
        }

        // Build the changes map with only non-empty fields
        Map<String, Object> changes = new HashMap<>();

        if (fullNameInput != null && !fullNameInput.isBlank()) {
            changes.put("fullName", fullNameInput);
        }
        if (phoneNumberInput != null && !phoneNumberInput.isBlank()) {
            changes.put("phoneNumber", phoneNumberInput);
        }
        if (contactEmailInput != null && !contactEmailInput.isBlank()) {
            changes.put("contactEmail", contactEmailInput);
        }
        if (addressInput != null && !addressInput.isBlank()) {
            changes.put("address", addressInput);
        }
        if (!preferencesInput.isEmpty()) {
            changes.put("preferences", preferencesInput);
        }

        return changes;
    }

    /**
     * Validates all form inputs for type and format.
     *
     * @return a list of validation error messages; empty if all valid
     */
    private List<String> validateInputs() {
        List<String> errors = new ArrayList<>();

        // fullName: required if submitted
        if (fullNameInput != null && !fullNameInput.isBlank()) {
            if (fullNameInput.length() > MAX_TEXT_LENGTH) {
                errors.add("Full name must not exceed " + MAX_TEXT_LENGTH + " characters.");
            }
        }

        // phoneNumber: optional, but if provided must be 7-15 digits
        if (phoneNumberInput != null && !phoneNumberInput.isBlank()) {
            if (!PHONE_PATTERN.matcher(phoneNumberInput).matches()) {
                errors.add("Phone number must contain 7 to 15 digits.");
            }
        }

        // contactEmail: optional, but if provided must be local@domain
        if (contactEmailInput != null && !contactEmailInput.isBlank()) {
            if (contactEmailInput.length() > MAX_TEXT_LENGTH) {
                errors.add("Contact email must not exceed " + MAX_TEXT_LENGTH + " characters.");
            } else if (!EMAIL_PATTERN.matcher(contactEmailInput).matches()) {
                errors.add("Contact email must be of the form local@domain.");
            }
        }

        // address: optional, but if provided must not exceed max length
        if (addressInput != null && !addressInput.isBlank()) {
            if (addressInput.length() > MAX_TEXT_LENGTH) {
                errors.add("Address must not exceed " + MAX_TEXT_LENGTH + " characters.");
            }
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
     * Displays an error indicating that the user attempted to modify
     * restricted fields (Requirement 2.4).
     *
     * <p>This method is called by the parent View when the controller
     * rejects the submission due to restricted field violation.
     */
    public void showRestrictedFieldError() {
        restrictedFieldErrorMessage = "The following fields cannot be modified: "
                + "role, account status, permissions, or system identifiers. "
                + "Please remove these changes and try again.";
        // In a real implementation, this would update the UI error display
    }

    /**
     * Displays a generic save error message. Called when the controller
     * fails to persist the changes for other reasons (validation error,
     * concurrency conflict, etc.).
     *
     * @param message the error message to display
     */
    public void showSaveError(String message) {
        saveErrorMessage = message != null ? message : "Failed to save profile changes.";
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
     * Returns the current restricted field error message, if any.
     *
     * @return the error message or null
     */
    public String getRestrictedFieldErrorMessage() {
        return restrictedFieldErrorMessage;
    }

    /**
     * Returns the current save error message, if any.
     *
     * @return the error message or null
     */
    public String getSaveErrorMessage() {
        return saveErrorMessage;
    }

    /**
     * Populates the form from an existing UserProfile. Typically called
     * when opening the form to show the user their current values.
     *
     * @param profile the UserProfile to populate from; must not be null
     */
    public void populateFromProfile(UserProfile profile) {
        if (profile != null) {
            fullNameInput = profile.getFullName() != null ? profile.getFullName() : "";
            phoneNumberInput = profile.getPhoneNumber() != null ? profile.getPhoneNumber() : "";
            contactEmailInput = profile.getContactEmail() != null ? profile.getContactEmail() : "";
            addressInput = profile.getAddress() != null ? profile.getAddress() : "";
            preferencesInput = new HashMap<>(profile.getPreferences());
        }
    }
}
