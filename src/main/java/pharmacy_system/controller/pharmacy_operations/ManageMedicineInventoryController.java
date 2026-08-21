package pharmacy_system.controller.pharmacy_operations;

import pharmacy_system.controller.common.SessionController;
import pharmacy_system.model.pharmacy_operations.InventoryItem;
import pharmacy_system.model.pharmacy_operations.Medicine;
import pharmacy_system.model.pharmacy_operations.StockMovement;
import pharmacy_system.model.pharmacy_operations.StockMovementType;
import pharmacy_system.storage.pharmacy_operations.InventoryStorage;
import pharmacy_system.storage.pharmacy_operations.MedicineStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pharmacy_system.view.common.Flash;
import pharmacy_system.view.pharmacy_operations.ucd10_manage_medicine_inventory.MedicineFormView;
import pharmacy_system.view.pharmacy_operations.ucd10_manage_medicine_inventory.StockAdjustmentView;

/**
 * Manages medicine master data, stock receipt, batch/expiry tracking, authorised adjustments,
 * and stock visibility.
 *
 * <p>Implements UCD-10 (Manage Medicine Inventory). Owns medicine master, stock receipt,
 * batch/expiry, authorised adjustments, stock visibility. Does not own prescription authoring,
 * patient notifications, or the full dispensing transaction.
 *
 * <p>Implements Algorithm 5 (Manual Stock Adjustment with Negative-Balance Prevention):
 * manual adjustments require a non-blank reason and are rejected before commit if they
 * would reduce the balance below zero (Requirement 9.5).
 *
 * <p>Requirements:
 * <ul>
 *   <li>FR-044..FR-048: Medicine and inventory management</li>
 *   <li>Property 5 (Non-negative stock): Maintains quantityOnHand >= 0 always</li>
 *   <li>Property 6 (Adjustment reason required): ADJUSTMENT movements have non-blank reason</li>
 *   <li>SC-011: Manual adjustment enforcement</li>
 * </ul>
 */
@Controller
public class ManageMedicineInventoryController {

    private final SessionController session;
    private final MedicineStorage medicineStorage;
    private final InventoryStorage inventoryStorage;

    public ManageMedicineInventoryController(
            SessionController session,
            MedicineStorage medicineStorage,
            InventoryStorage inventoryStorage) {
        this.session = session;
        this.medicineStorage = medicineStorage;
        this.inventoryStorage = inventoryStorage;
    }

