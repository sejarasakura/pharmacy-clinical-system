package pharmacy_system.storage.pharmacy_operations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pharmacy_system.model.pharmacy_operations.InventoryItem;
import pharmacy_system.model.pharmacy_operations.StockMovement;
import pharmacy_system.model.pharmacy_operations.StockMovementType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryInventoryItemStorageTest {
    private InventoryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryInventoryItemStorage();
    }

    @Test
    void testCreateAndFindById() {
        InventoryItem item = new InventoryItem(
                0, 1, "BATCH-001", LocalDate.of(2025, 12, 31), 100, 20, 0
        );

        InventoryItem created = storage.create(item);
        assertEquals(1, created.getInventoryId());
        assertEquals(1, created.getVersion());

        var found = storage.findById(created.getInventoryId());
        assertTrue(found.isPresent());
        assertEquals(100, found.get().getQuantityOnHand());
    }

    @Test
    void testFindByMedicineId() {
        InventoryItem item1 = new InventoryItem(
                0, 5, "BATCH-101", LocalDate.of(2025, 6, 30), 150, 20, 0
        );
        InventoryItem item2 = new InventoryItem(
                0, 5, "BATCH-102", LocalDate.of(2025, 8, 31), 200, 20, 0
        );

        storage.create(item1);
        storage.create(item2);

        List<InventoryItem> items = storage.findByMedicineId(5);
        assertEquals(2, items.size());
    }

    @Test
    void testUpdateWithVersionConflict() {
        InventoryItem item = new InventoryItem(
                0, 2, "BATCH-002", LocalDate.of(2025, 12, 31), 100, 20, 0
        );
        InventoryItem created = storage.create(item);

        InventoryItem modified = new InventoryItem(
                created.getInventoryId(), 2, "BATCH-002", LocalDate.of(2025, 12, 31), 80, 20, 0
        );

        boolean success = storage.update(modified, 0);
        assertFalse(success);
    }

    @Test
    void testUpdateWithCorrectVersion() {
        InventoryItem item = new InventoryItem(
                0, 3, "BATCH-003", LocalDate.of(2025, 12, 31), 100, 20, 0
        );
        InventoryItem created = storage.create(item);

        InventoryItem modified = new InventoryItem(
                created.getInventoryId(), 3, "BATCH-003", LocalDate.of(2025, 12, 31), 80, 20, 0
        );

        boolean success = storage.update(modified, 1);
        assertTrue(success);

        var updated = storage.findById(created.getInventoryId());
        assertEquals(80, updated.get().getQuantityOnHand());
        assertEquals(2, updated.get().getVersion());
    }

    @Test
    void testDelete() {
        InventoryItem item = new InventoryItem(
                0, 4, "BATCH-004", LocalDate.of(2025, 12, 31), 100, 20, 0
        );
        InventoryItem created = storage.create(item);

        boolean deleted = storage.delete(created.getInventoryId());
        assertTrue(deleted);

        var found = storage.findById(created.getInventoryId());
        assertFalse(found.isPresent());
    }

    @Test
    void testDeductInventorySuccess() {
        // Create three batches with expiry dates
        InventoryItem batch1 = storage.create(new InventoryItem(
                0, 10, "BATCH-A", LocalDate.of(2024, 6, 30), 50, 10, 0
        ));
        InventoryItem batch2 = storage.create(new InventoryItem(
                0, 10, "BATCH-B", LocalDate.of(2024, 8, 31), 100, 10, 0
        ));
        InventoryItem batch3 = storage.create(new InventoryItem(
                0, 10, "BATCH-C", LocalDate.of(2024, 12, 31), 75, 10, 0
        ));

        // Create allocation plan: take 40 from batch1, 50 from batch2, 10 from batch3
        Map<Long, Integer> plan = new HashMap<>();
        plan.put(batch1.getInventoryId(), 40);
        plan.put(batch2.getInventoryId(), 50);
        plan.put(batch3.getInventoryId(), 10);

        StockMovement movement = new StockMovement(
                0, 0, 10, StockMovementType.DISPENSE, 100, 100, null, LocalDateTime.now(), 1
        );

        boolean success = storage.deductInventory(plan, movement);
        assertTrue(success);

        // Verify quantities were deducted
        assertEquals(10, storage.findById(batch1.getInventoryId()).get().getQuantityOnHand());
        assertEquals(50, storage.findById(batch2.getInventoryId()).get().getQuantityOnHand());
        assertEquals(65, storage.findById(batch3.getInventoryId()).get().getQuantityOnHand());

        // Versions should be incremented
        assertEquals(2, storage.findById(batch1.getInventoryId()).get().getVersion());
        assertEquals(2, storage.findById(batch2.getInventoryId()).get().getVersion());
        assertEquals(2, storage.findById(batch3.getInventoryId()).get().getVersion());
    }

    @Test
    void testDeductInventoryInsufficientStock() {
        // Create a batch with 50 units
        InventoryItem batch = storage.create(new InventoryItem(
                0, 11, "BATCH-X", LocalDate.of(2025, 12, 31), 50, 10, 0
        ));

        // Try to deduct 100 (more than available)
        Map<Long, Integer> plan = new HashMap<>();
        plan.put(batch.getInventoryId(), 100);

        StockMovement movement = new StockMovement(
                0, 0, 11, StockMovementType.DISPENSE, 100, 100, null, LocalDateTime.now(), 1
        );

        boolean success = storage.deductInventory(plan, movement);
        assertFalse(success);

        // Verify no deduction happened
        assertEquals(50, storage.findById(batch.getInventoryId()).get().getQuantityOnHand());
        assertEquals(1, storage.findById(batch.getInventoryId()).get().getVersion());
    }

    @Test
    void testDeductInventoryInvalidBatch() {
        Map<Long, Integer> plan = new HashMap<>();
        plan.put(999L, 50); // Non-existent batch ID

        StockMovement movement = new StockMovement(
                0, 0, 12, StockMovementType.DISPENSE, 50, 50, null, LocalDateTime.now(), 1
        );

        boolean success = storage.deductInventory(plan, movement);
        assertFalse(success);
    }

    @Test
    void testAdjustStockSuccess() {
        InventoryItem item = storage.create(new InventoryItem(
                0, 20, "BATCH-ADJ", LocalDate.of(2025, 12, 31), 100, 20, 0
        ));

        InventoryItem modified = new InventoryItem(
                item.getInventoryId(), 20, "BATCH-ADJ", LocalDate.of(2025, 12, 31), 120, 20, 0
        );

        StockMovement movement = new StockMovement(
                0, item.getInventoryId(), 20, StockMovementType.ADJUSTMENT, 20, 120, "Stock count correction", LocalDateTime.now(), 1
        );

        boolean success = storage.adjustStock(modified, movement, 1);
        assertTrue(success);

        var updated = storage.findById(item.getInventoryId());
        assertEquals(120, updated.get().getQuantityOnHand());
        assertEquals(2, updated.get().getVersion());
    }

    @Test
    void testAdjustStockVersionConflict() {
        InventoryItem item = storage.create(new InventoryItem(
                0, 21, "BATCH-ADJ2", LocalDate.of(2025, 12, 31), 100, 20, 0
        ));

        InventoryItem modified = new InventoryItem(
                item.getInventoryId(), 21, "BATCH-ADJ2", LocalDate.of(2025, 12, 31), 120, 20, 0
        );

        StockMovement movement = new StockMovement(
                0, item.getInventoryId(), 21, StockMovementType.ADJUSTMENT, 20, 120, "Stock count correction", LocalDateTime.now(), 1
        );

        boolean success = storage.adjustStock(modified, movement, 0); // Wrong version
        assertFalse(success);

        var unchanged = storage.findById(item.getInventoryId());
        assertEquals(100, unchanged.get().getQuantityOnHand());
    }

    @Test
    void testAdjustStockNegativeBalance() {
        InventoryItem item = storage.create(new InventoryItem(
                0, 22, "BATCH-NEG", LocalDate.of(2025, 12, 31), 50, 20, 0
        ));

        InventoryItem modified = new InventoryItem(
                item.getInventoryId(), 22, "BATCH-NEG", LocalDate.of(2025, 12, 31), -10, 20, 0
        );

        StockMovement movement = new StockMovement(
                0, item.getInventoryId(), 22, StockMovementType.ADJUSTMENT, -60, -10, "Correction attempt", LocalDateTime.now(), 1
        );

        boolean success = storage.adjustStock(modified, movement, 1);
        assertFalse(success);

        var unchanged = storage.findById(item.getInventoryId());
        assertEquals(50, unchanged.get().getQuantityOnHand());
    }

    @Test
    void testListAll() {
        storage.create(new InventoryItem(0, 30, "BATCH-1", LocalDate.of(2025, 12, 31), 100, 20, 0));
        storage.create(new InventoryItem(0, 30, "BATCH-2", LocalDate.of(2025, 12, 31), 150, 20, 0));

        List<InventoryItem> all = storage.listAll();
        assertEquals(2, all.size());
    }

    @Test
    void testQueryForReport() {
        storage.create(new InventoryItem(0, 40, "BATCH-RPT", LocalDate.of(2024, 6, 30), 100, 20, 0));

        var rows = storage.queryForReport(new HashMap<>());
        assertEquals(1, rows.size());
        assertEquals(100L, rows.get(0).get("quantityOnHand"));
    }
}
