package pharmacy_system.model.pharmacy_operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * Master record for a medicine/medication.
 * Contains medicine information independent of stock quantity.
 * Does not track inventory; InventoryItem tracks individual batches.
 * 
 * [UCD-10]
 */
public class Medicine {
    private long medicineId;
    private String medicineCode;      // unique identifier
    private String medicineName;      // brand or common name (required)
    private String genericName;       // generic/active ingredient
    private String dosageForm;        // e.g. Tablet, Capsule, Injection
    private String strength;          // e.g. 500mg, 10mcg
    private String unit;              // e.g. tablets, ml
    private String description;       // additional description
    private boolean active;           // whether available for prescription
    private long version;             // optimistic locking
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor
    /** Compatibility constructor for name-only medicine master records. */
    public Medicine(long medicineId, String medicineName) {
        this(medicineId, null, medicineName);
    }

    public Medicine(long medicineId, String medicineCode, String medicineName) {
        this.medicineId = medicineId;
        this.medicineCode = medicineCode;
        this.medicineName = medicineName;
        this.genericName = null;
        this.dosageForm = null;
        this.strength = null;
        this.unit = null;
        this.description = null;
        this.active = true;
        this.version = 0L;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Medicine(long medicineId, String medicineCode, String medicineName,
                    String genericName, String dosageForm, String strength,
                    String unit, String description) {
        this(medicineId, medicineCode, medicineName, genericName, dosageForm,
                strength, unit, description, true, 0L);
    }

    public Medicine(long medicineId, String medicineCode, String medicineName,
                    String genericName, String dosageForm, String strength,
                    String unit, String description, boolean active, long version) {
        this.medicineId = medicineId;
        this.medicineCode = medicineCode;
        this.medicineName = medicineName;
        this.genericName = genericName;
        this.dosageForm = dosageForm;
        this.strength = strength;
        this.unit = unit;
        this.description = description;
        this.active = active;
        this.version = version;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // Getters
    public long getMedicineId() {
        return medicineId;
    }

    public String getMedicineCode() {
        return medicineCode;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getGenericName() {
        return genericName;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public String getStrength() {
        return strength;
    }

    public String getUnit() {
        return unit;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setMedicineCode(String medicineCode) {
        if (medicineCode != null && !medicineCode.isBlank()) {
            this.medicineCode = medicineCode;
        }
    }

    public void setMedicineName(String medicineName) {
        if (medicineName != null && !medicineName.isBlank() && medicineName.length() <= 255) {
            this.medicineName = medicineName;
        }
    }

    public void setGenericName(String genericName) {
        if (genericName != null && !genericName.isBlank() && genericName.length() <= 255) {
            this.genericName = genericName;
        }
    }

    public void setDosageForm(String dosageForm) {
        if (dosageForm != null && !dosageForm.isBlank() && dosageForm.length() <= 100) {
            this.dosageForm = dosageForm;
        }
    }

    public void setStrength(String strength) {
        if (strength != null && !strength.isBlank() && strength.length() <= 100) {
            this.strength = strength;
        }
    }

    public void setUnit(String unit) {
        if (unit != null && !unit.isBlank() && unit.length() <= 50) {
            this.unit = unit;
        }
    }

    public void setDescription(String description) {
        if (description != null && !description.isBlank() && description.length() <= 500) {
            this.description = description;
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void incrementVersion() {
        this.version++;
        this.updatedAt = LocalDateTime.now();
    }

    public void applyChanges(Map<String, Object> changes) {
        if (changes == null) return;
        if (changes.get("medicineCode") instanceof String value) setMedicineCode(value);
        if (changes.get("medicineName") instanceof String value) setMedicineName(value);
        if (changes.get("genericName") instanceof String value) setGenericName(value);
        if (changes.get("dosageForm") instanceof String value) setDosageForm(value);
        if (changes.get("strength") instanceof String value) setStrength(value);
        if (changes.get("unit") instanceof String value) setUnit(value);
        if (changes.get("description") instanceof String value) setDescription(value);
        if (changes.get("active") instanceof Boolean value) setActive(value);
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic
    /**
     * Validate medicine for creation or update.
     * @return list of validation error messages; empty list if valid
     */
    public List<String> validateRequiredFields() {
        List<String> errors = new ArrayList<>();

        if (medicineName == null || medicineName.isBlank()) {
            errors.add("Medicine name is required");
        } else if (medicineName.length() > 255) {
            errors.add("Medicine name must not exceed 255 characters");
        }

        if (medicineCode != null && medicineCode.length() > 50) {
            errors.add("Medicine code must not exceed 50 characters");
        }

        if (genericName != null && genericName.length() > 255) {
            errors.add("Generic name must not exceed 255 characters");
        }

        if (dosageForm != null && dosageForm.length() > 100) {
            errors.add("Dosage form must not exceed 100 characters");
        }

        if (strength != null && strength.length() > 100) {
            errors.add("Strength must not exceed 100 characters");
        }

        if (unit != null && unit.length() > 50) {
            errors.add("Unit must not exceed 50 characters");
        }

        if (description != null && description.length() > 500) {
            errors.add("Description must not exceed 500 characters");
        }

        return errors;
    }

    /** Compatibility alias for the complete medicine master-data validation contract. */
    public List<String> validateMedicineInformation() {
        return validateRequiredFields();
    }

    @Override
    public String toString() {
        return "Medicine{" +
                "medicineId=" + medicineId +
                ", medicineCode='" + medicineCode + '\'' +
                ", medicineName='" + medicineName + '\'' +
                ", strength='" + strength + '\'' +
                ", dosageForm='" + dosageForm + '\'' +
                ", active=" + active +
                ", version=" + version +
                '}';
    }
}
