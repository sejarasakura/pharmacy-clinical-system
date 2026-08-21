package pharmacy_system.model.clinical_prescription;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Aggregate root for clinical prescription.
 * Represents one prescription for one patient written by one doctor.
 * Contains one or more prescription items (medicines).
 * 
 * Manages clinical lifecycle (DRAFT -> ISSUED -> ON_HOLD -> CANCELLED/EXPIRED).
 * Tracks clinical status changes with audit trail (who changed it, when, why).
 * Carries version field for optimistic concurrency control.
 * 
 * [UCD-01, UCD-07]
 */
public class Prescription {
    private long prescriptionId;
    private long patientId;
    private long doctorId;
    private String clinicalNotes;
    private PrescriptionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private LocalDateTime issuedAt;
    private LocalDateTime statusChangedAt;
    private long statusChangedBy;
    private String statusChangeReason;
    private long version;
    private List<PrescriptionItem> items;

    // Constructor
    public Prescription(long prescriptionId, long patientId, long doctorId, String clinicalNotes) {
        this.prescriptionId = prescriptionId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.clinicalNotes = clinicalNotes;
        this.status = PrescriptionStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.cancelledAt = null;
        this.cancellationReason = null;
        this.version = 1L;
        this.items = new ArrayList<>();
    }

    public Prescription(long patientId, long doctorId, List<PrescriptionItem> items) {
        this(patientId, doctorId, items, null);
    }

