package pharmacy_system.controller.clinical_prescription;

import pharmacy_system.controller.common.SessionController;
import pharmacy_system.model.clinical_prescription.Prescription;
import pharmacy_system.model.clinical_prescription.PrescriptionItem;
import pharmacy_system.storage.clinical_prescription.PrescriptionStorage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pharmacy_system.view.common.Flash;
import pharmacy_system.view.clinical_prescription.ucd01_manage_prescription.ManagePrescriptionView;
import pharmacy_system.view.clinical_prescription.ucd01_manage_prescription.PrescriptionFormView;

/**
 * Manages clinical prescription creation, viewing, modification, and cancellation.
 *
 * <p>Implements UCD-01 (Manage Prescription). Owns prescription creation, viewing,
 * modification, and cancellation; medicine, dosage, quantity, frequency, instructions.
 *
 * <p>Enforces authorization and validation for all operations.
 * Works with SessionController for permission enforcement.
 * Persists prescriptions via PrescriptionStorage.
 *
 * [UCD-01]
 */
@Controller
public class ManagePrescriptionController {

    private final SessionController sessionController;
    private final PrescriptionStorage prescriptionStorage;

    public ManagePrescriptionController(
            SessionController sessionController,
            PrescriptionStorage prescriptionStorage) {
        this.sessionController = sessionController;
        this.prescriptionStorage = prescriptionStorage;
    }

    /**
     * Loads a prescription by its ID.
     * Requires MANAGE_PRESCRIPTION permission.
     *
     * @param prescriptionId the prescription ID
     * @return the Prescription if found and authorized, null otherwise
     * @throws IllegalStateException if permission check fails
     */
    public Prescription loadPrescription(long prescriptionId) {
        sessionController.requirePermission("MANAGE_PRESCRIPTION");
        return prescriptionStorage.findById(prescriptionId).orElse(null);
    }

    /**
     * Loads all prescriptions written by a specific doctor.
     * Requires MANAGE_PRESCRIPTION permission.
     *
     * @param doctorId the doctor ID
     * @return a list of Prescriptions written by the doctor
     * @throws IllegalStateException if permission check fails
     */
    public List<Prescription> loadDoctorPrescriptions(long doctorId) {
        sessionController.requirePermission("MANAGE_PRESCRIPTION");
        return prescriptionStorage.findByDoctorId(doctorId);
    }

    /**
     * Creates a new prescription for a patient.
     * Requires MANAGE_PRESCRIPTION permission.
     * Returns a new Prescription in DRAFT status.
     *
     * @param patientId the patient ID
     * @return a new Prescription ready for items and clinical notes, or null if creation fails
     * @throws IllegalStateException if permission check fails
     */
    public Prescription createPrescription(long patientId) {
        sessionController.requirePermission("MANAGE_PRESCRIPTION");

        long doctorId = requireDoctorAccess();

        Prescription prescription = new Prescription(0L, patientId, doctorId, null);
        return prescription;
    }

    /**
     * Adds a prescription item to an existing prescription.
     * Requires MANAGE_PRESCRIPTION permission.
     * Validates the item before adding.
     *
     * @param prescriptionId the prescription ID
     * @param item the PrescriptionItem to add
     * @return true if the item was added successfully, false otherwise
     * @throws IllegalStateException if permission check fails
     */
    public boolean addPrescriptionItem(long prescriptionId, PrescriptionItem item) {
        sessionController.requirePermission("MANAGE_PRESCRIPTION");

        Prescription prescription = prescriptionStorage.findById(prescriptionId).orElse(null);
        if (prescription == null) {
            return false;
        }

        // Validate item
        List<String> errors = item.validate();
        if (!errors.isEmpty()) {
            return false;
        }

        prescription.addItem(item);
        return true;
    }

    /**
     * Updates a prescription item's fields.
     * Requires MANAGE_PRESCRIPTION permission.
     * Validates changes before applying.
     *
     * @param prescriptionId the prescription ID
     * @param itemId the prescription item ID to update
     * @param changes a map of field names to new values
     * @return true if the update succeeds, false otherwise
     * @throws IllegalStateException if permission check fails
     */
    public boolean updatePrescriptionItem(long prescriptionId, long itemId, Map<String, Object> changes) {
        sessionController.requirePermission("MANAGE_PRESCRIPTION");

        Prescription prescription = prescriptionStorage.findById(prescriptionId).orElse(null);
        if (prescription == null) {
            return false;
        }

        return prescription.updateItem(itemId, changes);
    }

    /**
     * Removes a prescription item.
     * Requires MANAGE_PRESCRIPTION permission.
     *
     * @param prescriptionId the prescription ID
     * @param itemId the prescription item ID to remove
     * @return true if the item was removed, false otherwise
     * @throws IllegalStateException if permission check fails
     */
    public boolean removePrescriptionItem(long prescriptionId, long itemId) {
        sessionController.requirePermission("MANAGE_PRESCRIPTION");

        Prescription prescription = prescriptionStorage.findById(prescriptionId).orElse(null);
        if (prescription == null) {
            return false;
        }

        return prescription.removeItem(itemId);
    }

