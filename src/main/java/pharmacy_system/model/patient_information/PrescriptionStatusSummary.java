package pharmacy_system.model.patient_information;

import java.time.LocalDateTime;
import java.time.LocalDate;
import pharmacy_system.model.clinical_prescription.Prescription;
import pharmacy_system.model.clinical_prescription.PrescriptionStatus;
import pharmacy_system.model.pharmacy_operations.DispenseRecord;

/**
 * Read-only model representing patient-facing view of prescription and fulfilment status.
 * Combines clinical status (from Prescription) with pharmacy fulfilment status (from DispenseRecord).
 * 
 * This is a derived model, not persisted directly; built from Prescription + optional DispenseRecord.
 * Never mutates either source. Used exclusively by ViewPrescriptionStatusController (UCD-02).
 * 
 * [UCD-02]
 */
public class PrescriptionStatusSummary {
    public static final String FULFILMENT_NOT_STARTED = "NOT_STARTED";
    public static final String CLINICAL_STATUS_EXPIRED = "EXPIRED";
    private long prescriptionId;
    private long patientId;
    private long doctorId;
    private LocalDateTime prescribedAt;
    private LocalDateTime lastUpdatedAt;
    private String clinicalStatus;     // distinct from fulfilment (FR-026)
    private String fulfilmentStatus;   // e.g. PENDING, DISPENSED, FAILED
    private boolean hasFulfilmentRecord;

    // Constructor
    public PrescriptionStatusSummary(long prescriptionId, long patientId, long doctorId,
                                    LocalDateTime prescribedAt, LocalDateTime lastUpdatedAt,
                                    String clinicalStatus, String fulfilmentStatus,
                                    boolean hasFulfilmentRecord) {
        this.prescriptionId = prescriptionId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.prescribedAt = prescribedAt;
        this.lastUpdatedAt = lastUpdatedAt;
        this.clinicalStatus = clinicalStatus;
        this.fulfilmentStatus = fulfilmentStatus;
        this.hasFulfilmentRecord = hasFulfilmentRecord;
    }

    // Getters (read-only, no setters)
    public long getPrescriptionId() {
        return prescriptionId;
    }

    public long getPatientId() {
        return patientId;
    }

    public long getDoctorId() {
        return doctorId;
    }

    public LocalDateTime getPrescribedAt() {
        return prescribedAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public String getClinicalStatus() {
        return clinicalStatus;
    }

    public String getFulfilmentStatus() {
        return fulfilmentStatus;
    }

    public boolean hasFulfilmentRecord() {
        return hasFulfilmentRecord;
    }

    public static PrescriptionStatusSummary from(Prescription prescription,
                                                  DispenseRecord dispenseRecord,
                                                  LocalDate today) {
        return dispenseRecord == null
                ? buildFromPrescription(prescription, today)
                : combineClinicalAndFulfilmentStatus(prescription, dispenseRecord, today);
    }

    public static PrescriptionStatusSummary buildFromPrescription(Prescription prescription,
                                                                   LocalDate today) {
        if (prescription == null) throw new IllegalArgumentException("prescription must not be null");
        return build(prescription, null, today);
    }

    public static PrescriptionStatusSummary combineClinicalAndFulfilmentStatus(
            Prescription prescription, DispenseRecord dispenseRecord, LocalDate today) {
        if (prescription == null) throw new IllegalArgumentException("prescription must not be null");
        if (dispenseRecord == null) throw new IllegalArgumentException("dispenseRecord must not be null");
        return build(prescription, dispenseRecord, today);
    }

    private static PrescriptionStatusSummary build(Prescription prescription,
                                                   DispenseRecord dispenseRecord,
                                                   LocalDate today) {
        String clinical = prescription.isExpired(today)
                ? CLINICAL_STATUS_EXPIRED : prescription.getStatus().name();
        LocalDateTime updated = prescription.getUpdatedAt();
        if (dispenseRecord != null && dispenseRecord.getUpdatedAt() != null
                && (updated == null || dispenseRecord.getUpdatedAt().isAfter(updated))) {
            updated = dispenseRecord.getUpdatedAt();
        }
        return new PrescriptionStatusSummary(
                prescription.getPrescriptionId(), prescription.getPatientId(), prescription.getDoctorId(),
                prescription.getIssuedAt() == null ? prescription.getCreatedAt() : prescription.getIssuedAt(),
                updated, clinical,
                dispenseRecord == null ? FULFILMENT_NOT_STARTED : dispenseRecord.getStatus(),
                dispenseRecord != null);
    }

    public boolean isCancelled() { return PrescriptionStatus.CANCELLED.name().equals(clinicalStatus); }
    public boolean isExpired() { return CLINICAL_STATUS_EXPIRED.equals(clinicalStatus); }
    public boolean isDispensed() { return "DISPENSED".equals(fulfilmentStatus); }

    /**
     * Get patient-friendly status message combining clinical and fulfilment states.
     * @return descriptive status string
     */
    public String getStatusMessage() {
        StringBuilder msg = new StringBuilder();
        msg.append("Clinical Status: ").append(clinicalStatus);

        if (hasFulfilmentRecord) {
            msg.append(" | Fulfilment: ").append(fulfilmentStatus);
        } else {
            msg.append(" | Fulfilment: Not yet started");
        }

        return msg.toString();
    }

    @Override
    public String toString() {
        return "PrescriptionStatusSummary{" +
                "prescriptionId=" + prescriptionId +
                ", patientId=" + patientId +
                ", clinicalStatus='" + clinicalStatus + '\'' +
                ", fulfilmentStatus='" + fulfilmentStatus + '\'' +
                ", hasFulfilmentRecord=" + hasFulfilmentRecord +
                '}';
    }
}
