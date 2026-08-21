package pharmacy_system.controller.pharmacy_operations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pharmacy_system.view.common.Flash;
import pharmacy_system.view.pharmacy_operations.ucd08_dispense_medication.DispenseFormView;

import pharmacy_system.controller.common.SessionController;
import pharmacy_system.model.pharmacy_operations.DispenseRecord;
import pharmacy_system.model.pharmacy_operations.InventoryItem;
import pharmacy_system.model.pharmacy_operations.StockMovement;
import pharmacy_system.model.pharmacy_operations.StockMovementType;
import pharmacy_system.storage.pharmacy_operations.DispenseStorage;
import pharmacy_system.storage.pharmacy_operations.InventoryStorage;
import pharmacy_system.storage.clinical_prescription.PrescriptionStorage;
import pharmacy_system.storage.security_user.UserProfileStorage;

/**
 * Orchestrates the dispensing transaction. Implements Algorithm 2 from design.md:
 * guard sequence (eligibility, duplicate-fulfilment block, patient verification,
 * plan-all-before-deduct, single deduction with optimistic concurrency, completion,
 * and dispensed domain event).
 *
 * <p>The FEFO (First-Expiry-First-Out) allocation planner at the core ensures
 * no over-dispensing (no partial), no negative stock, and earliest-expiry-first
 * consumption, as per Requirements 8.1-8.3, 8.5.
 */
@Controller
public class DispenseMedicationController {

    /**
     * Functional interface for domain event publishing.
     * Allows decoupled event publication to external listeners (e.g., notifications).
     */
    @FunctionalInterface
    public interface DomainEventPublisher {
        void publish(String eventType, long prescriptionId, long patientId, String recipient, boolean patientActive);
    }

    private final SessionController sessionController;
    private final DispenseStorage dispenseStorage;
    private final InventoryStorage inventoryStorage;
    private final PrescriptionStorage prescriptionStorage;
    private final UserProfileStorage userProfileStorage;
    private DomainEventPublisher eventPublisher;

    public DispenseMedicationController(
            SessionController sessionController,
            DispenseStorage dispenseStorage,
            InventoryStorage inventoryStorage,
            PrescriptionStorage prescriptionStorage) {
        this.sessionController = sessionController;
        this.dispenseStorage = dispenseStorage;
        this.inventoryStorage = inventoryStorage;
        this.prescriptionStorage = prescriptionStorage;
        this.userProfileStorage = null; // Will be set via setter or constructor overload
        this.eventPublisher = (eventType, prescriptionId, patientId, recipient, patientActive) -> {
            // Default no-op publisher; override with setDomainEventPublisher
        };
    }

    @Autowired
    public DispenseMedicationController(
            SessionController sessionController,
            DispenseStorage dispenseStorage,
            InventoryStorage inventoryStorage,
            PrescriptionStorage prescriptionStorage,
            UserProfileStorage userProfileStorage) {
        this.sessionController = sessionController;
        this.dispenseStorage = dispenseStorage;
        this.inventoryStorage = inventoryStorage;
        this.prescriptionStorage = prescriptionStorage;
        this.userProfileStorage = userProfileStorage;
        this.eventPublisher = (eventType, prescriptionId, patientId, recipient, patientActive) -> {
            // Default no-op publisher; override with setDomainEventPublisher
        };
    }

    /**
     * Sets the domain event publisher callback for dispensing events.
     * Allows wiring external event listeners (e.g., notifications controller).
     *
     * @param publisher the event publisher callback
     */
    public void setDomainEventPublisher(DomainEventPublisher publisher) {
        this.eventPublisher = publisher != null ? publisher : (e, p, pa, r, pact) -> {
            // no-op if null passed
        };
    }

