package pharmacy_system.model.pharmacy_operations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The pharmacy fulfilment transaction record for an eligible Prescription [UCD-08].
 *
 * <p>Maintains required and dispensed quantities per medicine, tracks fulfilment
 * status (PENDING/VERIFIED/DISPENSED/FAILED), and provides the state machine
 * methods to verify the patient, confirm dispensing quantities, and complete
 * fulfilment. Each record carries an optimistic-concurrency {@code version}
 * and may record a failure reason if the fulfilment cannot proceed.</p>
 *
 * <p>Requirement 8.1, 8.4, 8.6; Design document: 5.3.</p>
 */
public class DispenseRecord {

    /** Fulfilment lifecycle values, per the design document: PENDING/VERIFIED/DISPENSED/FAILED. */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_DISPENSED = "DISPENSED";
    public static final String STATUS_FAILED = "FAILED";

    private final long dispenseId;
    private final long prescriptionId;
    private final long patientId;
    private long pharmacistId;
    private final Map<Long, Integer> requiredQuantities;    // medicineId -> quantity
    private final Map<Long, Integer> dispensedQuantities;   // medicineId -> quantity actually dispensed
    private String status;
    private LocalDateTime verifiedAt;
    private LocalDateTime dispensedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String failureReason;
    private long version;

    /**
     * Creates a new dispense record in PENDING state with the given prescription
     * and patient identifiers and the required medicine quantities.
     *
     * @param dispenseId        unique identifier for this fulfilment record
     * @param prescriptionId    the prescription being fulfilled
     * @param patientId         the patient to whom the prescription belongs
     * @param requiredQuantities mapping from medicineId to required quantity (must not be null, may be empty)
     * @param createdAt         when the record was first created
     */
    public DispenseRecord(
            long dispenseId,
            long prescriptionId,
            long patientId,
            Map<Long, Integer> requiredQuantities,
            LocalDateTime createdAt) {
        this.dispenseId = dispenseId;
        this.prescriptionId = prescriptionId;
        this.patientId = patientId;
        this.pharmacistId = 0L;
        this.requiredQuantities = new HashMap<>(Objects.requireNonNull(requiredQuantities, "requiredQuantities must not be null"));
        this.dispensedQuantities = new HashMap<>();
        this.status = STATUS_PENDING;
        this.verifiedAt = null;
        this.dispensedAt = null;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = createdAt;
        this.failureReason = null;
        this.version = 0L;
    }

    /**
     * Verifies that the patient on the prescription matches the supplied patient identifier
     * (Requirement 8.6). If the patient matches, the record transitions to VERIFIED;
     * otherwise it remains in PENDING and records a failure reason.
     *
     * @param patientId the patient identifier to verify against the record
     * @return true if the patient matches and the record is now VERIFIED, false otherwise
     */
    public boolean verifyPatient(long patientId) {
        if (this.patientId == patientId) {
            this.status = STATUS_VERIFIED;
            this.verifiedAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
            this.version++;
            return true;
        } else {
            this.status = STATUS_FAILED;
            this.failureReason = "Patient verification mismatch";
            this.updatedAt = LocalDateTime.now();
            this.version++;
            return false;
        }
    }

