package pharmacy_system.view.pharmacy_operations.ucd08_dispense_medication;

import pharmacy_system.model.pharmacy_operations.DispenseRecord;

/**
 * Component View for dispensing result display.
 * Shows the outcome of a dispensing transaction to the pharmacist.
 * 
 * [UCD-08]
 */
public class DispenseResultView {
    
    /**
     * Display the result of a completed dispensing transaction.
     * @param record the dispense record
     */
    public void displayDispenseResult(DispenseRecord record) {
        System.out.println("=== Dispensing Result ===");
        System.out.println("Dispense Record ID: " + record.getDispenseRecordId());
        System.out.println("Prescription ID: " + record.getPrescriptionId());
        System.out.println("Patient ID: " + record.getPatientId());
        System.out.println("Status: " + record.getStatus());
        System.out.println("Pharmacist ID: " + record.getPharmacistId());
        System.out.println("Dispensed At: " + record.getDispensedAt());
        System.out.println("========================");
    }

    /**
     * Display a verification failure message.
     * @param message the failure message
     */
    public void displayVerificationFailure(String message) {
        System.out.println("Verification Failed: " + message);
    }

    /**
     * Display a duplicate dispensing error.
     * @param existingRecordId the existing dispense record ID
     */
    public void displayDuplicateDispenseError(long existingRecordId) {
        System.out.println("Error - Medication Already Dispensed");
        System.out.println("Existing Dispense Record ID: " + existingRecordId);
    }
}
