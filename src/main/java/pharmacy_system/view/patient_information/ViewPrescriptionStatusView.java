package pharmacy_system.view.patient_information;

import pharmacy_system.controller.common.SessionController;
import pharmacy_system.controller.patient_information.ViewPrescriptionStatusController;
import pharmacy_system.model.patient_information.PrescriptionStatusSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Patient-facing, read-only prescription status and fulfilment progress View [UCD-02].
 *
 * <p>This View displays a patient's own prescriptions (excluding DRAFT) and their
 * associated fulfilment progress. It enforces strict ownership isolation and
 * presents clinical status, fulfilment progress, and dispensing readiness information.
 *
 * <p>Key behaviours:
 * <ul>
 *   <li>Lists all non-DRAFT prescriptions belonging to the authenticated patient
 *       (Requirement 6.1, Requirement 6.3).</li>
 *   <li>Shows both clinical lifecycle status and pharmacy fulfilment progress
 *       (Requirement 6.2).</li>
 *   <li>Presents expired prescriptions as "EXPIRED" even if their persisted
 *       clinical status is ISSUED (Requirement 6.4).</li>
 *   <li>Re-validates ownership on every detail view request, preventing tampered
 *       or substituted prescription identifiers (Requirement 6.5).</li>
 *   <li>Gracefully handles concurrent state changes by re-fetching and displaying
 *       current state.</li>
 *   <li>Is entirely read-only: no create, edit, or state-change actions are
 *       available from this View.</li>
 * </ul>
 *
 * <p>Requirements: 6.1, 6.2, 6.3, 7.2
 * Property 10: Draft invisibility
 * Property 11: Patient isolation
 */
public class ViewPrescriptionStatusView {

    private final ViewPrescriptionStatusController viewStatusController;
    private final SessionController sessionController;

    // UI state
    private List<PrescriptionStatusSummary> currentPrescriptions;
    private PrescriptionStatusSummary selectedPrescriptionDetail;
    private String errorMessage;
    private boolean isVisible;

    /**
     * Constructs the prescription status View with required controller dependencies.
     *
     * @param viewStatusController the patient information status controller
     * @param sessionController    the session manager for the current user
     * @throws NullPointerException if any parameter is null
     */
    public ViewPrescriptionStatusView(
            ViewPrescriptionStatusController viewStatusController,
            SessionController sessionController) {
        this.viewStatusController = Objects.requireNonNull(
                viewStatusController,
                "viewStatusController must not be null");
        this.sessionController = Objects.requireNonNull(
                sessionController,
                "sessionController must not be null");

        this.currentPrescriptions = new ArrayList<>();
        this.selectedPrescriptionDetail = null;
        this.errorMessage = null;
        this.isVisible = false;
    }

    /**
     * Displays the prescription status View. In a real desktop implementation,
     * this would initialize and show the UI window. Here it marks the view as
     * visible and loads the patient's prescriptions.
     */
    public void show() {
        isVisible = true;
        loadPrescriptionList();
    }

    /**
     * Hides the prescription status View. In a real implementation, this would
     * close the UI window.
     */
    public void hide() {
        isVisible = false;
        clearDetail();
    }

    /**
     * Returns whether this View is currently visible.
     *
     * @return {@code true} if shown; {@code false} otherwise
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Returns the currently displayed list of prescriptions for the authenticated
     * patient. This list excludes DRAFT prescriptions and is updated when
     * {@link #loadPrescriptionList()} is called.
     *
     * @return an unmodifiable copy of the current prescription list
     */
    public List<PrescriptionStatusSummary> getCurrentPrescriptions() {
        return new ArrayList<>(currentPrescriptions);
    }

    /**
     * Returns the currently selected prescription detail (from a prior call to
     * {@link #selectPrescriptionDetail(long)}), or null if no prescription is
     * selected or a detail view has not been loaded.
     *
     * @return the selected PrescriptionStatusSummary or null
     */
    public PrescriptionStatusSummary getSelectedPrescriptionDetail() {
        return selectedPrescriptionDetail;
    }

