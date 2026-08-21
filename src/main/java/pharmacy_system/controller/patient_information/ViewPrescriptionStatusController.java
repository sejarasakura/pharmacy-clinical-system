package pharmacy_system.controller.patient_information;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pharmacy_system.view.patient_information.ViewPrescriptionStatusView;

import pharmacy_system.controller.common.SessionController;
import pharmacy_system.model.clinical_prescription.Prescription;
import pharmacy_system.model.clinical_prescription.PrescriptionStatus;
import pharmacy_system.model.patient_information.PrescriptionStatusSummary;
import pharmacy_system.model.pharmacy_operations.DispenseRecord;
import pharmacy_system.storage.clinical_prescription.PrescriptionStorage;
import pharmacy_system.storage.pharmacy_operations.DispenseStorage;

/**
 * Controller for patient-facing prescription status visibility (UCD-02).
 *
 * <p>Provides read-only access to a patient's own prescriptions and their
 * clinical + fulfilment progress. Enforces strict ownership isolation
 * (Requirement 6.5) and excludes DRAFT prescriptions from the patient view
 * (Requirement 6.3).
 *
 * <p>Every detail view re-verifies ownership to prevent tampered or
 * substituted prescription identifiers.
 *
 * _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
 * _Property 10: Draft invisibility; Property 11: Patient isolation_
 */
@Controller
public class ViewPrescriptionStatusController {

    private final SessionController sessionController;
    private final PrescriptionStorage prescriptionStorage;
    private final DispenseStorage dispenseStorage;

    /**
     * Constructs the view controller.
     *
     * @param sessionController the session context for the current user
     * @param prescriptionStorage the prescription storage backend
     * @param dispenseStorage the dispense record storage backend
     */
    public ViewPrescriptionStatusController(
            SessionController sessionController,
            PrescriptionStorage prescriptionStorage,
            DispenseStorage dispenseStorage) {
        this.sessionController = sessionController;
        this.prescriptionStorage = prescriptionStorage;
        this.dispenseStorage = dispenseStorage;
    }

    /**
     * Retrieves all prescriptions owned by the authenticated patient, excluding
     * DRAFT prescriptions (Requirement 6.1, Requirement 6.3).
     *
     * <p>Each summary includes the clinical status (which may be resolved as
     * EXPIRED if the prescription's validity end date has passed), the
     * fulfilment status (or a not-yet-started indication), and the
     * hasFulfilmentRecord flag.
     *
     * <p>This method enforces patient ownership at the controller level and
     * excludes DRAFT prescriptions from the result set, ensuring only the
     * patient's own visible prescriptions are returned (Property 10, Property 11).
     *
     * @param patientId the ID of the patient requesting their prescription status
     * @return a list of PrescriptionStatusSummary objects for prescriptions
     *         owned by this patient and not in DRAFT state, empty list if
     *         none exist
     * @throws IllegalArgumentException if the authenticated session does not
     *         represent the same patient (ownership mismatch)
     */
    public List<PrescriptionStatusSummary> requestPrescriptionStatus(long patientId) {
        // Verify the authenticated user is the same patient requesting their own data
        if (sessionController.getCurrentUserId() != patientId) {
            throw new IllegalArgumentException(
                    "Patient isolation: authenticated user does not own this prescription.");
        }

        // Retrieve all prescriptions for this patient from storage
        List<Prescription> allPrescriptions = prescriptionStorage.findByPatientId(patientId);

        List<PrescriptionStatusSummary> visibleSummaries = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Filter out DRAFT prescriptions and build summaries for the rest
        for (Prescription rx : allPrescriptions) {
            // Requirement 6.3: DRAFT prescriptions are excluded from the patient's list
            if (rx.getStatus() == PrescriptionStatus.DRAFT) {
                continue;
            }

            // Try to find a dispense record for this prescription
            Optional<DispenseRecord> dispenseRecordOpt = dispenseStorage
                    .findByPrescriptionId(rx.getPrescriptionId())
                    .stream()
                    .findFirst();

            // Build the summary combining clinical and fulfilment status
            PrescriptionStatusSummary summary = PrescriptionStatusSummary.from(
                    rx,
                    dispenseRecordOpt.orElse(null),
                    today);

            visibleSummaries.add(summary);
        }

        return visibleSummaries;
    }

