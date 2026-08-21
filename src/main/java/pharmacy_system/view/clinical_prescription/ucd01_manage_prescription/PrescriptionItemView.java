package pharmacy_system.view.clinical_prescription.ucd01_manage_prescription;

import pharmacy_system.controller.clinical_prescription.ManagePrescriptionController;
import pharmacy_system.model.clinical_prescription.PrescriptionItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Item-level view for prescription item (medication) data collection and display (UCD-01).
 * Each prescription contains 1 or more items. This view handles individual item input/display.
 * Surfaces field validation errors.
 *
 * Requirement traceability: 4.1 (items with 1..50 items), 4.6 (validation errors)
 */
public class PrescriptionItemView {

    private final ManagePrescriptionController controller;
    private long medicineId;
    private String medicineName = "";
    private String dosage = "";
    private int quantity;
    private String frequency = "";
    private String instructions = "";
    private String lastValidationError = "";

    public PrescriptionItemView(ManagePrescriptionController controller) {
        this.controller = controller;
    }

    /**
     * Collects item data from the form as a map.
     * Keys: medicineId (long), medicineName, dosage, quantity (int), frequency, instructions (all String).
     * Requirements: 4.1 (prescription items with required fields)
     */
    public Map<String, Object> collectItemData() {
        Map<String, Object> data = new HashMap<>();
        data.put("medicineId", this.medicineId);
        data.put("medicineName", this.medicineName);
        data.put("dosage", this.dosage);
        data.put("quantity", this.quantity);
        data.put("frequency", this.frequency);
        data.put("instructions", this.instructions);
        return data;
    }

    /**
     * Populates the item form with an existing PrescriptionItem's data.
     * Requirements: 4.1 (view prescription items)
     */
    public void populateItem(PrescriptionItem item) {
        if (item == null) {
            this.lastValidationError = "Item is null";
            return;
        }
        this.medicineId = item.getMedicineId();
        this.medicineName = item.getMedicineName();
        this.dosage = item.getDosage();
        this.quantity = item.getQuantity();
        this.frequency = item.getFrequency();
        this.instructions = item.getInstructions();
    }

    /**
     * Shows field validation errors for this item.
     * Requirements: 4.6 (validation error indication)
     */
    public void showItemValidationErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            this.lastValidationError = "Item validation error";
        } else {
            this.lastValidationError = "Item errors: " + String.join("; ", errors);
        }
    }

    /**
     * Gets the medicine ID.
     * For testing purposes.
     */
    public long getMedicineId() {
        return this.medicineId;
    }

    /**
     * Sets the medicine ID.
     * For testing purposes.
     */
    public void setMedicineId(long medicineId) {
        this.medicineId = medicineId;
    }

    /**
     * Gets the medicine name.
     * For testing purposes.
     */
    public String getMedicineName() {
        return this.medicineName;
    }

    /**
     * Sets the medicine name.
     * For testing purposes.
     */
    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    /**
     * Gets the dosage.
     * For testing purposes.
     */
    public String getDosage() {
        return this.dosage;
    }

    /**
     * Sets the dosage.
     * For testing purposes.
     */
    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    /**
     * Gets the quantity.
     * For testing purposes.
     */
    public int getQuantity() {
        return this.quantity;
    }

    /**
     * Sets the quantity.
     * For testing purposes.
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Gets the frequency.
     * For testing purposes.
     */
    public String getFrequency() {
        return this.frequency;
    }

    /**
     * Sets the frequency.
     * For testing purposes.
     */
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    /**
     * Gets the instructions.
     * For testing purposes.
     */
    public String getInstructions() {
        return this.instructions;
    }

    /**
     * Sets the instructions.
     * For testing purposes.
     */
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    /**
     * Gets the last validation error message.
     * For testing purposes.
     */
    public String getLastValidationError() {
        return this.lastValidationError;
    }
}
