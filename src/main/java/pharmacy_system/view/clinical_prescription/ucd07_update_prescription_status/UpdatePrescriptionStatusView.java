package pharmacy_system.view.clinical_prescription.ucd07_update_prescription_status;

import pharmacy_system.controller.clinical_prescription.UpdatePrescriptionStatusController;
import pharmacy_system.model.clinical_prescription.Prescription;
import pharmacy_system.model.clinical_prescription.PrescriptionStatus;

/**
 * Main view for prescription status management (UCD-07: Update Prescription Status).
 * Handles prescription status transitions with validation and error reporting.
 * Displays current status, allowed transitions, and update success/failure states.
 *
 * Requirement traceability: 5.1, 5.5
 */
public class UpdatePrescriptionStatusView {

    private final UpdatePrescriptionStatusController controller;
    private long selectedPrescriptionId = -1;
    private Prescription currentPrescription;
    private PrescriptionStatus currentStatus;
    private String lastErrorMessage = "";
    private String lastSuccessMessage = "";

    public UpdatePrescriptionStatusView(UpdatePrescriptionStatusController controller) {
        this.controller = controller;
    }

    /**
     * Opens the prescription status management interface.
     * Requirements: 5.1 (display prescription status for transition)
     */
    public void openStatusManagement() {
        this.lastErrorMessage = "";
        this.lastSuccessMessage = "";
        this.selectedPrescriptionId = -1;
        this.currentPrescription = null;
        this.currentStatus = null;
    }

    /**
     * Displays a prescription for status management.
     * Requirements: 5.1 (show current status and allowed transitions)
     */
    public void displayPrescription(Prescription prescription) {
        if (prescription == null) {
            this.lastErrorMessage = "Prescription is null";
            return;
        }
        this.selectedPrescriptionId = prescription.getPrescriptionId();
        this.currentPrescription = prescription;
        this.currentStatus = prescription.getStatus();
    }

    /**
     * Shows the current prescription status.
     * Requirements: 5.1 (display current status)
     */
    public void showCurrentStatus(PrescriptionStatus status) {
        if (status != null) {
            this.currentStatus = status;
        }
    }

    /**
     * Shows a success message after status update.
     * Requirements: 5.1 (update confirmed)
     */
    public void showUpdateSuccess(PrescriptionStatus newStatus) {
        this.lastSuccessMessage = "Prescription status updated to " + newStatus;
        this.currentStatus = newStatus;
    }

    /**
     * Shows an error message when prescription is not found.
     * Requirements: 5.5 (rejection for missing prescription)
     */
    public void showPrescriptionNotFound() {
        this.lastErrorMessage = "Prescription not found";
    }

    /**
     * Shows an invalid transition error.
     * Requirements: 5.5 (reject invalid transitions)
     */
    public void showInvalidTransition() {
        this.lastErrorMessage = "Invalid status transition. This transition is not permitted from the current status.";
    }

    /**
     * Shows an invalid transition error with reason requirement.
     * Requirements: 5.2 (reason required for certain transitions)
     */
    public void showMissingReason() {
        this.lastErrorMessage = "Reason is required for this status transition. Please provide at least one non-whitespace character.";
    }

    /**
     * Shows a role/permission error for non-Doctor users attempting ON_HOLD.
     * Requirements: 5.3 (Doctor-only ON_HOLD)
     */
    public void showInsufficientPermission() {
        this.lastErrorMessage = "Only Doctors can place a prescription on hold. This action requires the Doctor role.";
    }

    /**
     * Shows a concurrent update error (version conflict).
     * Requirements: 5.1 (concurrent modification handling)
     */
    public void showConcurrentUpdateError() {
        this.lastErrorMessage = "Prescription was modified by another user. Please reload and try again.";
    }

    /**
     * Shows a generic operation error.
     * Requirements: 5.5 (error indication)
     */
    public void showOperationError(String message) {
        this.lastErrorMessage = message == null ? "Operation failed" : message;
    }

    /**
     * Shows a save error (generic).
     * Requirements: 5.5 (error feedback)
     */
    public void showSaveError() {
        this.lastErrorMessage = "Failed to save status update. Please try again.";
    }

    /**
     * Shows a specific save error.
     * Requirements: 5.5 (error feedback)
     */
    public void showSaveError(String message) {
        this.lastErrorMessage = message;
    }

    /**
     * Gets the currently selected prescription ID.
     * For testing purposes.
     */
    public long getSelectedPrescriptionId() {
        return this.selectedPrescriptionId;
    }

    /**
     * Gets the current prescription.
     * For testing purposes.
     */
    public Prescription getCurrentPrescription() {
        return this.currentPrescription;
    }

    /**
     * Gets the current status.
     * For testing purposes.
     */
    public PrescriptionStatus getCurrentStatus() {
        return this.currentStatus;
    }

    /**
     * Gets the last error message.
     * For testing purposes.
     */
    public String getLastErrorMessage() {
        return this.lastErrorMessage;
    }

    /**
     * Gets the last success message.
     * For testing purposes.
     */
    public String getLastSuccessMessage() {
        return this.lastSuccessMessage;
    }

    /**
     * Gets the controller instance.
     * For testing purposes (to access controller methods).
     */
    public UpdatePrescriptionStatusController getController() {
        return this.controller;
    }
}