    /**
     * Updates prescription fields (clinical notes, etc).
     * Requires MANAGE_PRESCRIPTION permission.
     * Validates changes before applying.
     *
     * @param prescriptionId the prescription ID
     * @param changes a map of field names to new values
     * @return true if the update succeeds, false otherwise
     * @throws IllegalStateException if permission check fails
     */
    public boolean updatePrescription(long prescriptionId, Map<String, Object> changes) {
        sessionController.requirePermission("MANAGE_PRESCRIPTION");

        Prescription prescription = prescriptionStorage.findById(prescriptionId).orElse(null);
        if (prescription == null) {
            return false;
        }

        if (!prescription.canBeEdited()) {
            return false;
        }

        // Apply changes
        if (changes.containsKey("clinicalNotes") && changes.get("clinicalNotes") instanceof String) {
            prescription.updateClinicalNotes((String) changes.get("clinicalNotes"));
        }

        return true;
    }

    /**
     * Saves a prescription to storage.
     * Requires MANAGE_PRESCRIPTION permission.
     * Validates prescription content before saving.
     *
     * @param prescription the Prescription to save
     * @return true if the save succeeds, false if validation fails
     * @throws IllegalStateException if permission check fails
     */
    public boolean savePrescription(Prescription prescription) {
        sessionController.requirePermission("MANAGE_PRESCRIPTION");

        List<String> errors = validateForPersistence(prescription);
        if (!errors.isEmpty()) {
            return false;
        }

        return prescriptionStorage.save(prescription);
    }

    /**
     * Cancels a prescription.
     * Requires MANAGE_PRESCRIPTION permission.
     * Validates that cancellation is allowed before proceeding.
     *
     * @param prescriptionId the prescription ID
     * @param reason the cancellation reason
     * @return true if the cancellation succeeds, false otherwise
     * @throws IllegalStateException if permission check fails
     */
    public boolean cancelPrescription(long prescriptionId, String reason) {
        sessionController.requirePermission("MANAGE_PRESCRIPTION");

        Prescription prescription = prescriptionStorage.findById(prescriptionId).orElse(null);
        if (prescription == null) {
            return false;
        }

        if (!prescription.canBeCancelled()) {
            return false;
        }

        try {
            long cancelledBy = requireDoctorAccess();
            prescription.cancel(reason, cancelledBy);
            return prescriptionStorage.update(prescription, prescription.getVersion());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return false;
        }
    }

    /**
     * Private helper: ensures the current user is a doctor and returns their ID.
     * Calls SessionController.requirePermission() to verify authorization.
     *
     * @return the doctor's user ID
     * @throws IllegalStateException if the user lacks permission
     */
    private long requireDoctorAccess() {
        sessionController.requirePermission("MANAGE_PRESCRIPTION");
        return sessionController.getCurrentUserId();
    }

    /**
     * Private helper: validates a prescription for persistence.
     * Checks that all required fields are present and clinically valid.
     *
     * @param prescription the Prescription to validate
     * @return list of validation error messages; empty list if valid
     */
    private List<String> validateForPersistence(Prescription prescription) {
        return prescription.validateClinicalContent();
    }

    @GetMapping("/doctor/prescriptions")
    public String prescriptions(@RequestParam(defaultValue = "") String search,
                                @RequestParam(defaultValue = "") String status,
                                Model model) {
        List<Prescription> prescriptions = loadDoctorPrescriptions(sessionController.getCurrentUserId()).stream()
                .filter(rx -> search.isBlank() || String.valueOf(rx.getPrescriptionId()).contains(search)
                        || (rx.getClinicalNotes() != null && rx.getClinicalNotes().toLowerCase().contains(search.toLowerCase())))
                .filter(rx -> status.isBlank() || rx.getStatus().name().equalsIgnoreCase(status))
                .toList();
        model.addAttribute("view", new ManagePrescriptionView(this));
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", status);
        model.addAttribute("filtersActive", !search.isBlank() || !status.isBlank());
        prescriptionPage(model, "Prescriptions");
        return "prescription/list";
    }

    @GetMapping("/doctor/prescriptions/{id}")
    public String prescription(@PathVariable long id, Model model) {
        Prescription prescription = loadPrescription(id);
        if (prescription == null || prescription.getDoctorId() != sessionController.getCurrentUserId()) {
            return "errors/not-found";
        }
        model.addAttribute("view", new ManagePrescriptionView(this));
        model.addAttribute("prescription", prescription);
        model.addAttribute("items", prescription.getItems());
        model.addAttribute("terminal", prescription.isTerminalState());
        prescriptionPage(model, "Prescription " + id);
        return "prescription/detail";
    }

    @GetMapping("/doctor/prescriptions/new")
    public String newPrescription(Model model) {
        sessionController.requirePermission("MANAGE_PRESCRIPTION");
        model.addAttribute("form", new PrescriptionFormView(this));
        model.addAttribute("items", List.of());
        prescriptionPage(model, "Create prescription");
        return "prescription/form";
    }

