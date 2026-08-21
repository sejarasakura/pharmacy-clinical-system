package pharmacy_system.view.pharmacy_operations.ucd08_dispense_medication;

import pharmacy_system.controller.pharmacy_operations.DispenseMedicationController;
import pharmacy_system.model.pharmacy_operations.DispenseRecord;

import java.util.Scanner;

/**
 * Primary View for UCD-08: Dispense Medication.
 * Orchestrates the pharmacist workflow for verifying prescriptions and dispensing medication,
 * integrating the dispense form and result component Views.
 * 
 * [UCD-08]
 */
public class DispenseMedicationView {
    private DispenseMedicationController controller;
    private DispenseFormView dispenseFormView;
    private DispenseResultView dispenseResultView;
    private Scanner scanner;

    public DispenseMedicationView(DispenseMedicationController controller,
                                  DispenseFormView dispenseFormView,
                                  DispenseResultView dispenseResultView) {
        this.controller = controller;
        this.dispenseFormView = dispenseFormView;
        this.dispenseResultView = dispenseResultView;
        this.scanner = new Scanner(System.in);
    }

    /**
     * Show the dispensing medication workflow.
     * Orchestrates:
     * 1. Request prescription and verify eligibility
     * 2. Confirm patient details
     * 3. Check stock availability
     * 4. Complete dispensing
     * 5. Display result
     */
    public void show() {
        System.out.println("=== Dispense Medication Workflow ===");

        // Step 1: Get prescription ID
        long prescriptionId = dispenseFormView.getPrescriptionIdInput();
        if (prescriptionId <= 0) {
            System.out.println("Invalid prescription ID provided.");
            return;
        }

        // Step 2: Request eligible prescription
        DispenseRecord dispenseRecord = controller.requestEligiblePrescription(prescriptionId);
        if (dispenseRecord == null) {
            dispenseResultView.displayVerificationFailure("Prescription not found or not eligible for dispensing.");
            return;
        }

        // Step 3: Get patient ID from prescription and verify
        // Note: In a real implementation, this would be extracted from the prescription object
        System.out.print("Enter Patient ID to verify: ");
        long patientId;
        try {
            patientId = Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid patient ID format.");
            return;
        }

        // Step 4: Verify prescription and patient
        boolean verified = controller.verifyPrescriptionAndPatient(prescriptionId, patientId);
        if (!verified) {
            dispenseResultView.displayVerificationFailure("Prescription and patient verification failed.");
            return;
        }

        // Step 5: Confirm patient details
        if (!dispenseFormView.confirmPatientDetails("Patient ID: " + patientId)) {
            System.out.println("Dispensing cancelled by pharmacist.");
            return;
        }

        // Step 6: Check stock availability
        boolean stockAvailable = controller.checkStockAvailability(dispenseRecord.getRequiredQuantities());
        if (!stockAvailable) {
            dispenseFormView.showInsufficientStockError("Insufficient non-expired stock available for this prescription.");
            return;
        }

        // Step 7: Confirm dispensing
        System.out.print("Confirm dispensing? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        if (!"yes".equals(confirmation) && !"y".equals(confirmation)) {
            System.out.println("Dispensing cancelled by pharmacist.");
            return;
        }

        // Step 8: Complete dispensing
        controller.confirmDispensing(dispenseRecord.getDispenseId(), dispenseRecord.getRequiredQuantities());
        dispenseRecord = controller.completeDispensing(dispenseRecord.getDispenseId());
        if (dispenseRecord == null || !dispenseRecord.isCompleted()) {
            dispenseFormView.showDispensError("Failed to complete dispensing. Please try again.");
            return;
        }

        // Step 9: Display success result
        dispenseFormView.showDispensSuccess(dispenseRecord.getDispenseRecordId());
        dispenseResultView.displayDispenseResult(dispenseRecord);
    }
}