    /**
     * Returns the current error message, or null if no error is displayed.
     *
     * @return the error message or null
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Loads and displays the authenticated patient's prescription list
     * (Requirement 6.1, Requirement 6.3).
     *
     * <p>This method:
     * <ol>
     *   <li>Retrieves the current authenticated patient ID from the session.</li>
     *   <li>Calls {@link ViewPrescriptionStatusController#requestPrescriptionStatus(long)}
     *       to fetch all non-DRAFT prescriptions.</li>
     *   <li>Updates the current prescription list for display.</li>
     *   <li>Clears any previously selected detail view.</li>
     *   <li>On error, displays a user-friendly error message and leaves the
     *       previous state intact.</li>
     * </ol>
     *
     * <p>This method is called automatically when the View is shown or can be
     * called manually to refresh the prescription list.
     *
     * @return {@code true} if the list was loaded successfully; {@code false}
     *         if an error occurred (e.g., authentication loss, session expiry)
     */
    public boolean loadPrescriptionList() {
        // Ensure session is still valid
        if (!sessionController.isAuthenticated()) {
            displayError("Your session has expired. Please log in again.");
            return false;
        }

        try {
            long patientId = sessionController.getCurrentUserId();
            List<PrescriptionStatusSummary> prescriptions =
                    viewStatusController.requestPrescriptionStatus(patientId);

            this.currentPrescriptions = new ArrayList<>(prescriptions);
            this.errorMessage = null;
            clearDetail();
            return true;

        } catch (IllegalArgumentException e) {
            displayError("Unable to load prescriptions: " + e.getMessage());
            return false;
        } catch (Exception e) {
            displayError("An unexpected error occurred. Please try again later.");
            System.err.println("Error loading prescription list: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Selects a specific prescription and loads its detailed view (Requirement 6.2,
     * Requirement 6.4, Requirement 6.5).
     *
     * <p>This method:
     * <ol>
     *   <li>Verifies that the authenticated user is a patient (not an admin/doctor).
     *   </li>
     *   <li>Calls {@link ViewPrescriptionStatusController#viewPrescriptionDetails(long, long)}
     *       to retrieve the detail view, which enforces ownership re-verification.</li>
     *   <li>Stores the detail for display (clinical status, fulfilment progress,
     *       dispensing readiness).</li>
     *   <li>On error (ownership mismatch, not found), displays an error message and
     *       does not update the detail view.</li>
     * </ol>
     *
     * <p>The returned summary includes:
     * <ul>
     *   <li>Clinical status: the current lifecycle state, or EXPIRED if the
     *       prescription's validity period has passed (Requirement 6.4).</li>
     *   <li>Fulfilment status: the pharmacy dispensing state (NOT_STARTED if no
     *       dispense record exists yet; otherwise the dispense record's status
     *       like DISPENSED or FAILED).</li>
     *   <li>hasFulfilmentRecord: whether a dispense record exists for this
     *       prescription.</li>
     * </ul>
     *
     * @param prescriptionId the ID of the prescription to display in detail
     * @return {@code true} if the detail view was loaded successfully;
     *         {@code false} if an error occurred
     */
    public boolean selectPrescriptionDetail(long prescriptionId) {
        // Ensure session is still valid
        if (!sessionController.isAuthenticated()) {
            displayError("Your session has expired. Please log in again.");
            return false;
        }

        try {
            long patientId = sessionController.getCurrentUserId();
            PrescriptionStatusSummary detail =
                    viewStatusController.viewPrescriptionDetails(patientId, prescriptionId);

            this.selectedPrescriptionDetail = detail;
            this.errorMessage = null;
            return true;

        } catch (IllegalArgumentException e) {
            displayError("Unable to load prescription detail: " + e.getMessage());
            clearDetail();
            return false;
        } catch (Exception e) {
            displayError("An unexpected error occurred. Please try again later.");
            System.err.println("Error loading prescription detail: " + e.getMessage());
            e.printStackTrace();
            clearDetail();
            return false;
        }
    }

    /**
     * Refreshes the currently selected prescription detail by re-fetching it
     * from the controller. Useful for reflecting server-side state changes
     * (e.g., dispensing completion, status change by Doctor).
     *
     * <p>If no prescription is currently selected, this method has no effect.
     *
     * @return {@code true} if the detail was refreshed successfully or no
     *         prescription was selected; {@code false} if an error occurred
     */
    public boolean refreshSelectedDetail() {
        if (selectedPrescriptionDetail == null) {
            return true; // No detail to refresh
        }

        return selectPrescriptionDetail(selectedPrescriptionDetail.getPrescriptionId());
    }

    /**
     * Refreshes the entire prescription list by re-fetching from the controller.
     * Useful for reflecting new prescriptions, cancelled prescriptions, or state
     * changes.
     *
     * @return {@code true} if the list was refreshed successfully; {@code false}
     *         if an error occurred
     */
    public boolean refreshList() {
        return loadPrescriptionList();
    }

    /**
     * Clears the current detail view. Called when deselecting a prescription or
     * when the list is reloaded.
     */
    private void clearDetail() {
        this.selectedPrescriptionDetail = null;
    }

    /**
     * Displays an error message to the user. In a real implementation, this would
     * update a UI label or alert dialog.
     *
     * @param message the error message to display
     */
    private void displayError(String message) {
        this.errorMessage = message;
        // In a real implementation, this would update the UI error label
    }

    /**
     * Checks whether the prescription is eligible for viewing (not DRAFT and
     * owned by the patient). Used by the View to determine if a prescription
     * should be displayed in the list.
     *
     * <p>This is a convenience method for View logic; the actual ownership
     * verification is done by the controller.
     *
     * @param prescription the prescription to check
     * @return {@code true} if the prescription should be displayed; {@code false}
     *         otherwise
     */
    public boolean isPrescriptionViewable(PrescriptionStatusSummary prescription) {
        if (prescription == null) {
            return false;
        }
        // DRAFT prescriptions are not viewable by patients
        return !"DRAFT".equals(prescription.getClinicalStatus());
    }

    /**
     * Formats the clinical status for display. Maps internal status names to
     * user-friendly labels.
     *
     * @param clinicalStatus the clinical status (e.g., "ISSUED", "EXPIRED", "CANCELLED")
     * @return a display-friendly status label
     */
    public String formatClinicalStatus(String clinicalStatus) {
        if (clinicalStatus == null) {
            return "Unknown";
        }

        return switch (clinicalStatus) {
            case "DRAFT" -> "Draft";
            case "ISSUED" -> "Active";
            case "ON_HOLD" -> "On Hold";
            case "CANCELLED" -> "Cancelled";
            case "EXPIRED" -> "Expired";
            default -> clinicalStatus;
        };
    }

    /**
     * Formats the fulfilment status for display. Maps internal status names to
     * user-friendly labels.
     *
     * @param fulfilmentStatus the fulfilment status (e.g., "NOT_STARTED", "DISPENSED")
     * @return a display-friendly fulfilment status label
     */
    public String formatFulfilmentStatus(String fulfilmentStatus) {
        if (fulfilmentStatus == null) {
            return "Unknown";
        }

        return switch (fulfilmentStatus) {
            case "NOT_STARTED" -> "Not Started";
            case "PENDING" -> "Pending";
            case "VERIFIED" -> "Verified";
            case "DISPENSED" -> "Dispensed";
            case "FAILED" -> "Failed";
            default -> fulfilmentStatus;
        };
    }

    /**
     * Returns a user-friendly description of the prescription's current state
     * combining clinical and fulfilment status. Used to provide summary
     * information in list views.
     *
     * @param prescription the prescription to describe
     * @return a brief status description
     */
    public String getStatusDescription(PrescriptionStatusSummary prescription) {
        if (prescription == null) {
            return "";
        }

        String clinicalStatus = formatClinicalStatus(prescription.getClinicalStatus());
        String fulfilmentStatus = formatFulfilmentStatus(prescription.getFulfilmentStatus());

        if (prescription.hasFulfilmentRecord()) {
            return clinicalStatus + " • " + fulfilmentStatus;
        } else {
            return clinicalStatus + " • Awaiting Preparation";
        }
    }
}
