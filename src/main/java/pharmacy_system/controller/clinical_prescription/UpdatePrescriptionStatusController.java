package pharmacy_system.controller.clinical_prescription;

import pharmacy_system.controller.common.SessionController;
import pharmacy_system.model.clinical_prescription.Prescription;
import pharmacy_system.model.clinical_prescription.PrescriptionStatus;
import pharmacy_system.storage.clinical_prescription.PrescriptionStorage;

import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pharmacy_system.view.common.Flash;
import pharmacy_system.view.clinical_prescription.ucd07_update_prescription_status.UpdatePrescriptionStatusView;
import pharmacy_system.view.clinical_prescription.ucd07_update_prescription_status.PrescriptionStatusFormView;

/**
 * Manages clinical prescription status transitions through the prescription lifecycle.
 *
 * <p>Implements UCD-07 (Update Prescription Status). Owns clinical lifecycle
 * transitions only (DRAFT/ISSUED/ON_HOLD/CANCELLED/EXPIRED). Does not own
 * dosage editing or fulfilment states.
 *
 * <p>Enforces that:
 * <ul>
 *   <li>Only valid transitions are allowed (FR-023, Property 16)
 *   <li>Only Doctors may place a prescription ON_HOLD (FR-024)
 *   <li>Status-change reasons are captured where required (FR-025)
 *   <li>EXPIRED prescriptions are ineligible for dispensing (FR-022)
 *   <li>Optimistic concurrency control prevents lost updates (FR-023)
 * </ul>
 *
 * <p>Requirements:
 * <ul>
 *   <li>FR-022..FR-026: Prescription status lifecycle and transitions</li>
 *   <li>SC-006: Expiry evaluation</li>
 *   <li>Property 16 (Valid transitions only): Validates via {@code Prescription.canTransitionTo}</li>
 * </ul>
 */
@Controller
public class UpdatePrescriptionStatusController {

    /**
     * Functional interface for domain event publishing.
     * Allows decoupled event publication to external listeners (e.g., notifications).
     */
    @FunctionalInterface
    public interface DomainEventPublisher {
        void publish(String eventType, long prescriptionId, long patientId, String recipient, boolean patientActive);
    }

    private final SessionController session;
    private final PrescriptionStorage prescriptionStorage;
    private DomainEventPublisher eventPublisher;

    public UpdatePrescriptionStatusController(
            SessionController session,
            PrescriptionStorage prescriptionStorage) {
        this.session = session;
        this.prescriptionStorage = prescriptionStorage;
        this.eventPublisher = (eventType, prescriptionId, patientId, recipient, patientActive) -> {
            // Default no-op publisher; override with setDomainEventPublisher
        };
    }

    /**
     * Sets the domain event publisher callback for prescription status events.
     * Allows wiring external event listeners (e.g., notifications controller).
     *
     * @param publisher the event publisher callback
     */
    public void setDomainEventPublisher(DomainEventPublisher publisher) {
        this.eventPublisher = publisher != null ? publisher : (e, p, pr, r, pact) -> {
            // no-op if null passed
        };
    }

