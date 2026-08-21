package pharmacy_system.model.pharmacy_operations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable audit record for a change to an inventory batch. An adjustment
 * cannot be recorded unless it includes a meaningful reason.
 */
public final class StockMovement {
    private final long movementId;
    private final long inventoryId;
    private final long medicineId;
    private final StockMovementType movementType;
    private final int quantityDelta;
    private final int balanceAfter;
    private final String reason;
    private final long performedBy;
    private final LocalDateTime createdAt;

    public StockMovement(
            long movementId,
            long inventoryId,
            long medicineId,
            StockMovementType movementType,
            int quantityDelta,
            int balanceAfter,
            String reason,
            long performedBy,
            LocalDateTime createdAt) {
        this.movementId = movementId;
        this.inventoryId = inventoryId;
        this.medicineId = medicineId;
        this.movementType = movementType;
        this.quantityDelta = quantityDelta;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.performedBy = performedBy;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;

        List<String> errors = validateMovement();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    /** Compatibility overload retaining the persisted-record field order used by storage clients. */
    public StockMovement(
            long movementId,
            long inventoryId,
            long medicineId,
            StockMovementType movementType,
            int quantityDelta,
            int balanceAfter,
            String reason,
            LocalDateTime createdAt,
            long performedBy) {
        this(movementId, inventoryId, medicineId, movementType, quantityDelta,
                balanceAfter, reason, performedBy, createdAt);
    }

    /**
     * Creates a timestamped audit record after enforcing movement validation.
     */
    public static StockMovement createMovement(
            StockMovementType movementType,
            long movementId,
            long inventoryId,
            long medicineId,
            int quantityDelta,
            int balanceAfter,
            String reason,
            long performedBy) {
        return new StockMovement(
                movementId,
                inventoryId,
                medicineId,
                movementType,
                quantityDelta,
                balanceAfter,
                reason,
                performedBy,
                LocalDateTime.now());
    }

    /** Alias for concise inventory-operation construction. */
    public static StockMovement of(
            StockMovementType movementType,
            long movementId,
            long inventoryId,
            long medicineId,
            int quantityDelta,
            int balanceAfter,
            String reason,
            long performedBy) {
        return createMovement(
                movementType,
                movementId,
                inventoryId,
                medicineId,
                quantityDelta,
                balanceAfter,
                reason,
                performedBy);
    }

    /**
     * Checks the audit record's invariants without changing it.
     *
     * @return field-specific validation errors
     */
    public List<String> validateMovement() {
        List<String> errors = new ArrayList<String>();
        if (movementType == null) {
            errors.add("movementType is required");
        } else if (movementType == StockMovementType.ADJUSTMENT && isBlank(reason)) {
            errors.add("reason is required for ADJUSTMENT");
        }
        return Collections.unmodifiableList(errors);
    }

    public boolean isReceipt() {
        return movementType == StockMovementType.RECEIVE;
    }

    public boolean isAdjustment() {
        return movementType == StockMovementType.ADJUSTMENT;
    }

    public boolean isDispense() {
        return movementType == StockMovementType.DISPENSE;
    }

    public long getMovementId() {
        return movementId;
    }

    public long getInventoryId() {
        return inventoryId;
    }

    public long getMedicineId() {
        return medicineId;
    }

    public StockMovementType getMovementType() {
        return movementType;
    }

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    public String getReason() {
        return reason;
    }

    public long getPerformedBy() {
        return performedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    private static boolean isBlank(String value) {
        return value == null || value.codePoints().allMatch(Character::isWhitespace);
    }
}
