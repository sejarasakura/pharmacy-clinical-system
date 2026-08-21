package pharmacy_system.storage.pharmacy_operations;

import pharmacy_system.model.pharmacy_operations.InventoryItem;
import pharmacy_system.model.pharmacy_operations.StockMovement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Storage contract for InventoryItem entities (batch tracking).
 * Supports CRUD operations, queries by medicine, and combined inventory+movement operations.
 * Each InventoryItem represents one batch position for a Medicine, including quantity and expiry.
 * Batch tracking and stock movements are recorded atomically together.
 * Implements optimistic concurrency control via version fields.
 */
public interface InventoryStorage {

    /**
     * Creates and persists a new InventoryItem (batch).
     * @param inventoryItem the InventoryItem to create (must have medicineId, batchNumber, expiryDate set)
     * @return the created InventoryItem with assigned inventoryId and version set to 1
     */
    InventoryItem create(InventoryItem inventoryItem);

    /**
     * Retrieves an InventoryItem by its unique ID.
     * @param inventoryId the inventory ID
     * @return Optional containing the InventoryItem if found, empty otherwise
     */
    Optional<InventoryItem> findById(long inventoryId);

    /**
     * Retrieves all InventoryItems (batches) for a specific Medicine.
     * @param medicineId the medicine ID
     * @return a list of all batches for this medicine
     */
    List<InventoryItem> findByMedicineId(long medicineId);

    /**
     * Updates an existing InventoryItem with optimistic concurrency control.
     * @param inventoryItem the updated InventoryItem (must have version set to the current value)
     * @param expectedVersion the version at the time of the last read (must match current to succeed)
     * @return true if update succeeds, false if version conflict detected
     * @throws IllegalArgumentException if the inventory item does not exist
     */
    boolean update(InventoryItem inventoryItem, long expectedVersion);

    /**
     * Deletes an InventoryItem by ID.
     * @param inventoryId the inventory ID to delete
     * @return true if deletion succeeds, false if the inventory item does not exist
     */
    boolean delete(long inventoryId);

    /**
     * Retrieves all InventoryItems.
     * @return a list of all InventoryItems in the system
     */
    List<InventoryItem> listAll();

    /**
     * Deducts inventory across multiple batches for dispensing and records a combined stock movement.
     * The allocation plan specifies which inventoryId receives which quantity deduction.
     * All deductions are applied atomically with a single StockMovement record.
     * @param allocationPlan a Map of (inventoryId -> quantity to deduct) for FEFO-ordered allocation
     * @param stockMovement the single StockMovement to record for this dispensing
     * @return true if deduction succeeds, false if any batch version conflict or insufficient balance detected
     */
    boolean deductInventory(Map<Long, Integer> allocationPlan, StockMovement stockMovement);

    /**
     * Adjusts stock for a single InventoryItem and records the adjustment movement.
     * @param inventoryItem the updated InventoryItem (must have version set to current value)
     * @param movement the StockMovement to record (must be of type ADJUSTMENT with a reason)
     * @param expectedVersion the version at the time of the last read (must match current to succeed)
     * @return true if adjustment succeeds, false if version conflict detected or would result in negative balance
     */
    boolean adjustStock(InventoryItem inventoryItem, StockMovement movement, long expectedVersion);

    /**
     * Queries InventoryItems for reporting purposes.
     * Used by the GenerateReportsController to retrieve inventory reports.
     * @param filters optional filter criteria (implementation-dependent)
     * @return a list of Maps representing InventoryItem rows matching the filters
     */
    List<Map<String, Object>> queryForReport(Map<String, String> filters);
}