    /**
     * Confirms the quantities to be dispensed (Requirement 8.1). This method should be
     * called after patient verification and before stock deduction. It validates that
     * all confirmed quantities match the required quantities exactly (no partial
     * dispensing, per Requirement 8.2) and that the record is in VERIFIED state.
     *
     * @param quantities mapping from medicineId to quantity to dispense (must not be null)
     * @throws IllegalStateException if the record is not in VERIFIED state
     * @throws IllegalArgumentException if any confirmed quantity does not match the required quantity
     */
    public void confirmDispensing(Map<Long, Integer> quantities) {
        if (!STATUS_VERIFIED.equals(status)) {
            throw new IllegalStateException("Cannot confirm dispensing: record must be in VERIFIED state");
        }

        Objects.requireNonNull(quantities, "quantities must not be null");

        // Validate all required medicines are present and quantities match exactly
        for (Map.Entry<Long, Integer> requiredEntry : requiredQuantities.entrySet()) {
            Long medicineId = requiredEntry.getKey();
            Integer requiredQty = requiredEntry.getValue();
            Integer confirmedQty = quantities.get(medicineId);

            if (confirmedQty == null) {
                throw new IllegalArgumentException("Missing confirmed quantity for medicine " + medicineId);
            }
            if (!confirmedQty.equals(requiredQty)) {
                throw new IllegalArgumentException(
                    String.format("Confirmed quantity %d for medicine %d does not match required quantity %d",
                        confirmedQty, medicineId, requiredQty));
            }
        }

        // Validate no extra medicines beyond those required
        if (quantities.size() != requiredQuantities.size()) {
            throw new IllegalArgumentException(
                String.format("Confirmed medicines count %d does not match required medicines count %d",
                    quantities.size(), requiredQuantities.size()));
        }

        this.dispensedQuantities.clear();
        this.dispensedQuantities.putAll(quantities);
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    /**
     * Completes the fulfilment process, marking the record as DISPENSED and recording
     * the pharmacist who performed the dispensing (Requirement 8.1, 8.4).
     *
     * @param pharmacistId the identifier of the pharmacist who completed the dispensing
     * @throws IllegalStateException if the record is not in VERIFIED state or if
     *         dispensing quantities have not been confirmed
     */
    public void completeFulfilment(long pharmacistId) {
        if (!STATUS_VERIFIED.equals(status)) {
            throw new IllegalStateException("Cannot complete fulfilment: record must be in VERIFIED state");
        }
        if (dispensedQuantities.isEmpty()) {
            throw new IllegalStateException("Cannot complete fulfilment: dispensing quantities must be confirmed first");
        }

        this.pharmacistId = pharmacistId;
        this.status = STATUS_DISPENSED;
        this.dispensedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    /**
     * Marks the fulfilment as FAILED with a reason (e.g., insufficient stock,
     * verification failure, concurrent update conflict).
     *
     * @param reason description of why the fulfilment failed (must not be null or blank)
     */
    public void markFailed(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Failure reason must not be null or blank");
        }
        this.status = STATUS_FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    /**
     * Returns whether this fulfilment has been completed (status = DISPENSED).
     *
     * @return true if the record is in DISPENSED state, false otherwise
     */
    public boolean isCompleted() {
        return STATUS_DISPENSED.equals(status);
    }

    // Accessors
    public long getDispenseId() {
        return dispenseId;
    }

    public long getDispenseRecordId() { return dispenseId; }

    public long getPrescriptionId() {
        return prescriptionId;
    }

    public long getPatientId() {
        return patientId;
    }

    public long getPharmacistId() {
        return pharmacistId;
    }

    /**
     * Returns an unmodifiable view of the required quantities mapping.
     */
    public Map<Long, Integer> getRequiredQuantities() {
        return Collections.unmodifiableMap(requiredQuantities);
    }

    /**
     * Returns an unmodifiable view of the confirmed/dispensed quantities mapping.
     */
    public Map<Long, Integer> getDispensedQuantities() {
        return Collections.unmodifiableMap(dispensedQuantities);
    }

    /** The fulfilment status: PENDING, VERIFIED, DISPENSED, or FAILED. */
    public String getStatus() {
        return status;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public LocalDateTime getDispensedAt() {
        return dispensedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public long getVersion() {
        return version;
    }

    /** Creates an independent persistence snapshot with an assigned identity and version. */
    public DispenseRecord persistedCopy(long persistedId, long persistedVersion) {
        DispenseRecord copy = new DispenseRecord(persistedId, prescriptionId, patientId,
                requiredQuantities, createdAt);
        copy.pharmacistId = pharmacistId;
        copy.dispensedQuantities.putAll(dispensedQuantities);
        copy.status = status;
        copy.verifiedAt = verifiedAt;
        copy.dispensedAt = dispensedAt;
        copy.updatedAt = updatedAt;
        copy.failureReason = failureReason;
        copy.version = persistedVersion;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DispenseRecord that = (DispenseRecord) o;
        return dispenseId == that.dispenseId &&
               prescriptionId == that.prescriptionId &&
               patientId == that.patientId &&
               pharmacistId == that.pharmacistId &&
               version == that.version &&
               Objects.equals(requiredQuantities, that.requiredQuantities) &&
               Objects.equals(dispensedQuantities, that.dispensedQuantities) &&
               Objects.equals(status, that.status) &&
               Objects.equals(verifiedAt, that.verifiedAt) &&
               Objects.equals(dispensedAt, that.dispensedAt) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(updatedAt, that.updatedAt) &&
               Objects.equals(failureReason, that.failureReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dispenseId, prescriptionId, patientId, pharmacistId, requiredQuantities,
                            dispensedQuantities, status, verifiedAt, dispensedAt, createdAt,
                            updatedAt, failureReason, version);
    }

    @Override
    public String toString() {
        return "DispenseRecord{" +
               "dispenseId=" + dispenseId +
               ", prescriptionId=" + prescriptionId +
               ", patientId=" + patientId +
               ", pharmacistId=" + pharmacistId +
               ", status='" + status + '\'' +
               ", version=" + version +
               '}';
    }
}