    /**
     * FEFO (First-Expiry-First-Out) stock allocation planner (Algorithm 1).
     *
     * <p>Implements Requirements 8.1 (allocation conservation), 8.2 (no over-dispensing),
     * 8.3 (FEFO ordering), 8.5 (exclude expired), and the three related properties:
     * <ul>
     *   <li>Property 1: No over-dispensing (insufficient eligible stock → empty plan, no deduction)</li>
     *   <li>Property 2: FEFO ordering (allocated batches ordered by ascending expiry, no expired)</li>
     *   <li>Property 3: Allocation conservation (sum of quantities = required quantity exactly)</li>
     * </ul>
     *
     * @param batches      list of inventory batches for the same medicine
     * @param requiredQty  the total quantity to allocate (must be > 0)
     * @param today        the reference date for expiry evaluation
     * @return an allocation plan (inventoryId → qty) summing exactly to requiredQty,
     *         or empty if total eligible (non-expired) stock is insufficient
     * @throws IllegalArgumentException if requiredQty <= 0 or batches is null
     *
     * <p>Precondition: requiredQty > 0; batches all belong to the same medicine.
     * <p>Postcondition: returns a plan whose quantities sum exactly to requiredQty
     * allocated from earliest-expiring eligible batches, or empty when eligible
     * stock is insufficient; input batches are not mutated.
     */
    public Optional<Map<Long, Integer>> planFefoAllocation(
            List<InventoryItem> batches,
            int requiredQty,
            LocalDate today) {
        if (batches == null) {
            throw new IllegalArgumentException("batches cannot be null");
        }
        if (requiredQty <= 0) {
            throw new IllegalArgumentException("requiredQty must be > 0");
        }

        // Filter eligible batches: not expired and have available quantity > 0.
        // Sort by earliest expiry date first, then by inventoryId for deterministic tiebreak.
        List<InventoryItem> eligible = batches.stream()
                .filter(b -> !b.isExpired() && b.getAvailableQuantity() > 0)
                .sorted((a, b) -> {
                    int expiryCompare = a.getExpiryDate().compareTo(b.getExpiryDate());
                    if (expiryCompare != 0) {
                        return expiryCompare;  // earliest expiry first
                    }
                    // Tiebreak: by inventoryId for determinism (FR-040, SC-008)
                    return Long.compare(a.getInventoryId(), b.getInventoryId());
                })
                .toList();

        // Compute total eligible stock
        int totalEligible = eligible.stream().mapToInt(InventoryItem::getAvailableQuantity).sum();

        // Insufficient eligible stock: return empty, no deduction happens (FR-038, FR-039)
        if (totalEligible < requiredQty) {
            return Optional.empty();
        }

        // Allocate greedily from earliest-expiring batches (FEFO)
        Map<Long, Integer> plan = new LinkedHashMap<>();
        int remaining = requiredQty;

        // Loop invariant: remaining == requiredQty - (sum of quantities already placed in plan)
        // and every batch already visited has expiry <= current batch (FEFO order maintained)
        for (InventoryItem batch : eligible) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(remaining, batch.getAvailableQuantity());
            plan.put(batch.getInventoryId(), take);
            remaining -= take;
        }

        // Postcondition: remaining == 0 and sum(plan.values()) == requiredQty
        assert remaining == 0 : "Allocation incomplete: " + remaining + " units unallocated";
        assert plan.values().stream().mapToInt(Integer::intValue).sum() == requiredQty
                : "Allocation quantity mismatch";