    /**
     * Retrieves the detailed view of a specific prescription owned by the
     * authenticated patient (Requirement 6.2, Requirement 6.4, Requirement 6.5).
     *
     * <p>This method enforces ownership isolation by re-verifying that:
     * 1. The authenticated user is the same patient requesting access
     * 2. The prescription being accessed belongs to that patient
     *
     * <p>Access is denied for any patient attempting to access a prescription
     * not owned by them, including access requested by supplying a substituted
     * or tampered prescription identifier (Property 11).
     *
     * <p>The returned summary includes:
     * - The clinical status (displayed as EXPIRED if the prescription's
     *   validity end date has passed)
     * - The fulfilment status (either the dispense record's status if one
     *   exists, or a not-yet-started indication)
     *
     * @param patientId the ID of the patient requesting the prescription detail
     * @param prescriptionId the ID of the prescription to retrieve
     * @return a PrescriptionStatusSummary for the requested prescription
     *         with combined clinical and fulfilment status
     * @throws IllegalArgumentException if:
     *         - the authenticated user is not the same as patientId
     *         - the prescription does not exist
     *         - the prescription is not owned by the requested patientId
     */
    public PrescriptionStatusSummary viewPrescriptionDetails(long patientId, long prescriptionId) {
        // Requirement 6.5: Deny access if the authenticated user is not the same patient
        if (sessionController.getCurrentUserId() != patientId) {
            throw new IllegalArgumentException(
                    "Patient isolation: authenticated user does not own this prescription.");
        }

        // Retrieve the prescription from storage
        Optional<Prescription> prescriptionOpt = prescriptionStorage.findById(prescriptionId);
        if (prescriptionOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Prescription not found: " + prescriptionId);
        }

        Prescription prescription = prescriptionOpt.get();

        // Requirement 6.5: Enforce ownership re-verification (prevent tampered identifiers)
        if (prescription.getPatientId() != patientId) {
            throw new IllegalArgumentException(
                    "Patient isolation: prescription does not belong to this patient.");
        }

        // Retrieve any existing dispense record for this prescription
        Optional<DispenseRecord> dispenseRecordOpt = dispenseStorage
                .findByPrescriptionId(prescriptionId)
                .stream()
                .findFirst();

        // Build and return the status summary with combined clinical and fulfilment information
        LocalDate today = LocalDate.now();
        return PrescriptionStatusSummary.from(
                prescription,
                dispenseRecordOpt.orElse(null),
                today);
    }

    @GetMapping("/patient/prescriptions")
    public String prescriptions(Model model) {
        sessionController.requirePermission("VIEW_PRESCRIPTION_STATUS");
        List<PrescriptionStatusSummary> summaries = requestPrescriptionStatus(sessionController.getCurrentUserId());
        model.addAttribute("view", new ViewPrescriptionStatusView(this, sessionController));
        model.addAttribute("prescriptions", summaries);
        patientPage(model, "My prescriptions");
        return "patient/prescriptions";
    }

    @GetMapping("/patient/prescriptions/{id}")
    public String prescription(@PathVariable long id, Model model) {
        sessionController.requirePermission("VIEW_PRESCRIPTION_STATUS");
        try {
            PrescriptionStatusSummary summary = viewPrescriptionDetails(sessionController.getCurrentUserId(), id);
            model.addAttribute("prescription", summary);
            model.addAttribute("prescriptionRecord", prescriptionStorage.findById(id).orElse(null));
            patientPage(model, "Prescription status");
            return "patient/prescription-detail";
        } catch (IllegalArgumentException denied) {
            model.addAttribute("notFound", true);
            model.addAttribute("title", "Prescription not found");
            model.addAttribute("breadcrumb", "My prescriptions / Not found");
            return "errors/not-found";
        }
    }

    private void patientPage(Model model, String title) {
        model.addAttribute("title", title);
        model.addAttribute("breadcrumb", "My prescriptions / " + title);
    }
}
