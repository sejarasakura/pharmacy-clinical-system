package pharmacy_system.view.pharmacy_operations.ucd08_dispense_medication;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Component View for dispensing form input.
 * Collects prescription selection and patient verification from the pharmacist.
 * 
 * [UCD-08]
 */
public class DispenseFormView {
    private Scanner scanner;

    public DispenseFormView() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Prompt the pharmacist to enter a prescription ID for dispensing.
     * @return the prescription ID entered
     */
    public long getPrescriptionIdInput() {
        System.out.print("Enter Prescription ID: ");
        try {
            return Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid prescription ID format.");
            return -1L;
        }
    }

    /**
     * Prompt the pharmacist to confirm patient details.
     * @param patientName the patient name to confirm
     * @return true if confirmed, false otherwise
     */
    public boolean confirmPatientDetails(String patientName) {
        System.out.println("Patient: " + patientName);
        System.out.print("Confirm patient details? (yes/no): ");
        String response = scanner.nextLine().trim().toLowerCase();
        return "yes".equals(response) || "y".equals(response);
    }

    /**
     * Show error message for insufficient stock.
     * @param message the error message
     */
    public void showInsufficientStockError(String message) {
        System.out.println("Error - Insufficient Stock: " + message);
    }

    /**
     * Show error message for failed dispensing.
     * @param message the error message
     */
    public void showDispensError(String message) {
        System.out.println("Error - Dispense Failed: " + message);
    }

    /**
     * Show success message after successful dispensing.
     * @param dispensRecordId the dispense record ID
     */
    public void showDispensSuccess(long dispensRecordId) {
        System.out.println("Medication dispensed successfully. Record ID: " + dispensRecordId);
    }
}
