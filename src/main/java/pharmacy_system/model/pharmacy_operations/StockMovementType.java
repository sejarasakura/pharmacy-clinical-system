package pharmacy_system.model.pharmacy_operations;

/**
 * Enumeration of stock movement types in inventory audit trail.
 * Represents the reason for inventory quantity change.
 * 
 * [UCD-08, UCD-10]
 */
public enum StockMovementType {
    RECEIVE,     // stock receipt from supplier
    ADJUSTMENT,  // manual adjustment (requires reason)
    DISPENSE     // quantity dispensed to patient
}
