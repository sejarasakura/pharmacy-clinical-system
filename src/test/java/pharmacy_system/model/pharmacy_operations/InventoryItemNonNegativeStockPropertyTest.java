package pharmacy_system.model.pharmacy_operations;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Positive;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for non-negative stock invariant (Property 5, Requirements 9.5).
 *
 * <p>These tests validate that inventory balances never go negative through:
 * <ul>
 *   <li>Stock receipts increase quantity correctly</li>
 *   <li>Stock deductions decrease quantity correctly</li>
 *   <li>Any operation that would reduce balance below 0 is rejected before commit</li>
 *   <li>Expired batches contribute 0 to available quantity but records are retained</li>
 * </ul>
 *
 * <p>**Validates: Requirements 9.5**
 */
public class InventoryItemNonNegativeStockPropertyTest {

    /**
     * Property 5.1: After any valid receipt operation, the quantity on hand
     * increases by exactly the received amount.
     */
    @Property
    void receiptIncreasesQuantityCorrectly(
            @ForAll @IntRange(min = 1, max = 1000000) int initialQuantity,
            @ForAll @IntRange(min = 1, max = 1000000) int receiveQuantity) {
        LocalDate futureDate = LocalDate.now().plusDays(30);
        InventoryItem item = new InventoryItem(1L, 100L, "BATCH1", futureDate, initialQuantity);
        int originalQuantity = item.getQuantityOnHand();

        item.receive(receiveQuantity);

        assertEquals(originalQuantity + receiveQuantity, item.getQuantityOnHand(),
                "Receipt should increase quantity by exactly the received amount");
        assertTrue(item.getQuantityOnHand() > 0,
                "After receipt, quantity should always be positive");
    }

    /**
     * Property 5.2: After any valid deduction operation, the quantity on hand
     * decreases by exactly the deducted amount, and remains non-negative.
     */
    @Property
    void deductionDecreasesQuantityCorrectly(
            @ForAll @IntRange(min = 1, max = 1000) int initialQuantity,
            @ForAll @IntRange(min = 1, max = 1000) int deductQuantity) {
        if (deductQuantity > initialQuantity) return;

        LocalDate futureDate = LocalDate.now().plusDays(30);
        InventoryItem item = new InventoryItem(1L, 100L, "BATCH2", futureDate, initialQuantity);
        int originalQuantity = item.getQuantityOnHand();

        item.deduct(deductQuantity);

        assertEquals(originalQuantity - deductQuantity, item.getQuantityOnHand(),
                "Deduction should decrease quantity by exactly the deducted amount");
        assertTrue(item.getQuantityOnHand() >= 0,
                "After deduction, quantity should never go negative");
    }

    /**
     * Property 5.3: Operations that would reduce balance below 0 are rejected
     * before commit and quantity remains unchanged.
     */
    @Property
    void deductionPreventingNegativeBalance(
            @ForAll @IntRange(min = 1, max = 1000) int initialQuantity) {
        LocalDate futureDate = LocalDate.now().plusDays(30);
        InventoryItem item = new InventoryItem(1L, 100L, "BATCH3", futureDate, initialQuantity);
        int originalQuantity = item.getQuantityOnHand();
        int excessiveDeduction = initialQuantity + 1;

        assertFalse(item.canDeduct(excessiveDeduction),
                "canDeduct should return false when deduction exceeds available quantity");
        assertThrows(IllegalStateException.class, () -> item.deduct(excessiveDeduction),
                "deduct should throw IllegalStateException when deduction would go negative");
        assertEquals(originalQuantity, item.getQuantityOnHand(),
                "Quantity should remain unchanged after rejected deduction");
    }