    /**
     * Creates a new medicine master record with the given master-data attributes.
     *
     * <p>Persists the medicine in the ACTIVE state. The medicine code (if provided)
     * must be unique within the system (Requirement 9.1, 9.6).
     *
     * <p>Validates all mandatory and optional fields:
     * <ul>
     *   <li>Mandatory: medicineName (1..255 characters)
     *   <li>Optional: medicineCode, genericName, dosageForm, strength, unit, description
     * </ul>
     *
     * @param medicineCode  optional unique medicine code
     * @param medicineName  mandatory medicine name (1..255 characters)
     * @param genericName   optional generic name
     * @param dosageForm    optional dosage form (e.g., "tablet", "syrup")
     * @param strength      optional strength (e.g., "500mg")
     * @param unit          optional unit (e.g., "mg", "ml")
     * @param description   optional description
     * @return Optional containing the created Medicine with assigned medicineId,
     *         or empty if validation fails
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public Optional<Medicine> createMedicine(
            String medicineCode,
            String medicineName,
            String genericName,
            String dosageForm,
            String strength,
            String unit,
            String description) {
        // Enforce permission (Requirement 1.3, 1.4)
        session.requirePermission("MANAGE_INVENTORY");

        // Create the medicine in memory for validation before persistence
        Medicine medicine = new Medicine(
                0L,  // medicineId will be assigned by storage
                medicineCode,
                medicineName,
                genericName,
                dosageForm,
                strength,
                unit,
                description);

        // Validate mandatory fields (Requirement 9.1, 9.7)
        List<String> validationErrors = medicine.validateRequiredFields();
        if (!validationErrors.isEmpty()) {
            return Optional.empty();
        }

        // Check for duplicate medicine code if provided (Requirement 9.6)
        if (medicineCode != null && !medicineCode.isBlank()) {
            Optional<Medicine> existing = medicineStorage.findByCode(medicineCode);
            if (existing.isPresent()) {
                return Optional.empty();  // Code already exists
            }
        }

        // Persist the medicine in the ACTIVE state
        Medicine created = medicineStorage.create(medicine);
        return Optional.of(created);
    }

    /**
     * Updates an existing medicine's editable master-data fields.
     *
     * <p>System identifiers, timestamps, and concurrency version are not editable.
     * Uses optimistic concurrency control: the expectedVersion must match the
     * current stored version to succeed (Requirement 9.7).
     *
     * <p>Valid editable fields: medicineCode, medicineName, genericName, dosageForm,
     * strength, unit, description, active.
     *
     * @param medicineId     the medicine identifier
     * @param changes        field names mapped to their new values
     * @param expectedVersion the version at the time of the last read
     * @return true if the update succeeds; false if the medicine does not exist,
     *         the version conflicts, or validation fails
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     * @throws IllegalArgumentException if a field is unknown or has the wrong type
     */
    public boolean updateMedicine(long medicineId, Map<String, Object> changes, long expectedVersion) {
        // Enforce permission (Requirement 1.3, 1.4)
        session.requirePermission("MANAGE_INVENTORY");

        // Retrieve the current medicine
        Optional<Medicine> medicineOpt = medicineStorage.findById(medicineId);
        if (medicineOpt.isEmpty()) {
            return false;
        }

        Medicine medicine = medicineOpt.get();

        try {
            // Apply the changes; this may throw if fields are invalid or restricted
            medicine.applyChanges(changes);
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Invalid change (unknown or restricted field, or wrong type)
            return false;
        }

        // Validate after changes
        List<String> validationErrors = medicine.validateRequiredFields();
        if (!validationErrors.isEmpty()) {
            return false;
        }

        // Check for duplicate medicine code if code was updated (Requirement 9.6)
        if (changes != null && changes.containsKey("medicineCode")) {
            Object newCode = changes.get("medicineCode");
            if (newCode != null && newCode instanceof String && !((String) newCode).isBlank()) {
                Optional<Medicine> existing = medicineStorage.findByCode((String) newCode);
                if (existing.isPresent() && existing.get().getMedicineId() != medicineId) {
                    return false;  // Code already exists for a different medicine
                }
            }
        }

        // Persist with optimistic concurrency control
        return medicineStorage.update(medicine, expectedVersion);
    }

