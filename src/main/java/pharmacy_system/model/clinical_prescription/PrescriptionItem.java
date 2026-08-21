package pharmacy_system.model.clinical_prescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Represents one medication entry within a Prescription.
 * Contains medicine information (ID, name), dosage, quantity, frequency, and instructions.
 * 
 * [UCD-01]
 */
public class PrescriptionItem {
    private long prescriptionItemId;
    private long medicineId;
    private String medicineName;
    private String dosage;
    private String dosageUnit;
    private String frequency;
    private String route;
    private int durationDays;
    private String instructions;
    private int quantity;

    // Constructor
    public PrescriptionItem(long prescriptionItemId, long medicineId, String medicineName,
                           String dosage, String dosageUnit, String frequency, String route,
                           int durationDays, String instructions, int quantity) {
        this.prescriptionItemId = prescriptionItemId;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.dosageUnit = dosageUnit;
        this.frequency = frequency;
        this.route = route;
        this.durationDays = durationDays;
        this.instructions = instructions;
        this.quantity = quantity;
    }

    public PrescriptionItem(long prescriptionItemId, long medicineId, String medicineName,
                            String dosage, String frequency, String instructions, int quantity) {
        this(prescriptionItemId, medicineId, medicineName, dosage, "unit", frequency,
                "oral", 1, instructions, quantity);
    }

    public PrescriptionItem(long medicineId, String medicineName, String dosage,
                            int quantity, String frequency, String instructions) {
        this(0L, medicineId, medicineName, dosage, "unit", frequency,
                "oral", 1, instructions, quantity);
    }

    /** Original item-entry field order retained for form and test clients. */
    public PrescriptionItem(long medicineId, String medicineName, String dosage,
                            String frequency, String instructions, int quantity) {
        this(0L, medicineId, medicineName, dosage, "unit", frequency,
                "oral", 1, instructions, quantity);
    }

    // Getters
    public long getPrescriptionItemId() {
        return prescriptionItemId;
    }

    public long getMedicineId() {
        return medicineId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getDosage() {
        return dosage;
    }

    public String getDosageUnit() {
        return dosageUnit;
    }

    public String getFrequency() {
        return frequency;
    }

    public String getRoute() {
        return route;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public String getInstructions() {
        return instructions;
    }

    public int getQuantity() {
        return quantity;
    }

    // Business logic methods per diagram
    /**
     * Validates that all required fields are present and valid.
     * Validates medication data including medicine ID, name, dosage, quantity, frequency.
     * 
     * @return list of validation error messages; empty list if valid
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (medicineId <= 0) {
            errors.add("Medicine ID must be valid");
        }

        if (medicineName == null || medicineName.isBlank()) {
            errors.add("Medicine name is required");
        } else if (medicineName.length() > 255) {
            errors.add("Medicine name must not exceed 255 characters");
        }

        if (dosage == null || dosage.isBlank()) {
            errors.add("Dosage is required");
        } else if (dosage.length() > 100) {
            errors.add("Dosage must not exceed 100 characters");
        }

        if (dosageUnit == null || dosageUnit.isBlank()) {
            errors.add("Dosage unit is required");
        }

        if (frequency == null || frequency.isBlank()) {
            errors.add("Frequency is required");
        } else if (frequency.length() > 100) {
            errors.add("Frequency must not exceed 100 characters");
        }

        if (quantity <= 0) {
            errors.add("Quantity must be greater than 0");
        }

        if (route == null || route.isBlank()) {
            errors.add("Route is required");
        }

        return errors;
    }

    public List<String> validateRequiredFields() {
        List<String> errors = new ArrayList<>();
        if (medicineId <= 0) errors.add("medicineId");
        if (medicineName == null || medicineName.isBlank()) errors.add("medicineName");
        if (dosage == null || dosage.isBlank()) errors.add("dosage");
        if (frequency == null || frequency.isBlank()) errors.add("frequency");
        if (instructions == null || instructions.isBlank()) errors.add("instructions");
        if (quantity <= 0) errors.add("quantity");
        return Collections.unmodifiableList(errors);
    }

    public boolean hasRequiredFields() { return validateRequiredFields().isEmpty(); }

    public void setPrescriptionItemId(long value) { this.prescriptionItemId = value; }
    public void setMedicineId(long value) { this.medicineId = value; }
    public void setMedicineName(String value) { this.medicineName = value; }
    public void setDosage(String value) { this.dosage = value; }
    public void setFrequency(String value) { this.frequency = value; }
    public void setInstructions(String value) { this.instructions = value; }
    public void setQuantity(int value) { this.quantity = value; }

    /**
     * Updates the dosage and dosage unit.
     * 
     * @param dosage the new dosage value
     * @param unit the dosage unit (e.g., "mg", "ml")
     */
    public void updateDosage(String dosage, String unit) {
        if (dosage != null && !dosage.isBlank()) {
            this.dosage = dosage;
        }
        if (unit != null && !unit.isBlank()) {
            this.dosageUnit = unit;
        }
    }

    /**
     * Updates the quantity to dispense.
     * 
     * @param quantity the new quantity (must be > 0)
     */
    public void updateQuantity(int quantity) {
        if (quantity > 0) {
            this.quantity = quantity;
        }
    }

    /**
     * Updates the frequency of administration.
     * 
     * @param frequency the new frequency (e.g., "twice daily")
     */
    public void updateFrequency(String frequency) {
        if (frequency != null && !frequency.isBlank()) {
            this.frequency = frequency;
        }
    }

    /**
     * Updates the instructions for medication administration.
     * 
     * @param instructions the new instructions (e.g., "take with food")
     */
    public void updateInstructions(String instructions) {
        if (instructions != null && !instructions.isBlank()) {
            this.instructions = instructions;
        }
    }

    @Override
    public String toString() {
        return "PrescriptionItem{" +
                "prescriptionItemId=" + prescriptionItemId +
                ", medicineId=" + medicineId +
                ", medicineName='" + medicineName + '\'' +
                ", dosage='" + dosage + '\'' +
                ", frequency='" + frequency + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
