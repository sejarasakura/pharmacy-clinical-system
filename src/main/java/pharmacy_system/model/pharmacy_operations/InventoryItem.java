package pharmacy_system.model.pharmacy_operations;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents one stock batch position for a medicine.
 * Contains quantity on hand, batch number, and expiry date.
 * Expiry is evaluated dynamically; expired stock contributes zero to available quantity.
 * 
 * Carries version field for optimistic concurrency control during stock deduction.
 * 
 * [UCD-08, UCD-10]
 */
public class InventoryItem {
    private long inventoryId;
    private long medicineId;
    private String batchNumber;
    private LocalDate expiryDate;
    private int quantityOnHand;
    private int reorderLevel;
    private long version;  // optimistic locking

    // Constructor
    public InventoryItem(long inventoryId, long medicineId, String batchNumber,
                        LocalDate expiryDate, int quantityOnHand, int reorderLevel) {
        this.inventoryId = inventoryId;
        this.medicineId = medicineId;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.quantityOnHand = quantityOnHand;
        this.reorderLevel = reorderLevel;
        this.version = 0L;
    }

    public InventoryItem(long inventoryId, long medicineId, String batchNumber,
                         LocalDate expiryDate, int quantityOnHand) {
        this(inventoryId, medicineId, batchNumber, expiryDate, quantityOnHand, 0, 0L);
    }

    public InventoryItem(long inventoryId, long medicineId, String batchNumber,
                         LocalDate expiryDate, int quantityOnHand, int reorderLevel,
                         long version) {
        this.inventoryId = inventoryId;
        this.medicineId = medicineId;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.quantityOnHand = quantityOnHand;
        this.reorderLevel = reorderLevel;
        this.version = version;
    }

    /** Compatibility constructor for persisted batches that carry audit timestamps externally. */
    public InventoryItem(long inventoryId, long medicineId, String batchNumber,
                         LocalDate expiryDate, int quantityOnHand, int reorderLevel,
                         long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(inventoryId, medicineId, batchNumber, expiryDate, quantityOnHand, reorderLevel, version);
    }

    // Getters
    public long getInventoryId() {
        return inventoryId;
    }

    public long getMedicineId() {
        return medicineId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public long getVersion() {
        return version;
    }

    // Business logic
    /**
     * Get available quantity for dispensing.
     * Returns 0 if expired, otherwise returns quantityOnHand.
     * Implements FR-046 (exclude expired from dispensable).
     * 
     * @return available quantity (0 if expired)
     */
    public int getAvailableQuantity() {
        if (isExpired()) {
            return 0;
        }
        return quantityOnHand;
    }

    /**
     * Check if batch is expired.
     * Implements FR-046.
     * 
     * @return true if expiryDate is earlier than today, false otherwise
     */
    public boolean isExpired() {
        if (expiryDate == null) {
            return false;
        }
        return expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Check if stock is below reorder level.
     * @return true if quantityOnHand <= reorderLevel, false otherwise
     */
    public boolean isLowStock() {
        return quantityOnHand <= reorderLevel;
    }

    /**
     * Check if specified quantity can be deducted without going negative.
     * Implements FR-048 (no negative balance).
     * 
     * @param quantity the quantity to check
     * @return true if quantityOnHand - quantity >= 0, false otherwise
     */
    public boolean canDeduct(int quantity) {
        return quantity > 0 && (quantityOnHand - quantity) >= 0;
    }

    /**
     * Deduct quantity from stock.
     * Does not validate availability; caller must verify using canDeduct() first.
     * 
     * @param quantity the quantity to deduct
     * @throws IllegalArgumentException if quantity is invalid or would result in negative balance
     */
    public void deduct(int quantity) {
        if (!canDeduct(quantity)) {
            throw new IllegalStateException("Insufficient stock; deduction must be positive and cannot take balance below zero");
        }
        this.quantityOnHand -= quantity;
        this.version++;
    }

    /**
     * Receive stock (add to quantity).
     * @param quantity the quantity to add
     */
    public void receive(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than 0");
        this.quantityOnHand += quantity;
        this.version++;
    }

    /**
     * Adjust stock by delta (can be positive or negative).
     * Validates that result does not go negative.
     * 
     * @param quantityDelta the change in quantity
     * @throws IllegalArgumentException if adjustment would result in negative balance
     */
    public void adjust(int quantityDelta) {
        if ((this.quantityOnHand + quantityDelta) < 0) {
            throw new IllegalStateException("Adjustment would take balance below zero");
        }
        this.quantityOnHand += quantityDelta;
        this.version++;
    }

    public boolean canAdjust(int quantityDelta) {
        return quantityOnHand + quantityDelta >= 0;
    }

    public java.util.List<String> validateStockData(int quantity, LocalDate expiry) {
        java.util.List<String> errors = new java.util.ArrayList<>();
        if (quantity <= 0) errors.add("Quantity must be greater than 0");
        if (expiry == null) errors.add("Expiry date is required");
        if (batchNumber == null || batchNumber.isBlank()) errors.add("Batch number is required");
        return errors;
    }

    /**
     * Increment version for optimistic concurrency control.
     */
    public void incrementVersion() {
        this.version++;
    }

    @Override
    public String toString() {
        return "InventoryItem{" +
                "inventoryId=" + inventoryId +
                ", medicineId=" + medicineId +
                ", batchNumber='" + batchNumber + '\'' +
                ", expiryDate=" + expiryDate +
                ", quantityOnHand=" + quantityOnHand +
                ", isExpired=" + isExpired() +
                ", isLowStock=" + isLowStock() +
                ", version=" + version +
                '}';
    }
}