        return Optional.of(plan);
    }

    /**
     * Requests an eligible prescription for dispensing, blocking if already dispensed
     * or not yet issued (Requirements 8.4, FR-042, FR-035).
     *
     * <p>Guards:</p>
     * <ul>
     *   <li>Eligibility (issued, not expired, not cancelled) — FR-035, FR-022</li>
     *   <li>Duplicate fulfilment block — FR-042, SC-009</li>
     * </ul>
     *
     * @param prescriptionId the prescription to dispense
     * @return a DispenseRecord ready for verification, or null if blocked
     */
    public DispenseRecord requestEligiblePrescription(long prescriptionId) {
        sessionController.requirePermission("DISPENSE_MEDICATION");

        // Cross-domain read: load the prescription
        var optPrescription = prescriptionStorage.findById(prescriptionId);
        if (optPrescription.isEmpty()) {
            return null;  // Prescription not found
        }

        var prescription = optPrescription.get();

        // Guard 1: eligibility (issued, not expired, not cancelled) — FR-035, FR-022
        if (!prescription.isEligibleForDispensing()) {
            return null;  // Prescription not eligible (not issued, expired, or cancelled)
        }

        // Guard 2: duplicate fulfilment block — FR-042, SC-009
        if (dispenseStorage.existsCompletedDispense(prescriptionId)) {
            return null;  // Already dispensed
        }

        // Build required quantities from prescription items
        Map<Long, Integer> requiredQuantities = new HashMap<>();
        for (var item : prescription.getItems()) {
            requiredQuantities.put(item.getMedicineId(), item.getQuantity());
        }

        // Create the dispense record in PENDING state
        DispenseRecord record = new DispenseRecord(
                0L,  // dispenseId assigned by storage
                prescriptionId,
                prescription.getPatientId(),
                requiredQuantities,
                LocalDateTime.now());

        return dispenseStorage.create(record);
    }

    /**
     * Verifies patient and prescription match, and validates that quantities
     * correspond to prescription items (Requirements FR-036, FR-041).
     *
     * @param prescriptionId the prescription being dispensed
     * @param patientId      the patient's identifier
     * @return true if verification passes
     */
    public boolean verifyPrescriptionAndPatient(long prescriptionId, long patientId) {
        sessionController.requirePermission("DISPENSE_MEDICATION");
        
        var optPrescription = prescriptionStorage.findById(prescriptionId);
        if (optPrescription.isEmpty()) {
            return false;
        }

        var prescription = optPrescription.get();
        return prescription.getPatientId() == patientId;
    }

    /**
     * Checks whether required stock is available across all items, using FEFO
     * allocation (Requirements 8.2, 8.3, 8.5, FR-039).
     *
     * @param quantities medicineId → required quantity map
     * @return true if sufficient eligible stock exists for all items
     */
    public boolean checkStockAvailability(Map<Long, Integer> quantities) {
        sessionController.requirePermission("DISPENSE_MEDICATION");

        LocalDate today = LocalDate.now();
        for (var entry : quantities.entrySet()) {
            long medicineId = entry.getKey();
            int requiredQty = entry.getValue();

            // Retrieve all batches for this medicine
            List<InventoryItem> batches = inventoryStorage.findByMedicineId(medicineId);

            // Plan FEFO allocation
            Optional<Map<Long, Integer>> plan = planFefoAllocation(batches, requiredQty, today);
            if (plan.isEmpty()) {
                return false;  // Insufficient eligible stock for this medicine
            }
        }

        return true;  // All items have sufficient stock
    }

    /**
     * Confirms and commits a dispensing transaction with a single FEFO-allocated
     * inventory deduction and optimistic concurrency handling (Algorithm 2,
     * Requirements 8.1, 8.3, 8.6, 8.7, FR-039, FR-041, FR-043).
     *
     * <p>This method orchestrates the complete dispensing flow:</p>
     * <ul>
     *   <li>Load the dispense record</li>
     *   <li>Verify patient (FR-036)</li>
     *   <li>Plan all FEFO allocations before any deduction (FR-039)</li>
     *   <li>Single atomic inventory deduction with optimistic concurrency (FR-041, FR-043)</li>
     *   <li>Complete fulfilment and publish domain event</li>
     * </ul>
     *
     * @param dispenseId the DispenseRecord to complete
     * @param quantities medicineId → dispensed quantity map (must match required)
     * @return the completed DispenseRecord with status DISPENSED, or FAILED if any guard fails
     */
    public DispenseRecord confirmDispensing(long dispenseId, Map<Long, Integer> quantities) {
        sessionController.requirePermission("DISPENSE_MEDICATION");

        // Load the dispense record
        var optRecord = dispenseStorage.findById(dispenseId);
        if (optRecord.isEmpty()) {
            return null;
        }

        DispenseRecord record = optRecord.get();

        // Verify patient (FR-036)
        var optPrescription = prescriptionStorage.findById(record.getPrescriptionId());
        if (optPrescription.isEmpty()) {
            record.markFailed("Prescription not found");
            return record;
        }

        var prescription = optPrescription.get();
        if (!record.verifyPatient(prescription.getPatientId())) {
            dispenseStorage.update(record, record.getVersion() - 1);  // Persist the failure
            return record;
        }

        // Persist the VERIFIED state
        if (!dispenseStorage.update(record, record.getVersion() - 1)) {
            record.markFailed("Concurrent update conflict during verification");
            return record;
        }

        try {
            record.confirmDispensing(quantities);
        } catch (IllegalStateException | IllegalArgumentException e) {
            record.markFailed("Dispensing confirmation failed: " + e.getMessage());
            dispenseStorage.update(record, record.getVersion() - 1);
            return record;
        }

        return record;
    }

    /**
     * Completes the dispensing fulfilment, deducting inventory and publishing
     * a "Medication Dispensed" domain event (Requirements 8.1, 8.6, 8.7, FR-043).
     *
     * <p>This is the main Algorithm 2 orchestration:</p>
     * <ul>
     *   <li>Load dispense record and prescription</li>
     *   <li>Guard 1: eligibility (issued, not expired, not cancelled) — FR-035, FR-022</li>
     *   <li>Guard 2: duplicate fulfilment block — FR-042, SC-009</li>
     *   <li>Guard 3: patient verification — FR-036</li>
     *   <li>Plan all FEFO allocations BEFORE any deduction — FR-039</li>
     *   <li>Single atomic deduction with optimistic concurrency — FR-041, FR-043</li>
     *   <li>Complete fulfilment and publish domain event</li>
     * </ul>
     *
     * @param dispenseId the DispenseRecord to complete
     * @return the completed DispenseRecord (status DISPENSED on success, FAILED on error)
     */
    public DispenseRecord completeDispensing(long dispenseId) {
        sessionController.requirePermission("DISPENSE_MEDICATION");

        // Load the dispense record
        var optRecord = dispenseStorage.findById(dispenseId);
        if (optRecord.isEmpty()) {
            return null;
        }

        DispenseRecord record = optRecord.get();

        // Load the prescription (cross-domain read)
        var optPrescription = prescriptionStorage.findById(record.getPrescriptionId());
        if (optPrescription.isEmpty()) {
            record.markFailed("Prescription not found");
            dispenseStorage.update(record, record.getVersion() - 1);
            return record;
        }

        var prescription = optPrescription.get();

        // Guard 1: eligibility (issued, not expired, not cancelled) — FR-035, FR-022
        if (!prescription.isEligibleForDispensing()) {
            record.markFailed("Prescription not eligible (not issued, expired, or cancelled)");
            dispenseStorage.update(record, record.getVersion() - 1);
            return record;
        }

        // Guard 2: duplicate fulfilment block — FR-042, SC-009
        if (dispenseStorage.existsCompletedDispense(prescription.getPrescriptionId())) {
            record.markFailed("Prescription already dispensed");
            dispenseStorage.update(record, record.getVersion() - 1);
            return record;
        }

        // Guard 3: patient verification — FR-036
        if (record.getPatientId() != prescription.getPatientId()) {
            record.markFailed("Patient verification mismatch");
            dispenseStorage.update(record, record.getVersion() - 1);
            return record;
        }

        // Plan all FEFO allocations BEFORE any deduction — FR-039
        LocalDate today = LocalDate.now();
        Map<Long, Map<Long, Integer>> allocationByMedicine = new HashMap<>();

        for (var entry : record.getRequiredQuantities().entrySet()) {
            long medicineId = entry.getKey();
            int requiredQty = entry.getValue();

            // Retrieve all batches for this medicine
            List<InventoryItem> batches = inventoryStorage.findByMedicineId(medicineId);

            // Plan FEFO allocation
            Optional<Map<Long, Integer>> plan = planFefoAllocation(batches, requiredQty, today);
            if (plan.isEmpty()) {
                // Insufficient eligible stock: block, no deduction
                record.markFailed("Insufficient stock for medicine " + medicineId);
                dispenseStorage.update(record, record.getVersion() - 1);
                return record;
            }

            allocationByMedicine.put(medicineId, plan.get());
        }

        // Flatten the allocation plan into a single (inventoryId -> quantity) map
        Map<Long, Integer> flatAllocation = new HashMap<>();
        for (var allocsByMedicine : allocationByMedicine.values()) {
            flatAllocation.putAll(allocsByMedicine);
        }

        // Single atomic deduction with optimistic concurrency — FR-041, FR-043
        // Create a single StockMovement record for this dispensing transaction
        StockMovement movement = StockMovement.of(
                StockMovementType.DISPENSE,
                0L,  // movementId assigned by storage
                0L,  // inventoryId not used for multi-batch movement (included in allocationPlan)
                0L,  // medicineId not used for multi-medicine movement
                0,   // quantityDelta not used (implicit in allocationPlan)
                0,   // balanceAfter not used (per-batch in InventoryItem)
                "Dispensing for prescription " + prescription.getPrescriptionId(),
                sessionController.getCurrentUserId());

        boolean deducted = inventoryStorage.deductInventory(flatAllocation, movement);
        if (!deducted) {
            // Version conflict or other deduction failure: recompute and retry
            record.markFailed("Concurrent stock change detected - please reload and retry");
            dispenseStorage.update(record, record.getVersion() - 1);
            return record;
        }

        // Completion: mark as DISPENSED and persist
        record.completeFulfilment(sessionController.getCurrentUserId());

        long recordVersion = record.getVersion();
        if (!dispenseStorage.update(record, recordVersion - 1)) {
            // Version conflict on dispense record itself (unlikely but possible)
            record.markFailed("Concurrent update on dispense record");
            return record;
        }

        // Publish "Medication Dispensed" domain event (FR-043, UCD-03)
        // Note: Event publishing is deferred to UCD-03 SendAlertsNotificationsController
        // This is a placeholder for integration with the notification system.
        publishMedicationDispensedEvent(record, prescription);

        return record;
    }

    @GetMapping("/pharmacy/dispensing")
    public String queue(Model model) {
        sessionController.requirePermission("DISPENSE_MEDICATION");
        List<pharmacy_system.model.clinical_prescription.Prescription> prescriptions = prescriptionStorage.listAll();
        Map<Long, String> fulfilmentByPrescription = new HashMap<>();
        Map<Long, String> blockReason = new HashMap<>();
        Map<Long, Boolean> actionable = new HashMap<>();
        dispenseStorage.listAll().forEach(record -> fulfilmentByPrescription.put(record.getPrescriptionId(), record.getStatus()));
        prescriptions.forEach(rx -> {
            boolean canDispense = rx.isEligibleForDispensing() && !dispenseStorage.existsCompletedDispense(rx.getPrescriptionId());
            actionable.put(rx.getPrescriptionId(), canDispense);
            if (!canDispense) blockReason.put(rx.getPrescriptionId(),
                    rx.getStatus() == pharmacy_system.model.clinical_prescription.PrescriptionStatus.CANCELLED ? "Cancelled" :
                    rx.isTerminalState() ? "Expired" :
                    dispenseStorage.existsCompletedDispense(rx.getPrescriptionId()) ? "Dispensed" : "Pending");
        });
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("fulfilmentByPrescription", fulfilmentByPrescription);
        model.addAttribute("blockReason", blockReason);
        model.addAttribute("actionable", actionable);
        dispensingPage(model, "Dispensing queue");
        return "dispensing/queue";
    }

    @GetMapping("/pharmacy/dispensing/prescription/{id}")
    public String start(@PathVariable long id, RedirectAttributes redirect) {
        DispenseRecord record = requestEligiblePrescription(id);
        if (record == null) {
            redirect.addFlashAttribute("flash", new Flash("warning",
                    "This prescription is cancelled, expired, ineligible or already dispensed."));
            return "redirect:/pharmacy/dispensing";
        }
        return "redirect:/pharmacy/dispensing/" + record.getDispenseId() + "/verify";
    }

    @GetMapping("/pharmacy/dispensing/{id}/verify")
    public String verify(@PathVariable long id, Model model) {
        sessionController.requirePermission("DISPENSE_MEDICATION");
        var record = dispenseStorage.findById(id);
        if (record.isEmpty()) return "errors/not-found";
        model.addAttribute("form", new DispenseFormView());
        model.addAttribute("record", record.get());
        model.addAttribute("sufficientStock", checkStockAvailability(record.get().getRequiredQuantities()));
        dispensingPage(model, "Verify and dispense");
        return "dispensing/verify";
    }

    @PostMapping("/pharmacy/dispensing/{id}/confirm")
    public String confirm(@PathVariable long id) {
        sessionController.requirePermission("DISPENSE_MEDICATION");
        var record = dispenseStorage.findById(id);
        if (record.isEmpty()) return "redirect:/pharmacy/dispensing";
        DispenseRecord confirmed = confirmDispensing(id, record.get().getRequiredQuantities());
        if (confirmed != null && !DispenseRecord.STATUS_FAILED.equals(confirmed.getStatus())) {
            completeDispensing(id);
        }
        return "redirect:/pharmacy/dispensing/" + id + "/result";
    }

    @GetMapping("/pharmacy/dispensing/{id}/result")
    public String result(@PathVariable long id, Model model) {
        sessionController.requirePermission("DISPENSE_MEDICATION");
        var record = dispenseStorage.findById(id);
        if (record.isEmpty()) return "errors/not-found";
        model.addAttribute("record", record.get());
        model.addAttribute("success", record.get().isCompleted());
        dispensingPage(model, "Dispensing result");
        return "dispensing/result";
    }

    private void dispensingPage(Model model, String title) {
        model.addAttribute("title", title);
        model.addAttribute("breadcrumb", "Dispensing / " + title);
    }

    /**
     * Helper method to publish a domain event when medication is dispensed.
     * Publishes to external listeners via the registered event publisher callback.
     *
     * @param record the completed DispenseRecord
     * @param prescription the associated Prescription
     */
    private void publishMedicationDispensedEvent(DispenseRecord record, pharmacy_system.model.clinical_prescription.Prescription prescription) {
        // Retrieve the patient's profile to get their email/recipient
        var optPatientProfile = userProfileStorage.findById(record.getPatientId());
        String recipient = optPatientProfile.isPresent() ? optPatientProfile.get().getContactEmail() : "unknown";
        
        // Publish the "Medication Dispensed" event to registered listeners
        // (primarily the SendAlertsNotificationsController for patient notifications)
        eventPublisher.publish(
                "MEDICATION_DISPENSED",
                prescription.getPrescriptionId(),
                record.getPatientId(),
                recipient,
                true  // assuming patient is active when dispensing occurs
        );
    }
}
