package pharmacy_system.view.pharmacy_operations.ucd10_manage_medicine_inventory;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Component View for stock adjustment input.
 * Collects stock receipt, adjustment, and reason information from the pharmacist.
 * 
 * [UCD-10]
 */
public class StockAdjustmentView {
    private Scanner scanner;

    public StockAdjustmentView() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Prompt the pharmacist to receive stock.
     * @return a map with quantity, expiryDate, and optional batch info
     */
    public Map<String, Object> getStockReceiptInput() {
        Map<String, Object> receiptData = new HashMap<>();

        System.out.println("=== Receive Stock ===");
        
        System.out.print("Quantity to receive: ");
        try {
            long quantity = Long.parseLong(scanner.nextLine().trim());
            if (quantity <= 0) {
                System.out.println("Quantity must be positive.");
                return new HashMap<>();
            }
            receiptData.put("quantity", quantity);
        } catch (NumberFormatException e) {
            System.out.println("Invalid quantity format.");
            return new HashMap<>();
        }

        System.out.print("Expiry Date (YYYY-MM-DD): ");
        try {
            LocalDate expiryDate = LocalDate.parse(scanner.nextLine().trim());
            receiptData.put("expiryDate", expiryDate);
        } catch (Exception e) {
            System.out.println("Invalid date format. Use YYYY-MM-DD.");
            return new HashMap<>();
        }

        System.out.print("Batch Number (optional): ");
        String batchNumber = scanner.nextLine().trim();
        if (!batchNumber.isEmpty()) {
            receiptData.put("batchNumber", batchNumber);
        }

        return receiptData;
    }

    /**
     * Prompt the pharmacist to adjust stock manually.
     * @return a map with delta and reason
     */
    public Map<String, Object> getStockAdjustmentInput() {
        Map<String, Object> adjustmentData = new HashMap<>();

        System.out.println("=== Adjust Stock ====");
        
        System.out.print("Quantity delta (positive for increase, negative for decrease): ");
        try {
            long delta = Long.parseLong(scanner.nextLine().trim());
            adjustmentData.put("delta", delta);
        } catch (NumberFormatException e) {
            System.out.println("Invalid quantity format.");
            return new HashMap<>();
        }

        System.out.print("Reason for adjustment (required): ");
        String reason = scanner.nextLine().trim();
        if (reason.isEmpty()) {
            System.out.println("Reason is required for stock adjustment.");
            return new HashMap<>();
        }
        adjustmentData.put("reason", reason);

        return adjustmentData;
    }

    /**
     * Show success message for stock receipt.
     * @param medicineId the medicine ID
     * @param quantity the received quantity
     */
    public void showStockReceivedSuccess(long medicineId, long quantity) {
        System.out.println("Stock received successfully.");
        System.out.println("  Medicine ID: " + medicineId);
        System.out.println("  Quantity: " + quantity);
    }

    /**
     * Show success message for stock adjustment.
     * @param medicineId the medicine ID
     * @param delta the adjustment delta
     */
    public void showStockAdjustedSuccess(long medicineId, long delta) {
        System.out.println("Stock adjusted successfully.");
        System.out.println("  Medicine ID: " + medicineId);
        System.out.println("  Delta: " + delta);
    }

    /**
     * Show error message for stock adjustment failure.
     * @param message the error message
     */
    public void showStockAdjustmentError(String message) {
        System.out.println("Stock Adjustment Error: " + message);
    }

    /**
     * Show error for negative balance prevention.
     * @param attemptedDelta the attempted delta
     * @param currentBalance the current balance
     */
    public void showNegativeBalanceError(long attemptedDelta, long currentBalance) {
        System.out.println("Error: Stock adjustment would create negative balance.");
        System.out.println("  Attempted Delta: " + attemptedDelta);
        System.out.println("  Current Balance: " + currentBalance);
    }
}
