package pharmacy_system.view.clinical_prescription.ucd07_update_prescription_status;

import pharmacy_system.controller.clinical_prescription.UpdatePrescriptionStatusController;
import pharmacy_system.model.clinical_prescription.PrescriptionStatus;

import java.util.HashSet;
import java.util.Set;

/**
 * Form view for prescription status selection and transition (UCD-07).
 * Handles user selection of target status and optional reason for the transition.
 * Surfaces validation errors including invalid transitions, concurrent updates, and insufficient permission.
 *
 * Requirement traceability: 5.1, 5.2, 5.3, 5.5
 */
public class PrescriptionStatusFormView {

    private final UpdatePrescriptionStatusController controller;
    private PrescriptionStatus selectedStatus;
    private String reason = "";
    private Set<PrescriptionStatus> allowedStatuses = new HashSet<>();
    private String lastValidationError = "";
    private String lastSaveError = "";

    public PrescriptionStatusFormView(UpdatePrescriptionStatusController controller) {
        this.controller = controller;
    }

    /**
     * Displays the allowed statuses for the current prescription.
     * Requirements: 5.1 (show allowed transitions)
     */
    public void displayAllowedStatuses(Set<PrescriptionStatus> statuses) {
        this.allowedStatuses = statuses == null ? new HashSet<>() : new HashSet<>(statuses);
    }

    /**
     * Gets the selected target status.
     * Requirements: 5.1 (user selects target status)
     */
    public PrescriptionStatus getSelectedStatus() {
        return this.selectedStatus;
    }

    /**
     * Sets the selected target status.
     * For testing purposes.
     */
    public void setSelectedStatus(PrescriptionStatus status) {
        this.selectedStatus = status;
    }

    /**
     * Gets the reason for the status transition.
     * Requirements: 5.2 (reason for certain transitions)
     */
    public String getReason() {
        return this.reason;
    }

    /**
     * Sets the reason for the status transition.
     * For testing purposes.
     */
    public void setReason(String reason) {
        this.reason = reason == null ? "" : reason;
    }

    /**
     * Gets the set of allowed statuses currently displayed.
     * For testing purposes.
     */
    public Set<PrescriptionStatus> getAllowedStatuses() {
        return new HashSet<>(this.allowedStatuses);
    }

    /**
     * Submits the status update to the controller.
     * In a real UI, this would trigger the controller action.
     * Requirements: 5.1 (transition execution)
     */
    public void submitStatusUpdate() {
        this.lastValidationError = "";
        this.lastSaveError = "";
    }

    /**
     * Shows an invalid transition error when the selected status is not in allowed transitions.
     * Requirements: 5.5 (reject invalid transitions)
     */
    public void showInvalidTransition() {
        this.lastValidationError = "Invalid status transition. The selected status is not an allowed transition from the current status.";
    }

    /**
     * Shows a missing reason error when reason is required but not provided.
     * Requirements: 5.2 (reason required for certain transitions)
     */
    public void showMissingReason() {
        this.lastValidationError = "Reason is required for this status transition.";
    }

    /**
     * Shows an insufficient permission error when attempting an action that requires a specific role.
     * Requirements: 5.3 (Doctor-only ON_HOLD)
     */
    public void showInsufficientPermission() {
        this.lastValidationError = "You do not have permission to perform this action.";
    }

    /**
     * Shows a concurrent update error (version conflict).
     * Requirements: 5.1 (concurrent modification handling)
     */
    public void showConcurrentUpdateError() {
        this.lastSaveError = "Prescription was modified by another user. Please reload and try again.";
    }

    /**
     * Shows a generic save error.
     * Requirements: 5.5 (error indication)
     */
    public void showSaveError() {
        this.lastSaveError = "Failed to save status update. Please try again.";
    }

    /**
     * Shows a specific save error message.
     * Requirements: 5.5 (error indication)
     */
    public void showSaveError(String message) {
        this.lastSaveError = message;
    }

    /**
     * Gets the last validation error.
     * For testing purposes.
     */
    public String getLastValidationError() {
        return this.lastValidationError;
    }

    /**
     * Gets the last save error.
     * For testing purposes.
     */
    public String getLastSaveError() {
        return this.lastSaveError;
    }

    /**
     * Clears all form data and error messages.
     * For testing purposes or when resetting the form.
     */
    public void clearForm() {
        this.selectedStatus = null;
        this.reason = "";
        this.allowedStatuses.clear();
        this.lastValidationError = "";
        this.lastSaveError = "";
    }

    /**
     * Gets the controller instance.
     * For testing purposes (to access controller methods).
     */
    public UpdatePrescriptionStatusController getController() {
        return this.controller;
    }
}
