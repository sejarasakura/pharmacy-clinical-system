package pharmacy_system.view.pharmacy_operations.ucd10_manage_medicine_inventory;

import pharmacy_system.model.pharmacy_operations.Medicine;

import java.util.List;

/**
 * Component View for displaying the medicine list.
 * Shows active medicines and their basic information to the pharmacist.
 * 
 * [UCD-10]
 */
public class MedicineListView {
    
    /**
     * Display a list of medicines.
     * @param medicines the list of medicines to display
     */
    public void displayMedicineList(List<Medicine> medicines) {
        if (medicines == null || medicines.isEmpty()) {
            System.out.println("No medicines available.");
            return;
        }

        System.out.println("=== Medicine List ===");
        System.out.println(String.format("%-10s %-30s %-15s", "ID", "Name", "Active"));
        System.out.println("-".repeat(55));
        
        for (Medicine medicine : medicines) {
            System.out.println(String.format("%-10d %-30s %-15s",
                    medicine.getMedicineId(),
                    truncate(medicine.getMedicineName(), 30),
                    medicine.isActive() ? "Yes" : "No"));
        }
        System.out.println("===================");
    }

    /**
     * Display details of a single medicine.
     * @param medicine the medicine to display
     */
    public void displayMedicineDetails(Medicine medicine) {
        System.out.println("=== Medicine Details ===");
        System.out.println("ID: " + medicine.getMedicineId());
        System.out.println("Name: " + medicine.getMedicineName());
        System.out.println("Active: " + (medicine.isActive() ? "Yes" : "No"));
        System.out.println("Created At: " + medicine.getCreatedAt());
        System.out.println("Updated At: " + medicine.getUpdatedAt());
        System.out.println("========================");
    }

    /**
     * Display an error message for medicine not found.
     * @param medicineId the ID not found
     */
    public void displayMedicineNotFound(long medicineId) {
        System.out.println("Error: Medicine with ID " + medicineId + " not found.");
    }

    /**
     * Truncate a string to a maximum length.
     * @param str the string to truncate
     * @param maxLength the maximum length
     * @return the truncated string
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