    @GetMapping("/doctor/prescriptions/{id}/edit")
    public String editPrescription(@PathVariable long id, Model model) {
        Prescription prescription = loadPrescription(id);
        if (prescription == null || prescription.isTerminalState()) return "errors/not-found";
        model.addAttribute("form", new PrescriptionFormView(this));
        model.addAttribute("prescription", prescription);
        model.addAttribute("items", prescription.getItems());
        model.addAttribute("expectedVersion", prescription.getVersion());
        prescriptionPage(model, "Edit prescription");
        return "prescription/form";
    }

    @PostMapping("/doctor/prescriptions")
    public String createPrescriptionWeb(@RequestParam long patientId,
                                        @RequestParam(defaultValue = "") String clinicalNotes,
                                        @RequestParam(name = "medicineId", required = false) List<Long> medicineIds,
                                        @RequestParam(name = "medicineName", required = false) List<String> medicineNames,
                                        @RequestParam(name = "dosage", required = false) List<String> dosages,
                                        @RequestParam(name = "frequency", required = false) List<String> frequencies,
                                        @RequestParam(name = "instructions", required = false) List<String> instructions,
                                        @RequestParam(name = "quantity", required = false) List<Integer> quantities,
                                        Model model, RedirectAttributes redirect) {
        Prescription prescription = createPrescription(patientId);
        prescription.updateClinicalNotes(clinicalNotes);
        addSubmittedItems(prescription, medicineIds, medicineNames, dosages, frequencies, instructions, quantities);
        List<String> errors = validateForPersistence(prescription);
        if (!errors.isEmpty()) {
            model.addAttribute("prescription", prescription);
            model.addAttribute("items", prescription.getItems());
            model.addAttribute("formError", String.join("; ", errors));
            prescriptionPage(model, "Create prescription");
            return "prescription/form";
        }
        Prescription created = prescriptionStorage.create(prescription);
        redirect.addFlashAttribute("flash", new Flash("success", "Prescription created."));
        return "redirect:/doctor/prescriptions/" + created.getPrescriptionId();
    }

    @PostMapping("/doctor/prescriptions/{id}")
    public String updatePrescriptionWeb(@PathVariable long id,
                                        @RequestParam(defaultValue = "") String clinicalNotes,
                                        @RequestParam long expectedVersion,
                                        Model model, RedirectAttributes redirect) {
        Prescription prescription = loadPrescription(id);
        if (prescription == null || prescription.isTerminalState()) return "errors/not-found";
        prescription.updateClinicalNotes(clinicalNotes);
        if (!prescriptionStorage.update(prescription, expectedVersion)) {
            model.addAttribute("prescription", prescription);
            model.addAttribute("items", prescription.getItems());
            model.addAttribute("stale", true);
            prescriptionPage(model, "Edit prescription");
            return "prescription/form";
        }
        redirect.addFlashAttribute("flash", new Flash("success", "Prescription updated."));
        return "redirect:/doctor/prescriptions/" + id;
    }

    @GetMapping("/doctor/prescriptions/{id}/cancel")
    public String cancelPage(@PathVariable long id, Model model) {
        Prescription prescription = loadPrescription(id);
        if (prescription == null) return "errors/not-found";
        model.addAttribute("prescription", prescription);
        prescriptionPage(model, "Cancel prescription");
        return "prescription/cancel";
    }

    @PostMapping("/doctor/prescriptions/{id}/cancel")
    public String cancelWeb(@PathVariable long id, @RequestParam(defaultValue = "") String reason,
                            Model model, RedirectAttributes redirect) {
        if (reason.isBlank() || !cancelPrescription(id, reason)) {
            model.addAttribute("prescription", loadPrescription(id));
            model.addAttribute("reasonError", reason.isBlank() ? "A cancellation reason is required."
                    : "This prescription can no longer be cancelled.");
            prescriptionPage(model, "Cancel prescription");
            return "prescription/cancel";
        }
        redirect.addFlashAttribute("flash", new Flash("success", "Prescription cancelled."));
        return "redirect:/doctor/prescriptions/" + id;
    }

    private void addSubmittedItems(Prescription prescription, List<Long> ids, List<String> names,
                                   List<String> dosages, List<String> frequencies,
                                   List<String> instructions, List<Integer> quantities) {
        if (ids == null) return;
        for (int i = 0; i < Math.min(ids.size(), 50); i++) {
            prescription.addItem(new PrescriptionItem(0L, ids.get(i), value(names, i), value(dosages, i),
                    value(frequencies, i), value(instructions, i), number(quantities, i)));
        }
    }

    private String value(List<String> values, int index) {
        return values != null && index < values.size() ? values.get(index) : "";
    }

    private int number(List<Integer> values, int index) {
        return values != null && index < values.size() ? values.get(index) : 0;
    }

    private void prescriptionPage(Model model, String title) {
        model.addAttribute("title", title);
        model.addAttribute("breadcrumb", "Prescriptions / " + title);
    }
}