    /**
     * Property 5.4: Operations that would reduce balance below 0 are rejected
     * before commit for adjustments.
     */
    @Property
    void adjustmentPreventingNegativeBalance(
            @ForAll @IntRange(min = 1, max = 1000) int initialQuantity) {
        LocalDate futureDate = LocalDate.now().plusDays(30);
        InventoryItem item = new InventoryItem(1L, 100L, "BATCH4", futureDate, initialQuantity);
        int originalQuantity = item.getQuantityOnHand();
        int excessiveDecrement = -(initialQuantity + 1);

        assertFalse(item.canAdjust(excessiveDecrement),
                "canAdjust should return false when adjustment would take balance below zero");
        assertThrows(IllegalStateException.class, () -> item.adjust(excessiveDecrement),
                "adjust should throw IllegalStateException when adjustment would go negative");
        assertEquals(originalQuantity, item.getQuantityOnHand(),
                "Quantity should remain unchanged after rejected adjustment");
    }

    /**
     * Property 5.5: Valid adjustments are applied correctly and do not result in negative balance.
     */
    @Property
    void validAdjustmentChangesQuantityCorrectly(
            @ForAll @IntRange(min = 10, max = 1000) int initialQuantity,
            @ForAll @IntRange(min = -10, max = 10) int adjustmentDelta) {
        if (initialQuantity + adjustmentDelta < 0) return;

        LocalDate futureDate = LocalDate.now().plusDays(30);
        InventoryItem item = new InventoryItem(1L, 100L, "BATCH5", futureDate, initialQuantity);
        int originalQuantity = item.getQuantityOnHand();

        item.adjust(adjustmentDelta);

        assertEquals(originalQuantity + adjustmentDelta, item.getQuantityOnHand(),
                "Adjustment should change quantity by exactly the adjustment amount");
        assertTrue(item.getQuantityOnHand() >= 0,
                "After adjustment, quantity should never go negative");
    }

    /**
     * Property 5.6: Expired batches contribute 0 to available quantity
     * but the record is retained with the physical balance unchanged.
     */
    @Property
    void expiredBatchContributesZeroToAvailableQuantity(
            @ForAll @Positive int quantityOnHand) {
        LocalDate expiredDate = LocalDate.now().minusDays(1);
        InventoryItem expiredItem = new InventoryItem(1L, 100L, "EXPIRED_BATCH", expiredDate, quantityOnHand);

        assertTrue(expiredItem.isExpired(),
                "Batch should be marked as expired");
        assertEquals(0, expiredItem.getAvailableQuantity(),
                "Expired batch should contribute 0 to available quantity");
        assertEquals(quantityOnHand, expiredItem.getQuantityOnHand(),
                "Physical balance should be unchanged even for expired batch");
    }

    /**
     * Property 5.7: Non-expired batches contribute their full quantity to available quantity.
     */
    @Property
    void nonExpiredBatchContributesFullQuantityToAvailable(
            @ForAll @Positive int quantityOnHand) {
        LocalDate futureDate = LocalDate.now().plusDays(30);
        InventoryItem item = new InventoryItem(1L, 100L, "FUTURE_BATCH", futureDate, quantityOnHand);

        assertFalse(item.isExpired(),
                "Batch should not be marked as expired");
        assertEquals(quantityOnHand, item.getAvailableQuantity(),
                "Non-expired batch should contribute its full quantity to available");
        assertEquals(quantityOnHand, item.getQuantityOnHand(),
                "Physical balance should match available quantity for non-expired batch");
    }

    /**
     * Property 5.8: canDeduct predicts whether deduct will succeed.
     */
    @Property
    void canDeductPredictsBehavior(
            @ForAll @IntRange(min = 1, max = 1000) int initialQuantity,
            @ForAll @IntRange(min = 0, max = 2000) int deductQuantity) {
        LocalDate futureDate = LocalDate.now().plusDays(30);
        InventoryItem item = new InventoryItem(1L, 100L, "BATCH7", futureDate, initialQuantity);
        boolean canDeduct = item.canDeduct(deductQuantity);

        if (canDeduct) {
            assertDoesNotThrow(() -> item.deduct(deductQuantity),
                    "If canDeduct returns true, deduct should not throw");
        } else {
            InventoryItem testItem = new InventoryItem(1L, 100L, "BATCH7", futureDate, initialQuantity);
            assertThrows(IllegalStateException.class, () -> testItem.deduct(deductQuantity),
                    "If canDeduct returns false, deduct should throw");
        }
    }