    public Prescription(long patientId, long doctorId, List<PrescriptionItem> items,
                        String clinicalNotes) {
        this.prescriptionId = 0L;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.clinicalNotes = clinicalNotes;
        this.status = PrescriptionStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.version = 0L;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public Prescription(long prescriptionId, long patientId, long doctorId, String clinicalNotes,
                        PrescriptionStatus status, LocalDateTime issuedAt,
                        LocalDateTime statusChangedAt, long statusChangedBy,
                        String statusChangeReason, LocalDateTime createdAt,
                        LocalDateTime updatedAt, long version, List<PrescriptionItem> items) {
        this.prescriptionId = prescriptionId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.clinicalNotes = clinicalNotes;
        this.status = status;
        this.issuedAt = issuedAt;
        this.statusChangedAt = statusChangedAt;
        this.statusChangedBy = statusChangedBy;
        this.statusChangeReason = statusChangeReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.cancelledAt = status == PrescriptionStatus.CANCELLED ? statusChangedAt : null;
        this.cancellationReason = status == PrescriptionStatus.CANCELLED ? statusChangeReason : null;
        this.version = version;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    // Getters
    public long getPrescriptionId() {
        return prescriptionId;
    }

    public long getPatientId() {
        return patientId;
    }

    public long getDoctorId() {
        return doctorId;
    }

    public String getClinicalNotes() {
        return clinicalNotes;
    }

    public PrescriptionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getStatusChangedAt() { return statusChangedAt; }
    public long getStatusChangedBy() { return statusChangedBy; }
    public String getStatusChangeReason() { return statusChangeReason; }

    public long getVersion() {
        return version;
    }

    public List<PrescriptionItem> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    // Business logic methods per diagram

    /**
     * Adds a prescription item to this prescription.
     * 
     * @param item the PrescriptionItem to add
     */
    public void addItem(PrescriptionItem item) {
        if (item != null) {
            items.add(item);
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Updates an existing prescription item's fields.
     * 
     * @param itemId the ID of the item to update
     * @param changes a map of field names to new values
     * @return true if the item was found and updated, false otherwise
     */
    public boolean updateItem(long itemId, Map<String, Object> changes) {
        PrescriptionItem item = findItem(itemId);
        if (item == null) {
            return false;
        }

        // Apply changes to the item
        if (changes.containsKey("dosage") && changes.get("dosage") instanceof String) {
            String unit = (String) changes.getOrDefault("dosageUnit", item.getDosageUnit());
            item.updateDosage((String) changes.get("dosage"), unit);
        }

        if (changes.containsKey("quantity") && changes.get("quantity") instanceof Integer) {
            item.updateQuantity((Integer) changes.get("quantity"));
        }

        if (changes.containsKey("frequency") && changes.get("frequency") instanceof String) {
            item.updateFrequency((String) changes.get("frequency"));
        }

        if (changes.containsKey("instructions") && changes.get("instructions") instanceof String) {
            item.updateInstructions((String) changes.get("instructions"));
        }

        this.updatedAt = LocalDateTime.now();
        return true;
    }

    /**
     * Removes a prescription item by ID.
     * 
     * @param itemId the ID of the item to remove
     * @return true if the item was found and removed, false otherwise
     */
    public boolean removeItem(long itemId) {
        boolean removed = items.removeIf(item -> item.getPrescriptionItemId() == itemId);
        if (removed) {
            this.updatedAt = LocalDateTime.now();
        }
        return removed;
    }

    /**
     * Finds a prescription item by ID.
     * 
     * @param itemId the ID of the item to find
     * @return the PrescriptionItem if found, null otherwise
     */
    public PrescriptionItem findItem(long itemId) {
        return items.stream()
                .filter(item -> item.getPrescriptionItemId() == itemId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Updates the clinical notes for this prescription.
     * 
     * @param notes the new clinical notes
     */
    public void updateClinicalNotes(String notes) {
        if (notes != null && !notes.isBlank()) {
            this.clinicalNotes = notes;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Validates the clinical content of the prescription.
     * Checks that all items are valid and prescription has required fields.
     * 
     * @return list of validation error messages; empty list if valid
     */
    public List<String> validateClinicalContent() {
        List<String> errors = new ArrayList<>();

        if (patientId <= 0) {
            errors.add("Patient ID must be valid");
        }

        if (doctorId <= 0) {
            errors.add("Doctor ID must be valid");
        }

        if (items == null || items.isEmpty()) {
            errors.add("Prescription must contain at least 1 item");
        } else if (items.size() > 50) {
            errors.add("Prescription must not exceed 50 items");
        } else {
            // Validate each item
            for (PrescriptionItem item : items) {
                List<String> itemErrors = item.validate();
                errors.addAll(itemErrors);
            }
        }

        if (status == null) {
            errors.add("Prescription status is required");
        }

        return errors;
    }

    /** Returns stable field paths suitable for presentation-layer validation. */
    public List<String> validateRequiredFields() {
        List<String> errors = new ArrayList<>();
        if (patientId <= 0) errors.add("patientId");
        if (doctorId <= 0) errors.add("doctorId");
        if (items == null || items.isEmpty() || items.size() > 50) {
            errors.add("items");
        } else {
            for (int index = 0; index < items.size(); index++) {
                if (items.get(index) == null || !items.get(index).validate().isEmpty()) {
                    errors.add("items[" + index + "]");
                }
            }
        }
        return Collections.unmodifiableList(errors);
    }

    public boolean hasRequiredFields() {
        return validateRequiredFields().isEmpty();
    }

    /**
     * Checks if the prescription can be edited.
     * Only DRAFT and ISSUED prescriptions can be edited.
     * 
     * @return true if the prescription can be edited, false if it is in terminal state
     */
    public boolean canBeEdited() {
        return status != PrescriptionStatus.CANCELLED && status != PrescriptionStatus.EXPIRED;
    }

    /**
     * Checks if the prescription can be cancelled.
     * Prescriptions in DRAFT, ISSUED, or ON_HOLD status can be cancelled.
     * 
     * @return true if the prescription can be cancelled, false otherwise
     */
    public boolean canBeCancelled() {
        return status == PrescriptionStatus.DRAFT || 
               status == PrescriptionStatus.ISSUED || 
               status == PrescriptionStatus.ON_HOLD;
    }

    public boolean isTerminalState() {
        return getEffectiveStatus().isTerminal();
    }

    public boolean isExpired(LocalDate evaluationDate) {
        return status != PrescriptionStatus.DRAFT && status != PrescriptionStatus.CANCELLED
                && issuedAt != null && evaluationDate != null
                && evaluationDate.isAfter(issuedAt.toLocalDate().plusMonths(1));
    }

    public PrescriptionStatus getEffectiveStatus() {
        return isExpired(LocalDate.now()) ? PrescriptionStatus.EXPIRED : status;
    }

    public boolean canTransitionTo(PrescriptionStatus target) {
        return target != null && status != null && status.allowedTransitions().contains(target);
    }

    public boolean isEligibleForDispensing() {
        return status == PrescriptionStatus.ISSUED && !isTerminalState();
    }

    public void changeStatus(PrescriptionStatus target, long changedBy, String reason) {
        if (target == null) {
            throw new IllegalArgumentException("Target status is required");
        }
        if (!status.allowedTransitions().contains(target)) {
            throw new IllegalStateException("Invalid prescription status transition");
        }
        if ((target == PrescriptionStatus.ON_HOLD || target == PrescriptionStatus.CANCELLED)
                && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A reason is required");
        }
        this.status = target;
        this.statusChangedAt = LocalDateTime.now();
        this.statusChangedBy = changedBy;
        this.statusChangeReason = reason;
        if (target == PrescriptionStatus.ISSUED && issuedAt == null) issuedAt = statusChangedAt;
        if (target == PrescriptionStatus.CANCELLED) {
            cancelledAt = statusChangedAt;
            cancellationReason = reason;
        }
        updatedAt = statusChangedAt;
        version++;
    }

    public void issue(long changedBy) {
        changeStatus(PrescriptionStatus.ISSUED, changedBy, null);
    }

    public void placeOnHold(long changedBy, String reason) {
        changeStatus(PrescriptionStatus.ON_HOLD, changedBy, reason);
    }

    public void resume(long changedBy) {
        changeStatus(PrescriptionStatus.ISSUED, changedBy, null);
    }

    public void cancel(long cancelledBy, String reason) {
        changeStatus(PrescriptionStatus.CANCELLED, cancelledBy, reason);
    }

    /**
     * Cancels the prescription.
     * Records the cancellation reason and timestamp.
     * Transitions status to CANCELLED.
     * 
     * @param reason the reason for cancellation
     * @param cancelledBy the user ID of who cancelled it
     * @throws IllegalStateException if the prescription cannot be cancelled
     */
    public void cancel(String reason, long cancelledBy) {
        cancel(cancelledBy, reason);
    }

    /** Applies editable aggregate fields while preserving lifecycle and identifier boundaries. */
    public void applyChanges(Map<String, Object> changes) {
        if (changes == null || changes.isEmpty()) return;
        if (!canBeEdited()) throw new IllegalStateException("Terminal prescriptions cannot be edited");
        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            switch (entry.getKey()) {
                case "clinicalNotes" -> {
                    if (!(entry.getValue() instanceof String)) {
                        throw new IllegalArgumentException("clinicalNotes must be text");
                    }
                    clinicalNotes = (String) entry.getValue();
                }
                case "items" -> {
                    if (!(entry.getValue() instanceof List<?> values)) {
                        throw new IllegalArgumentException("items must be a list");
                    }
                    if (values.isEmpty() || values.size() > 50
                            || values.stream().anyMatch(value -> !(value instanceof PrescriptionItem))) {
                        throw new IllegalArgumentException("Prescription must contain 1 to 50 items");
                    }
                    @SuppressWarnings("unchecked")
                    List<PrescriptionItem> replacements = (List<PrescriptionItem>) values;
                    items = new ArrayList<>(replacements);
                }
                default -> throw new IllegalArgumentException("Unknown prescription field: " + entry.getKey());
            }
        }
        updatedAt = LocalDateTime.now();
        version++;
    }

    @Override
    public String toString() {
        return "Prescription{" +
                "prescriptionId=" + prescriptionId +
                ", patientId=" + patientId +
                ", doctorId=" + doctorId +
                ", status=" + status +
                ", itemCount=" + items.size() +
                ", version=" + version +
                '}';
    }
}
