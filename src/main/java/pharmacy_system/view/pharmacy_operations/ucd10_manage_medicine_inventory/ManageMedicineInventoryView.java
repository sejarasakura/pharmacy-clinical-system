package pharmacy_system.view.pharmacy_operations.ucd10_manage_medicine_inventory;

import pharmacy_system.controller.pharmacy_operations.ManageMedicineInventoryController;
import pharmacy_system.model.pharmacy_operations.InventoryItem;
import pharmacy_system.model.pharmacy_operations.Medicine;

import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * Primary View for UCD-10: Manage Medicine Inventory.
 * Orchestrates the pharmacist workflow for managing medicines and inventory,
 * integrating the medicine form, list, and stock adjustment component Views.
 * 
 * [UCD-10]
 */
public class ManageMedicineInventoryView {
    private ManageMedicineInventoryController controller;
    private MedicineListView medicineListView;
    private MedicineFormView medicineFormView;
    private StockAdjustmentView stockAdjustmentView;
    private Scanner scanner;

    public ManageMedicineInventoryView(ManageMedicineInventoryController controller,
                                        MedicineListView medicineListView,
                                        MedicineFormView medicineFormView,
                                        StockAdjustmentView stockAdjustmentView) {
        this.controller = controller;
        this.medicineListView = medicineListView;
        this.medicineFormView = medicineFormView;
        this.stockAdjustmentView = stockAdjustmentView;
        this.scanner = new Scanner(System.in);
    }

    /**
     * Show the medicine inventory management workflow.
     * Menu includes:
     * 1. Create medicine
     * 2. Update medicine
     * 3. View medicines
     * 4. Receive stock
     * 5. Adjust stock
     * 6. View inventory details
     */
    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== Manage Medicine Inventory ===");
            System.out.println("1. Create Medicine");
            System.out.println("2. Update Medicine");
            System.out.println("3. View All Medicines");
            System.out.println("4. Receive Stock");
            System.out.println("5. Adjust Stock");
            System.out.println("6. View Inventory Details");
            System.out.println("0. Exit");
            System.out.print("Select option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    handleCreateMedicine();
                    break;
                case "2":
                    handleUpdateMedicine();
                    break;
                case "3":
                    handleViewMedicines();
                    break;
                case "4":
                    handleReceiveStock();
                    break;
                case "5":
                    handleAdjustStock();
                    break;
                case "6":
                    handleViewInventory();
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void handleCreateMedicine() {
        Map<String, Object> medicineData = medicineFormView.getMedicineInput();
        if (medicineData.isEmpty()) {
            return;
        }

        String medicineName = (String) medicineData.get("medicineName");
        Optional<Medicine> medicineOpt = controller.createMedicine(
                (String) medicineData.get("medicineCode"), medicineName,
                (String) medicineData.get("genericName"), (String) medicineData.get("dosageForm"),
                (String) medicineData.get("strength"), (String) medicineData.get("unit"),
                (String) medicineData.get("description"));
        
        if (medicineOpt.isPresent()) {
            Medicine medicine = medicineOpt.get();
            medicineFormView.showMedicineCreatedSuccess(medicine.getMedicineId(), medicine.getMedicineName());
        } else {
            medicineFormView.showValidationError("Failed to create medicine. Check that the name is valid.");
        }
    }

    private void handleUpdateMedicine() {
        System.out.print("Enter Medicine ID to update: ");
        long medicineId;
        try {
            medicineId = Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid medicine ID format.");
            return;
        }

        Map<String, Object> medicineData = medicineFormView.getMedicineInput();
        if (medicineData.isEmpty()) {
            return;
        }

        // Attempt update with version 0 (simplified for now; in real app would fetch current version)
        boolean updated = controller.updateMedicine(medicineId, medicineData, 0L);
        
        if (updated) {
            medicineFormView.showMedicineUpdatedSuccess(medicineId);
        } else {
            medicineFormView.showValidationError("Failed to update medicine. Check the ID or version.");
        }
    }

    private void handleViewMedicines() {
        // In a real implementation, this would query the controller
        System.out.println("Medicines view would be displayed here.");
    }

    private void handleReceiveStock() {
        System.out.print("Enter Medicine ID: ");
        long medicineId;
        try {
            medicineId = Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid medicine ID format.");
            return;
        }

        Map<String, Object> receiptData = stockAdjustmentView.getStockReceiptInput();
        if (receiptData.isEmpty()) {
            return;
        }

        int quantity = Math.toIntExact((long) receiptData.get("quantity"));
        Object expiryDateObj = receiptData.get("expiryDate");
        
        Optional<InventoryItem> inventoryOpt = controller.receiveStock(
                medicineId,
                String.valueOf(receiptData.getOrDefault("batchNumber", "NEW")),
                expiryDateObj == null ? null : java.time.LocalDate.parse(expiryDateObj.toString()),
                quantity);
        
        if (inventoryOpt.isPresent()) {
            stockAdjustmentView.showStockReceivedSuccess(medicineId, quantity);
        } else {
            stockAdjustmentView.showStockAdjustmentError("Failed to receive stock. Check medicine ID and expiry date.");
        }
    }

    private void handleAdjustStock() {
        System.out.print("Enter Inventory Item ID: ");
        long inventoryItemId;
        try {
            inventoryItemId = Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid inventory item ID format.");
            return;
        }

        Map<String, Object> adjustmentData = stockAdjustmentView.getStockAdjustmentInput();
        if (adjustmentData.isEmpty()) {
            return;
        }

        int delta = Math.toIntExact((long) adjustmentData.get("delta"));
        String reason = (String) adjustmentData.get("reason");

        boolean adjusted = controller.adjustStock(inventoryItemId, delta, reason, 0L);
        
        if (adjusted) {
            stockAdjustmentView.showStockAdjustedSuccess(inventoryItemId, delta);
        } else {
            stockAdjustmentView.showStockAdjustmentError("Adjustment failed. This may result in a negative balance or invalid item.");
        }
    }

    private void handleViewInventory() {
        System.out.print("Enter Medicine ID: ");
        long medicineId;
        try {
            medicineId = Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid medicine ID format.");
            return;
        }

        // In a real implementation, would query controller for inventory items
        System.out.println("Inventory for medicine " + medicineId + " would be displayed here.");
    }
}
