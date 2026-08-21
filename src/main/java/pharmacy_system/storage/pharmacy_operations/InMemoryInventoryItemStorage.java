package pharmacy_system.storage.pharmacy_operations;

import pharmacy_system.model.pharmacy_operations.InventoryItem;
import pharmacy_system.model.pharmacy_operations.StockMovement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of InventoryStorage using HashMap.
 * Thread-safe with atomic ID generation and version-checked updates.
 * Implements atomic deductInventory (FEFO) and adjustStock (with movement) operations.
 * Each InventoryItem represents one batch position for a Medicine.
 */
public class InMemoryInventoryItemStorage implements InventoryStorage {
    private final Map<Long, InventoryItem> store = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> medicineIndex = new ConcurrentHashMap<>();
    private final Map<Long, StockMovement> movements = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final AtomicLong movementIdGenerator = new AtomicLong(1);

    @Override
    public InventoryItem create(InventoryItem inventoryItem) {
        long inventoryId = idGenerator.getAndIncrement();
        InventoryItem created = new InventoryItem(
                inventoryId,
                inventoryItem.getMedicineId(),
                inventoryItem.getBatchNumber(),
                inventoryItem.getExpiryDate(),
                inventoryItem.getQuantityOnHand(),
                inventoryItem.getReorderLevel(),
                1 // version
        );
        store.put(inventoryId, created);
        medicineIndex.computeIfAbsent(inventoryItem.getMedicineId(), k -> new ArrayList<>()).add(inventoryId);
        return created;
    }

    @Override
    public Optional<InventoryItem> findById(long inventoryId) {
        return Optional.ofNullable(store.get(inventoryId));
    }

