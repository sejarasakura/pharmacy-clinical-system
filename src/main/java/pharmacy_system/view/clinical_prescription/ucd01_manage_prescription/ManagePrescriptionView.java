package pharmacy_system.view.clinical_prescription.ucd01_manage_prescription;

import pharmacy_system.controller.clinical_prescription.ManagePrescriptionController;
import pharmacy_system.model.clinical_prescription.Prescription;

import java.util.ArrayList;
import java.util.List;

/**
 * Main view for prescription management (UCD-01: Manage Prescription).
 * Handles prescription creation, viewing, editing, and cancellation workflows.
 * Surfaces validation errors, successful operations, and concurrent-update conflicts.
 *
 * Requirement traceability: 4.1, 4.2, 4.3
 */
public class ManagePrescriptionView {

    private final ManagePrescriptionController controller;
    private long selectedPrescriptionId = -1;
    private List<Prescription> prescriptionList = new ArrayList<>();
    private String lastErrorMessage = "";
    private String lastSuccessMessage = "";

    public ManagePrescriptionView(ManagePrescriptionController controller) {
        this.controller = controller;
    }

    /**
     * Opens the prescription management interface.
     * In a real UI, this would initialize the view and display the menu.
     */
    public void openPrescriptionManagement() {
        this.lastErrorMessage = "";
        this.lastSuccessMessage = "";
        this.selectedPrescriptionId = -1;
    }

    /**
     * Displays a single prescription in read-only or editable form.
     * Requirements: 4.1, 4.2 (display prescription information)
     */
    public void displayPrescription(Prescription prescription) {
        if (prescription == null) {
            this.lastErrorMessage = "Prescription is null";
            return;
        }
        this.selectedPrescriptionId = prescription.getPrescriptionId();
        // In a real UI, this would render the prescription data to the UI
        this.lastSuccessMessage = "Prescription displayed: " + prescription.getPrescriptionId();
    }

    /**
     * Displays a list of prescriptions.
     * Requirements: 4.1 (show prescription list)
     */
    public void displayPrescriptionList(List<Prescription> prescriptions) {
        this.prescriptionList = prescriptions == null ? new ArrayList<>() : new ArrayList<>(prescriptions);
        this.lastSuccessMessage = "Prescription list displayed: " + this.prescriptionList.size() + " items";
    }

    /**
     * Shows a success message after prescription creation.
     * Requirements: 4.1
     */
    public void showCreateSuccess() {
        this.lastSuccessMessage = "Prescription created successfully";
    }

    /**
     * Shows a success message after prescription update.
     * Requirements: 4.2
     */
    public void showUpdateSuccess() {
        this.lastSuccessMessage = "Prescription updated successfully";
    }

    /**
     * Shows a success message after prescription cancellation.
     * Requirements: 4.3
     */
    public void showCancellationSuccess() {
        this.lastSuccessMessage = "Prescription cancelled successfully";
    }

    /**
     * Shows an error message when prescription is not found.
     * Requirements: 4.6
     */
    public void showPrescriptionNotFound() {
        this.lastErrorMessage = "Prescription not found";
    }

    /**
     * Shows a validation or operational error.
     * Requirements: 4.6, 4.7
     */
    public void showOperationError(String message) {
        this.lastErrorMessage = message == null ? "Unknown error" : message;
    }

    /**
     * Shows a concurrent update error (version conflict).
     * Requirements: 4.2 (when editing a prescription that was modified by another user)
     */
    public void showConcurrentUpdateError() {
        this.lastErrorMessage = "Prescription was modified by another user. Please reload and try again.";
    }

    /**
     * Shows validation errors during prescription creation/editing.
     * Requirements: 4.6 (validation error indication), 4.7 (edit rejection)
     */
    public void showValidationErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            this.lastErrorMessage = "Validation error";
        } else {
            this.lastErrorMessage = "Validation errors: " + String.join("; ", errors);
        }
    }

    /**
     * Shows a success message for patient business record creation.
     * Requirements: 3.1
     */
    public void showPatientCreationSuccess() {
        this.lastSuccessMessage = "Patient business record created successfully";
    }

    /**
     * Gets the last error message from the view.
     * For testing purposes.
     */
    public String getLastErrorMessage() {
        return this.lastErrorMessage;
    }

    /**
     * Gets the last success message from the view.
     * For testing purposes.
     */
    public String getLastSuccessMessage() {
        return this.lastSuccessMessage;
    }

    /**
     * Gets the currently selected prescription ID.
     * For testing purposes.
     */
    public long getSelectedPrescriptionId() {
        return this.selectedPrescriptionId;
    }

    /**
     * Gets the prescription list currently displayed.
     * For testing purposes.
     */
    public List<Prescription> getPrescriptionList() {
        return new ArrayList<>(this.prescriptionList);
    }
}