    /**
     * Receives stock for a medicine as a new batch position.
     *
     * <p>Creates and persists a new InventoryItem (batch) with the given batch number,
     * expiry date, and quantity. The batch is immediately available for dispensing
     * if it has not expired (Requirement 9.2, 9.4).
     *
     * <p>Validates:
     * <ul>
     *   <li>Medicine exists
     *   <li>Batch number is unique for the medicine (Requirement 9.6)
     *   <li>Quantity is greater than 0
     *   <li>Expiry date is set and valid
     * </ul>
     *
     * @param medicineId the medicine identifier
     * @param batchNumber the unique batch identifier for this receipt
     * @param expiryDate the batch expiry date (mandatory)
     * @param quantity   the quantity received (must be > 0)
     * @return Optional containing the created InventoryItem, or empty if validation fails
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public Optional<InventoryItem> receiveStock(
            long medicineId,
            String batchNumber,
            LocalDate expiryDate,
            int quantity) {
        // Enforce permission (Requirement 1.3, 1.4)
        session.requirePermission("MANAGE_INVENTORY");

        // Verify that the medicine exists (Requirement 9.1)
        Optional<Medicine> medicineOpt = medicineStorage.findById(medicineId);
        if (medicineOpt.isEmpty()) {
            return Optional.empty();
        }

        // Create the inventory item in memory for validation before persistence
        InventoryItem inventoryItem = new InventoryItem(
                0L,  // inventoryId will be assigned by storage
                medicineId,
                batchNumber,
                expiryDate,
                quantity);

        // Validate stock data (quantity > 0, expiryDate present) (Requirement 9.2, 9.7)
        List<String> validationErrors = inventoryItem.validateStockData(quantity, expiryDate);
        if (quantity <= 0 || expiryDate == null || !expiryDate.isAfter(LocalDate.now())) {
            return Optional.empty();
        }
        if (!validationErrors.isEmpty()) {
            return Optional.empty();
        }

        // Check for duplicate batch number within the medicine (Requirement 9.6)
        List<InventoryItem> existingBatches = inventoryStorage.findByMedicineId(medicineId);
        for (InventoryItem existing : existingBatches) {
            if (batchNumber != null && batchNumber.equals(existing.getBatchNumber())) {
                return Optional.empty();  // Batch number already exists for this medicine
            }
        }

        // Persist the inventory item
        InventoryItem created = inventoryStorage.create(inventoryItem);
        return Optional.of(created);
    }

    /**
     * Applies a manual stock adjustment to a single batch and records the movement.
     *
     * <p>Implements Algorithm 5 (Manual Stock Adjustment with Negative-Balance Prevention):
     * <ul>
     *   <li>A non-blank reason is mandatory (Requirement 9.5, Property 6)
     *   <li>The adjustment is rejected before commit if it would take the balance below zero
     *       (Requirement 9.5, Property 5)
     *   <li>Uses optimistic concurrency control: expectedVersion must match current version
     *       (Requirement 9.7)
     * </ul>
     *
     * <p>On success, the adjusted InventoryItem and a corresponding StockMovement are persisted
     * together in a single atomic operation. On any failure (missing reason, insufficient balance,
     * version conflict), neither the inventory nor the movement is recorded.
     *
     * @param inventoryId    the inventory ID to adjust
     * @param quantityDelta  the signed quantity change (positive to increase, negative to decrease)
     * @param reason         the mandatory adjustment reason (must be non-blank)
     * @param expectedVersion the version at the time of the last read
     * @return true if the adjustment succeeds; false if the inventory does not exist,
     *         the reason is blank, the adjustment would go negative, the version conflicts,
     *         or the permission is denied
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public boolean adjustStock(long inventoryId, int quantityDelta, String reason, long expectedVersion) {
        // Enforce permission (Requirement 1.3, 1.4)
        session.requirePermission("MANAGE_INVENTORY");

        // Enforce non-blank reason (Algorithm 5, Requirement 9.5, Property 6)
        if (reason == null || reason.isBlank()) {
            return false;
        }

        // Retrieve the current inventory item
        Optional<InventoryItem> inventoryOpt = inventoryStorage.findById(inventoryId);
        if (inventoryOpt.isEmpty()) {
            return false;
        }

        InventoryItem inventoryItem = inventoryOpt.get();

        // Check before mutating whether the adjustment would take the balance negative
        // (Algorithm 5, Requirement 9.5, Property 5)
        if (!inventoryItem.canAdjust(quantityDelta)) {
            return false;  // Reject before deducting
        }

        // Apply the adjustment to the in-memory item
        try {
            inventoryItem.adjust(quantityDelta);
        } catch (IllegalStateException e) {
            // Should not happen due to the canAdjust check, but be defensive
            return false;
        }

        // Record the movement (Algorithm 5, Property 6)
        StockMovement movement = StockMovement.of(
                StockMovementType.ADJUSTMENT,
                0L,  // movementId will be assigned by storage
                inventoryId,
                inventoryItem.getMedicineId(),
                quantityDelta,
                inventoryItem.getQuantityOnHand(),  // balance after adjustment
                reason,
                session.getCurrentUserId());

        // Persist the adjustment and movement together with optimistic concurrency control
        // (Requirement 9.7)
        return inventoryStorage.adjustStock(inventoryItem, movement, expectedVersion);
    }

    /**
     * Retrieves all inventory batch positions (InventoryItem) for a given medicine.
     *
     * <p>Returns all batches regardless of expiry status, but the caller can check
     * {@link InventoryItem#isExpired()} to filter eligible stock for dispensing
     * (Requirement 9.4).
     *
     * @param medicineId the medicine identifier
     * @return a list of all InventoryItems (batches) for the medicine; empty if the
     *         medicine does not exist or has no batches
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public List<InventoryItem> viewInventoryDetails(long medicineId) {
        // Enforce permission (Requirement 1.3, 1.4)
        session.requirePermission("MANAGE_INVENTORY");

        // Return all batches for the medicine (may be empty)
        return inventoryStorage.findByMedicineId(medicineId);
    }

    @GetMapping("/pharmacy/inventory")
    public String inventory(@RequestParam(defaultValue = "") String search, Model model) {
        session.requirePermission("MANAGE_INVENTORY");
        List<Medicine> medicines = medicineStorage.listAll().stream()
                .filter(medicine -> search.isBlank()
                        || medicine.getMedicineName().toLowerCase().contains(search.toLowerCase())
                        || (medicine.getMedicineCode() != null && medicine.getMedicineCode().toLowerCase().contains(search.toLowerCase())))
                .toList();
        Map<Long, Integer> stock = new HashMap<>();
        Map<Long, String> stockStatus = new HashMap<>();
        for (Medicine medicine : medicines) {
            int available = inventoryStorage.findByMedicineId(medicine.getMedicineId()).stream()
                    .mapToInt(InventoryItem::getAvailableQuantity).sum();
            int reorder = inventoryStorage.findByMedicineId(medicine.getMedicineId()).stream()
                    .mapToInt(InventoryItem::getReorderLevel).max().orElse(0);
            stock.put(medicine.getMedicineId(), available);
            stockStatus.put(medicine.getMedicineId(), available == 0 ? "OUT" : available <= reorder ? "LOW" : "NORMAL");
        }
        model.addAttribute("medicines", medicines);
        model.addAttribute("stock", stock);
        model.addAttribute("stockStatus", stockStatus);
        model.addAttribute("search", search);
        model.addAttribute("filtersActive", !search.isBlank());
        inventoryPage(model, "Medicine inventory");
        return "inventory/list";
    }

    @GetMapping("/pharmacy/inventory/{id}")
    public String inventoryDetail(@PathVariable long id, Model model) {
        session.requirePermission("MANAGE_INVENTORY");
        Optional<Medicine> medicine = medicineStorage.findById(id);
        if (medicine.isEmpty()) return "errors/not-found";
        List<InventoryItem> batches = viewInventoryDetails(id);
        model.addAttribute("medicine", medicine.get());
        model.addAttribute("batches", batches);
        model.addAttribute("dispensableStock", batches.stream().mapToInt(InventoryItem::getAvailableQuantity).sum());
        inventoryPage(model, medicine.get().getMedicineName());
        return "inventory/detail";
    }

    @GetMapping("/pharmacy/inventory/new")
    public String newMedicine(Model model) {
        session.requirePermission("MANAGE_INVENTORY");
        model.addAttribute("form", new MedicineFormView());
        inventoryPage(model, "Add medicine");
        return "inventory/form";
    }

    @GetMapping("/pharmacy/inventory/{id}/edit")
    public String editMedicine(@PathVariable long id, Model model) {
        session.requirePermission("MANAGE_INVENTORY");
        Optional<Medicine> medicine = medicineStorage.findById(id);
        if (medicine.isEmpty()) return "errors/not-found";
        model.addAttribute("form", new MedicineFormView());
        model.addAttribute("medicine", medicine.get());
        model.addAttribute("expectedVersion", medicine.get().getVersion());
        inventoryPage(model, "Edit medicine");
        return "inventory/form";
    }

    @PostMapping("/pharmacy/inventory/medicine")
    public String createMedicineWeb(@RequestParam(defaultValue = "") String medicineCode,
                                    @RequestParam(defaultValue = "") String medicineName,
                                    @RequestParam(defaultValue = "") String genericName,
                                    @RequestParam(defaultValue = "") String dosageForm,
                                    @RequestParam(defaultValue = "") String strength,
                                    @RequestParam(defaultValue = "") String unit,
                                    @RequestParam(defaultValue = "") String description,
                                    Model model, RedirectAttributes redirect) {
        Optional<Medicine> created = createMedicine(medicineCode, medicineName, genericName,
                dosageForm, strength, unit, description);
        if (created.isEmpty()) {
            model.addAttribute("medicineCode", medicineCode);
            model.addAttribute("medicineName", medicineName);
            model.addAttribute("duplicateError", "Correct the fields or use a unique medicine code.");
            inventoryPage(model, "Add medicine");
            return "inventory/form";
        }
        redirect.addFlashAttribute("flash", new Flash("success", "Medicine created."));
        return "redirect:/pharmacy/inventory/" + created.get().getMedicineId();
    }

    @PostMapping("/pharmacy/inventory/{id}/medicine")
    public String updateMedicineWeb(@PathVariable long id,
                                    @RequestParam String medicineCode,
                                    @RequestParam String medicineName,
                                    @RequestParam(defaultValue = "") String genericName,
                                    @RequestParam(defaultValue = "") String dosageForm,
                                    @RequestParam(defaultValue = "") String strength,
                                    @RequestParam(defaultValue = "") String unit,
                                    @RequestParam(defaultValue = "") String description,
                                    @RequestParam long expectedVersion,
                                    Model model, RedirectAttributes redirect) {
        Map<String, Object> changes = new HashMap<>();
        changes.put("medicineCode", medicineCode);
        changes.put("medicineName", medicineName);
        changes.put("genericName", genericName);
        changes.put("dosageForm", dosageForm);
        changes.put("strength", strength);
        changes.put("unit", unit);
        changes.put("description", description);
        if (!updateMedicine(id, changes, expectedVersion)) {
            model.addAttribute("stale", true);
            model.addAttribute("medicine", medicineStorage.findById(id).orElse(null));
            inventoryPage(model, "Edit medicine");
            return "inventory/form";
        }
        redirect.addFlashAttribute("flash", new Flash("success", "Medicine updated."));
        return "redirect:/pharmacy/inventory/" + id;
    }

    @GetMapping("/pharmacy/inventory/{id}/stock")
    public String stockAction(@PathVariable long id, Model model) {
        session.requirePermission("MANAGE_INVENTORY");
        Optional<Medicine> medicine = medicineStorage.findById(id);
        if (medicine.isEmpty()) return "errors/not-found";
        populateStockModel(id, model);
        inventoryPage(model, "Receive or adjust stock");
        return "inventory/stock-action";
    }

    @PostMapping("/pharmacy/inventory/{id}/receive")
    public String receive(@PathVariable long id, @RequestParam String batchNumber,
                          @RequestParam LocalDate expiryDate, @RequestParam int quantity,
                          Model model, RedirectAttributes redirect) {
        Optional<InventoryItem> created = receiveStock(id, batchNumber, expiryDate, quantity);
        if (created.isEmpty()) {
            populateStockModel(id, model);
            model.addAttribute("stockError", "Provide a unique batch, future expiry and positive quantity.");
            inventoryPage(model, "Receive or adjust stock");
            return "inventory/stock-action";
        }
        redirect.addFlashAttribute("flash", new Flash("success", "Stock received."));
        return "redirect:/pharmacy/inventory/" + id;
    }

    @PostMapping("/pharmacy/inventory/{inventoryId}/adjust")
    public String adjust(@PathVariable long inventoryId, @RequestParam int quantityDelta,
                         @RequestParam(defaultValue = "") String reason,
                         @RequestParam long expectedVersion, Model model,
                         RedirectAttributes redirect) {
        Optional<InventoryItem> item = inventoryStorage.findById(inventoryId);
        if (reason.isBlank() || item.isEmpty() || !item.get().canAdjust(quantityDelta)
                || !adjustStock(inventoryId, quantityDelta, reason, expectedVersion)) {
            if (item.isPresent()) {
                populateStockModel(item.get().getMedicineId(), model);
                model.addAttribute("failedInventoryId", inventoryId);
            }
            model.addAttribute("reasonError", reason.isBlank() ? "An adjustment reason is required." : null);
            model.addAttribute("stockError", item.isPresent() && !item.get().canAdjust(quantityDelta)
                    ? "Projected stock cannot be negative." : null);
            inventoryPage(model, "Receive or adjust stock");
            return "inventory/stock-action";
        }
        redirect.addFlashAttribute("flash", new Flash("success", "Stock adjusted."));
        return "redirect:/pharmacy/inventory/" + item.get().getMedicineId();
    }

    private void inventoryPage(Model model, String title) {
        model.addAttribute("title", title);
        model.addAttribute("breadcrumb", "Inventory / " + title);
    }

    private void populateStockModel(long medicineId, Model model) {
        model.addAttribute("form", new StockAdjustmentView());
        model.addAttribute("medicine", medicineStorage.findById(medicineId).orElse(null));
        model.addAttribute("batches", inventoryStorage.findByMedicineId(medicineId));
    }
}