    /**
     * Retrieves a prescription by its identifier without attempting to modify it.
     *
     * <p>Enforces authentication and permission check at the controller layer
     * (FR-003, FR-004).
     *
     * @param prescriptionId the prescription identifier
     * @return Optional containing the Prescription if found and authorized,
     *         empty otherwise
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public Optional<Prescription> loadPrescription(long prescriptionId) {
        // Enforce permission (Requirement 1.3, 1.4)
        session.requirePermission("PRESCRIPTION_STATUS_UPDATE");

        return prescriptionStorage.findById(prescriptionId);
    }

    /**
     * Returns the set of valid transitions from the current effective status of a
     * prescription (Property 16, FR-023).
     *
     * <p>For a prescription that is effectively EXPIRED (stored status is ISSUED/ON_HOLD
     * but more than one month has elapsed since issue), no transitions are allowed;
     * the returned set is empty (FR-022).
     *
     * <p>A prescription in a terminal state (CANCELLED, EXPIRED, or effectively
     * expired by issuedAt + 1 month) has no outgoing transitions.
     *
     * @param prescriptionId the prescription identifier
     * @return a Set of allowed PrescriptionStatus targets; empty if the prescription
     *         does not exist, is terminal, or is effectively expired
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public Set<PrescriptionStatus> getAllowedTransitions(long prescriptionId) {
        // Enforce permission
        session.requirePermission("PRESCRIPTION_STATUS_UPDATE");

        Optional<Prescription> prescriptionOpt = prescriptionStorage.findById(prescriptionId);
        if (prescriptionOpt.isEmpty()) {
            return Set.of();
        }

        Prescription prescription = prescriptionOpt.get();

        // If the prescription is terminal (CANCELLED, EXPIRED, or effectively
        // expired), no transitions are permitted (FR-022, FR-023, Property 16)
        if (prescription.isTerminalState()) {
            return Set.of();
        }

        // Return the allowed transitions from the current status
        return prescription.getStatus().allowedTransitions();
    }

    /**
     * Transitions a prescription to a target clinical status with optimistic
     * concurrency control (FR-023, Property 16).
     *
     * <p>Validates that:
     * <ul>
     *   <li>The target status is in the allowed transitions for the current status
     *   <li>A reason is provided if the target requires one (ON_HOLD, CANCELLED)
     *   <li>The prescription is not already terminal (CANCELLED, EXPIRED, or
     *       effectively expired)
     *   <li>The version matches the current stored version (optimistic concurrency)
     * </ul>
     *
     * <p>Does NOT enforce role-specific restrictions here; those are done by the
     * convenience methods (e.g., {@link #placeOnHold} is Doctor-only).
     *
     * @param prescriptionId the prescription identifier
     * @param targetStatus the desired clinical status
     * @param reason the change reason; required for ON_HOLD and CANCELLED targets
     * @param expectedVersion the version at the time of the last read
     * @return true if the transition succeeds; false if the prescription does not
     *         exist, the version conflicts, the transition is invalid, or the
     *         required reason is missing
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public boolean updateStatus(
            long prescriptionId,
            PrescriptionStatus targetStatus,
            String reason,
            long expectedVersion) {
        // Enforce permission
        session.requirePermission("PRESCRIPTION_STATUS_UPDATE");

        // Retrieve the current prescription
        Optional<Prescription> prescriptionOpt = prescriptionStorage.findById(prescriptionId);
        if (prescriptionOpt.isEmpty()) {
            return false;
        }

        Prescription prescription = prescriptionOpt.get();

        try {
            // Validate and perform the status transition; this checks state
            // validity and reason requirement (FR-023, FR-025, Property 16)
            prescription.changeStatus(targetStatus, session.getCurrentUserId(), reason);
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Invalid transition or missing required reason
            return false;
        }

        // Persist with optimistic concurrency control
        return prescriptionStorage.update(prescription, expectedVersion);
    }

    /**
     * Transitions a prescription to ISSUED, issuing it for dispensing.
     *
     * <p>Convenience method for the DRAFT/ON_HOLD -&gt; ISSUED transition;
     * no reason is required (FR-022).
     *
     * @param prescriptionId the prescription identifier
     * @param expectedVersion the version at the time of the last read
     * @return true if the transition succeeds; false if the prescription does not
     *         exist, the version conflicts, or the transition is invalid
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public boolean issuePrescription(long prescriptionId, long expectedVersion) {
        return updateStatus(prescriptionId, PrescriptionStatus.ISSUED, null, expectedVersion);
    }

    /**
     * Transitions a prescription to ON_HOLD, placing it on clinical hold.
     *
     * <p>Only a Doctor may place a prescription on hold (FR-024). This method
     * enforces the Doctor-only restriction at the controller level.
     *
     * <p>A reason must be provided (FR-025). The version must match to succeed.
     *
     * @param prescriptionId the prescription identifier
     * @param reason the reason for placing on hold; must be non-blank
     * @param expectedVersion the version at the time of the last read
     * @return true if the transition succeeds; false if the prescription does not
     *         exist, the version conflicts, the transition is invalid, the user
     *         is not a Doctor, or the reason is missing
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public boolean placeOnHold(long prescriptionId, String reason, long expectedVersion) {
        // Enforce permission
        session.requirePermission("PRESCRIPTION_STATUS_UPDATE");

        // Doctor-only check (FR-024): verify that the current user is a Doctor
        if (!requireDoctorAccess()) {
            return false;
        }

        return updateStatus(prescriptionId, PrescriptionStatus.ON_HOLD, reason, expectedVersion);
    }

    @GetMapping("/doctor/prescriptions/{id}/status")
    public String manageStatus(@PathVariable long id, Model model) {
        Optional<Prescription> prescription = loadPrescription(id);
        if (prescription.isEmpty()) return "errors/not-found";
        model.addAttribute("view", new UpdatePrescriptionStatusView(this));
        model.addAttribute("prescription", prescription.get());
        model.addAttribute("allowedTransitions", getAllowedTransitions(id));
        statusPage(model, "Clinical status");
        return "prescription-status/manage";
    }

    @GetMapping("/doctor/prescriptions/{id}/status/change")
    public String transitionPage(@PathVariable long id, @RequestParam PrescriptionStatus target,
                                 Model model) {
        Optional<Prescription> prescription = loadPrescription(id);
        if (prescription.isEmpty()) return "errors/not-found";
        model.addAttribute("form", new PrescriptionStatusFormView(this));
        model.addAttribute("prescription", prescription.get());
        model.addAttribute("target", target);
        model.addAttribute("expectedVersion", prescription.get().getVersion());
        statusPage(model, "Confirm status change");
        return "prescription-status/transition";
    }

    @PostMapping("/doctor/prescriptions/{id}/status")
    public String transition(@PathVariable long id, @RequestParam PrescriptionStatus target,
                             @RequestParam(defaultValue = "") String reason,
                             @RequestParam long expectedVersion, Model model,
                             RedirectAttributes redirect) {
        boolean requiresReason = target == PrescriptionStatus.ON_HOLD || target == PrescriptionStatus.CANCELLED;
        if (requiresReason && reason.isBlank()) {
            model.addAttribute("prescription", loadPrescription(id).orElse(null));
            model.addAttribute("target", target);
            model.addAttribute("expectedVersion", expectedVersion);
            model.addAttribute("reasonError", "A reason is required for this transition.");
            statusPage(model, "Confirm status change");
            return "prescription-status/transition";
        }
        if (!updateStatus(id, target, reason, expectedVersion)) {
            model.addAttribute("prescription", loadPrescription(id).orElse(null));
            model.addAttribute("target", target);
            model.addAttribute("stale", true);
            statusPage(model, "Confirm status change");
            return "prescription-status/transition";
        }
        redirect.addFlashAttribute("flash", new Flash("success", "Prescription status updated."));
        return "redirect:/doctor/prescriptions/" + id;
    }

    private void statusPage(Model model, String title) {
        model.addAttribute("title", title);
        model.addAttribute("breadcrumb", "Prescriptions / " + title);
    }

    /**
     * Transitions a prescription to CANCELLED, permanently marking it as cancelled.
     *
     * <p>A reason must be provided (FR-025). The version must match to succeed.
     *
     * @param prescriptionId the prescription identifier
     * @param reason the cancellation reason; must be non-blank
     * @param expectedVersion the version at the time of the last read
     * @return true if the transition succeeds; false if the prescription does not
     *         exist, the version conflicts, the transition is invalid, or the
     *         reason is missing
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public boolean cancelPrescription(long prescriptionId, String reason, long expectedVersion) {
        return updateStatus(prescriptionId, PrescriptionStatus.CANCELLED, reason, expectedVersion);
    }

    /**
     * Transitions a prescription from ON_HOLD back to ISSUED, resuming dispensing.
     *
     * <p>Convenience method for the ON_HOLD -&gt; ISSUED transition; no reason
     * is required.
     *
     * @param prescriptionId the prescription identifier
     * @param expectedVersion the version at the time of the last read
     * @return true if the transition succeeds; false if the prescription does not
     *         exist, the version conflicts, or the transition is invalid
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public boolean resumePrescription(long prescriptionId, long expectedVersion) {
        return updateStatus(prescriptionId, PrescriptionStatus.ISSUED, null, expectedVersion);
    }

    /**
     * Enforces that only Doctors can perform Doctor-restricted operations
     * (e.g., placing a prescription on hold per FR-024).
     *
     * <p>Checks whether the current session holds a Doctor-exclusive permission.
     * Returns true if the user is authorized for Doctor-only operations, false otherwise.
     *
     * @return true if the current user is authorized as a Doctor, false otherwise
     */
    private boolean requireDoctorAccess() {
        // The PRESCRIPTION_STATUS_UPDATE permission is Doctor-exclusive (per the design diagram).
        // All users with this permission are authorized as Doctors.
        // Since we already checked this permission in placeOnHold, this is a defensive check.
        return session.hasPermission("PRESCRIPTION_STATUS_UPDATE");
    }
}