    /**
     * Property 5.9: canAdjust predicts whether adjust will succeed.
     */
    @Property
    void canAdjustPredictsBehavior(
            @ForAll @IntRange(min = 1, max = 1000) int initialQuantity,
            @ForAll @IntRange(min = -2000, max = 2000) int adjustmentDelta) {
        LocalDate futureDate = LocalDate.now().plusDays(30);
        InventoryItem item = new InventoryItem(1L, 100L, "BATCH8", futureDate, initialQuantity);
        boolean canAdjust = item.canAdjust(adjustmentDelta);

        if (canAdjust) {
            assertDoesNotThrow(() -> item.adjust(adjustmentDelta),
                    "If canAdjust returns true, adjust should not throw");
        } else {
            InventoryItem testItem = new InventoryItem(1L, 100L, "BATCH8", futureDate, initialQuantity);
            assertThrows(IllegalStateException.class, () -> testItem.adjust(adjustmentDelta),
                    "If canAdjust returns false, adjust should throw");
        }
    }

    /**
     * Example-based test: Deduction to exact zero is valid.
     */
    @Example
    void deductionToExactZeroIsValid() {
        LocalDate futureDate = LocalDate.now().plusDays(30);
        InventoryItem item = new InventoryItem(1L, 100L, "EXACT_ZERO_BATCH", futureDate, 50);

        assertTrue(item.canDeduct(50), "canDeduct to exact zero should return true");
        assertDoesNotThrow(() -> item.deduct(50), "deduct to exact zero should succeed");
        assertEquals(0, item.getQuantityOnHand(), "Quantity should be exactly zero");
        assertTrue(item.getQuantityOnHand() >= 0, "Zero is still non-negative");
    }

    /**
     * Example-based test: Deduction one beyond capacity is rejected.
     */
    @Example
    void deductionOneBeyondCapacityIsRejected() {
        LocalDate futureDate = LocalDate.now().plusDays(30);
        InventoryItem item = new InventoryItem(1L, 100L, "OVER_CAPACITY_BATCH", futureDate, 50);

        assertFalse(item.canDeduct(51), "canDeduct beyond capacity should return false");
        assertThrows(IllegalStateException.class, () -> item.deduct(51),
                "deduct beyond capacity should throw");
        assertEquals(50, item.getQuantityOnHand(), "Quantity should remain unchanged");
    }

    /**
     * Example-based test: Zero adjustment does not change quantity.
     */
    @Example
    void zeroAdjustmentDoesNotChangeQuantity() {
        LocalDate futureDate = LocalDate.now().plusDays(30);
        InventoryItem item = new InventoryItem(1L, 100L, "ZERO_BATCH", futureDate, 50);
        int originalQuantity = item.getQuantityOnHand();

        assertTrue(item.canAdjust(0), "canAdjust(0) should return true");
        item.adjust(0);

        assertEquals(originalQuantity, item.getQuantityOnHand(),
                "Zero adjustment should not change quantity");
    }

    /**
     * Example-based test: Negative deduction is rejected.
     */
    @Example
    void negativeDeductionIsRejected() {
        LocalDate futureDate = LocalDate.now().plusDays(30);
        InventoryItem item = new InventoryItem(1L, 100L, "NEG_BATCH", futureDate, 50);
        int originalQuantity = item.getQuantityOnHand();

        assertFalse(item.canDeduct(-1),
                "canDeduct should return false for negative amounts");
        assertThrows(IllegalStateException.class, () -> item.deduct(-1),
                "deduct should throw IllegalStateException for negative amounts");
        assertEquals(originalQuantity, item.getQuantityOnHand(),
                "Quantity should remain unchanged after failed deduction attempt");
    }
}
