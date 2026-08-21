package pharmacy_system.view.pharmacy_operations.ucd10_manage_medicine_inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Component View for medicine form input.
 * Collects medicine information from the pharmacist.
 * 
 * [UCD-10]
 */
public class MedicineFormView {
    private Scanner scanner;

    public MedicineFormView() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Prompt the pharmacist to enter medicine information.
     * @return a map of medicine fields or empty if cancelled
     */
    public Map<String, Object> getMedicineInput() {
        Map<String, Object> medicineData = new HashMap<>();
        
        System.out.println("=== Create/Update Medicine ===");
        
        System.out.print("Medicine Name (max 255 chars): ");
        String medicineName = scanner.nextLine().trim();
        if (medicineName.isEmpty()) {
            System.out.println("Medicine name is required.");
            return new HashMap<>();
        }
        medicineData.put("medicineName", medicineName);

        System.out.print("Active? (yes/no): ");
        String activeInput = scanner.nextLine().trim().toLowerCase();
        medicineData.put("active", "yes".equals(activeInput) || "y".equals(activeInput));

        return medicineData;
    }

    /**
     * Show success message for medicine creation.
     * @param medicineId the created medicine ID
     * @param medicineName the created medicine name
     */
    public void showMedicineCreatedSuccess(long medicineId, String medicineName) {
        System.out.println("Medicine created successfully.");
        System.out.println("  ID: " + medicineId);
        System.out.println("  Name: " + medicineName);
    }

    /**
     * Show success message for medicine update.
     * @param medicineId the updated medicine ID
     */
    public void showMedicineUpdatedSuccess(long medicineId) {
        System.out.println("Medicine updated successfully. ID: " + medicineId);
    }

    /**
     * Show error message for validation failure.
     * @param message the error message
     */
    public void showValidationError(String message) {
        System.out.println("Validation Error: " + message);
    }
}