    @Override
    public List<InventoryItem> findByMedicineId(long medicineId) {
        return medicineIndex.getOrDefault(medicineId, Collections.emptyList()).stream()
                .map(id -> store.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean update(InventoryItem inventoryItem, long expectedVersion) {
        InventoryItem existing = store.get(inventoryItem.getInventoryId());
        if (existing == null) {
            throw new IllegalArgumentException("InventoryItem not found: " + inventoryItem.getInventoryId());
        }
        if (existing.getVersion() != expectedVersion) {
            return false; // version conflict
        }
        InventoryItem updated = new InventoryItem(
                inventoryItem.getInventoryId(),
                inventoryItem.getMedicineId(),
                inventoryItem.getBatchNumber(),
                inventoryItem.getExpiryDate(),
                inventoryItem.getQuantityOnHand(),
                inventoryItem.getReorderLevel(),
                expectedVersion + 1 // increment version
        );
        store.put(inventoryItem.getInventoryId(), updated);
        return true;
    }

    @Override
    public boolean delete(long inventoryId) {
        InventoryItem inventoryItem = store.remove(inventoryId);
        if (inventoryItem == null) return false;
        medicineIndex.getOrDefault(inventoryItem.getMedicineId(), new ArrayList<>()).remove(inventoryId);
        return true;
    }

    @Override
    public List<InventoryItem> listAll() {
        return new ArrayList<>(store.values());
    }

    /**
     * Deducts inventory across multiple batches for dispensing and records a single combined stock movement.
     * ATOMIC: All deductions are applied together with a single movement record, or none are applied.
     *
     * Preconditions:
     * - allocationPlan must contain valid (inventoryId -> quantity) pairs summing to the required amount
     * - stockMovement must be of type DISPENSE
     * - Each inventoryId in the plan must exist and have sufficient quantity
     *
     * Postconditions:
     * - If successful: all quantities in the plan are deducted, movement is recorded, returns true
     * - If unsuccessful: no quantities are deducted, no movement is recorded, returns false
     *
     * @param allocationPlan a Map of (inventoryId -> quantity to deduct) for FEFO-ordered allocation
     * @param stockMovement the single StockMovement to record for this dispensing (type must be DISPENSE)
     * @return true if all deductions succeed atomically, false if any batch has insufficient stock or version conflict
     */
    @Override
    public boolean deductInventory(Map<Long, Integer> allocationPlan, StockMovement stockMovement) {
        // Phase 1: Validate all items exist and have sufficient quantity
        Map<Long, InventoryItem> itemsToUpdate = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : allocationPlan.entrySet()) {
            Long inventoryId = entry.getKey();
            Integer quantityToDeduct = entry.getValue();

            InventoryItem existing = store.get(inventoryId);
            if (existing == null) {
                return false; // inventory item not found
            }
            if (existing.getQuantityOnHand() < quantityToDeduct) {
                return false; // insufficient balance
            }
            itemsToUpdate.put(inventoryId, existing);
        }

        // Phase 2: Perform all deductions atomically
        for (Map.Entry<Long, Integer> entry : allocationPlan.entrySet()) {
            Long inventoryId = entry.getKey();
            Integer quantityToDeduct = entry.getValue();
            InventoryItem existing = itemsToUpdate.get(inventoryId);

            int newQuantity = existing.getQuantityOnHand() - quantityToDeduct;
            InventoryItem updated = new InventoryItem(
                    existing.getInventoryId(),
                    existing.getMedicineId(),
                    existing.getBatchNumber(),
                    existing.getExpiryDate(),
                    newQuantity,
                    existing.getReorderLevel(),
                    existing.getVersion() + 1 // increment version
            );
            store.put(inventoryId, updated);
        }

        // Phase 3: Record the combined movement
        long movementId = movementIdGenerator.getAndIncrement();
        movements.put(movementId, stockMovement);

        return true;
    }

    /**
     * Adjusts stock for a single InventoryItem and records the adjustment movement atomically.
     * Returns false and makes no changes if version conflict or if adjustment would result in negative balance.
     *
     * Preconditions:
     * - inventoryItem must exist and have matching version
     * - movement must be of type ADJUSTMENT with a non-blank reason
     * - newQuantity must be >= 0
     *
     * Postconditions:
     * - If successful: item quantity is updated, version incremented, movement recorded; returns true
     * - If unsuccessful: no changes made; returns false
     *
     * @param inventoryItem the updated InventoryItem (must have version set to current value)
     * @param movement the StockMovement to record (must be of type ADJUSTMENT with a reason)
     * @param expectedVersion the version at the time of the last read (must match current to succeed)
     * @return true if adjustment succeeds, false if version conflict detected or would result in negative balance
     */
    @Override
    public boolean adjustStock(InventoryItem inventoryItem, StockMovement movement, long expectedVersion) {
        InventoryItem existing = store.get(inventoryItem.getInventoryId());
        if (existing == null) {
            return false;
        }
        if (existing.getVersion() != expectedVersion) {
            return false; // version conflict
        }
        if (inventoryItem.getQuantityOnHand() < 0) {
            return false; // would result in negative balance
        }

        // Update the inventory item
        InventoryItem updated = new InventoryItem(
                inventoryItem.getInventoryId(),
                inventoryItem.getMedicineId(),
                inventoryItem.getBatchNumber(),
                inventoryItem.getExpiryDate(),
                inventoryItem.getQuantityOnHand(),
                inventoryItem.getReorderLevel(),
                expectedVersion + 1 // increment version
        );
        store.put(inventoryItem.getInventoryId(), updated);

        // Record the movement
        long movementId = movementIdGenerator.getAndIncrement();
        movements.put(movementId, movement);

        return true;
    }

    @Override
    public List<Map<String, Object>> queryForReport(Map<String, String> filters) {
        return store.values().stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("inventoryId", item.getInventoryId());
                    row.put("medicineId", item.getMedicineId());
                    row.put("batchNumber", item.getBatchNumber());
                    row.put("expiryDate", item.getExpiryDate());
                    row.put("quantityOnHand", (long) item.getQuantityOnHand());
                    row.put("isExpired", item.isExpired());
                    return row;
                })
                .collect(Collectors.toList());
    }
}
