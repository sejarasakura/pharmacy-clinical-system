@startuml
title UCD-10 — Manage Medicine Inventory
Class Diagram

skinparam classAttributeIconSize 0
skinparam linetype ortho
hide empty members


package "view.pharmacy_operations.ucd10_manage_medicine_inventory" {

    class ManageMedicineInventoryView <<boundary>> {

        -selectedMedicineId : long

        +openMedicineInventory() : void

        +displayInventory(
            medicines : List<Medicine>,
            inventory : List<InventoryItem>
        ) : void

        +displayInventoryDetails(
            medicine : Medicine,
            inventory : List<InventoryItem>
        ) : void

        +showMedicineCreated() : void
        +showMedicineUpdated() : void
        +showInventoryUpdated() : void

        +showOperationError(
            message : String
        ) : void
    }


    class MedicineListView <<boundary>> {

        -medicines : List<Medicine>
        -inventoryItems : List<InventoryItem>
        -selectedMedicineId : long

        +displayInventory(
            medicines : List<Medicine>,
            inventoryItems : List<InventoryItem>
        ) : void

        +selectMedicine(
            medicineId : long
        ) : void
    }


    class MedicineFormView <<boundary>> {

        -medicineData : Map<String, Object>

        +collectMedicineData() : Map<String, Object>

        +populateMedicine(
            medicine : Medicine
        ) : void

        +submitMedicine() : void

        +showDuplicateMedicineError() : void

        +showValidationErrors(
            errors : List<String>
        ) : void

        +showSaveError() : void
    }


    class StockAdjustmentView <<boundary>> {

        -inventoryId : long
        -quantity : int
        -batchNumber : String
        -expiryDate : LocalDate
        -reason : String

        +collectStockData() : Map<String, Object>

        +submitStockReceipt() : void
        +submitStockAdjustment() : void

        +showQuantityError() : void
        +showExpiryError() : void
        +showNegativeStockError() : void
        +showConcurrentUpdateError() : void
    }
}


package "controller.pharmacy_operations" {

    class ManageMedicineInventoryController <<control>> {

        +loadInventory() : void

        +createMedicine(
            medicineData : Map<String, Object>
        ) : Medicine

        +loadMedicine(
            medicineId : long
        ) : Medicine

        +updateMedicine(
            medicineId : long,
            changes : Map<String, Object>
        ) : boolean

        +receiveStock(
            medicineId : long,
            batchNumber : String,
            expiryDate : LocalDate,
            quantity : int
        ) : boolean

        +adjustStock(
            inventoryId : long,
            quantityDelta : int,
            reason : String
        ) : boolean

        +viewInventoryDetails(
            medicineId : long
        ) : List<InventoryItem>

        -validateMedicineData(
            medicine : Medicine
        ) : List<String>

        -validateStockOperation(
            item : InventoryItem,
            quantityDelta : int
        ) : boolean
    }
}


package "model.pharmacy_operations" {

    class Medicine <<entity>> {

        -medicineId : long

        -medicineCode : String
        -medicineName : String
        -genericName : String

        -dosageForm : String
        -strength : String
        -unit : String

        -description : String
        -active : boolean

        -createdAt : LocalDateTime
        -updatedAt : LocalDateTime

        -version : long

        +validateMedicineInformation() : List<String>

        +applyChanges(
            changes : Map<String, Object>
        ) : void

        +activate() : void
        +deactivate() : void

        +isActive() : boolean
    }


    class InventoryItem <<entity>> {

        -inventoryId : long
        -medicineId : long

        -batchNumber : String
        -expiryDate : LocalDate

        -quantityOnHand : int
        -reorderLevel : int

        -createdAt : LocalDateTime
        -updatedAt : LocalDateTime

        -version : long

        +validateStockData(
            quantity : int,
            expiryDate : LocalDate
        ) : List<String>

        +receive(
            quantity : int
        ) : void

        +adjust(
            quantityDelta : int
        ) : void

        +canAdjust(
            quantityDelta : int
        ) : boolean

        +isExpired() : boolean

        +isLowStock() : boolean

        +getAvailableQuantity() : int
    }


    class StockMovement <<entity>> {

        -movementId : long
        -inventoryId : long
        -medicineId : long

        -movementType : StockMovementType

        -quantityDelta : int
        -balanceAfter : int

        -reason : String

        -performedBy : long
        -createdAt : LocalDateTime

        +createMovement(
            type : StockMovementType,
            inventoryItem : InventoryItem,
            quantityDelta : int,
            reason : String,
            performedBy : long
        ) : void

        +isReceipt() : boolean
        +isAdjustment() : boolean
    }


    enum StockMovementType {
        RECEIVE
        ADJUSTMENT
        DISPENSE
    }
}


package "storage.pharmacy_operations" {

    class MedicineStorage <<repository>> {

        +findAll() : List<Medicine>

        +findById(
            medicineId : long
        ) : Medicine

        +findByCode(
            medicineCode : String
        ) : Medicine

        +existsByCode(
            medicineCode : String
        ) : boolean

        +existsByName(
            medicineName : String
        ) : boolean

        +save(
            medicine : Medicine
        ) : boolean

        +update(
            medicine : Medicine,
            expectedVersion : long
        ) : boolean
    }


    class InventoryStorage <<repository>> {

        +findAll() : List<InventoryItem>

        +findById(
            inventoryId : long
        ) : InventoryItem

        +findByMedicineId(
            medicineId : long
        ) : List<InventoryItem>

        +findByMedicineAndBatch(
            medicineId : long,
            batchNumber : String
        ) : InventoryItem

        +receiveStock(
            item : InventoryItem,
            movement : StockMovement,
            expectedVersion : long
        ) : boolean

        +adjustStock(
            item : InventoryItem,
            movement : StockMovement,
            expectedVersion : long
        ) : boolean

        +findMovementsByInventoryId(
            inventoryId : long
        ) : List<StockMovement>
    }
}


' =====================================================
' VIEW
' =====================================================

ManageMedicineInventoryView *-- MedicineListView
ManageMedicineInventoryView *-- MedicineFormView
ManageMedicineInventoryView *-- StockAdjustmentView

MedicineListView ..> ManageMedicineInventoryController
MedicineFormView ..> ManageMedicineInventoryController
StockAdjustmentView ..> ManageMedicineInventoryController


' =====================================================
' CONTROLLER
' =====================================================

ManageMedicineInventoryController ..> MedicineStorage
ManageMedicineInventoryController ..> InventoryStorage

ManageMedicineInventoryController ..> Medicine
ManageMedicineInventoryController ..> InventoryItem
ManageMedicineInventoryController ..> StockMovement


' =====================================================
' DOMAIN
' =====================================================

Medicine "1" -- "0..*" InventoryItem : stock batches >

InventoryItem "1" -- "0..*" StockMovement : movement history >

StockMovement --> StockMovementType


' =====================================================
' STORAGE
' =====================================================

MedicineStorage ..> Medicine : persists

InventoryStorage ..> InventoryItem : persists
InventoryStorage ..> StockMovement : persists


note right of Medicine
Medicine is master data.

It does not contain the current
stock quantity directly because one
medicine can have multiple batches.
end note


note right of InventoryItem
One InventoryItem represents
one stock/batch position.

quantityOnHand belongs here,
not in Medicine.
end note


note right of StockMovement
StockMovement is the audit trail.

quantityDelta examples:
+100 RECEIVE
-10 DISPENSE
-5 ADJUSTMENT
end note


note bottom of InventoryStorage
There is no StockMovementStorage.java
in the defined architecture.

Therefore InventoryStorage must
persist the stock balance change
and StockMovement together.

receiveStock() / adjustStock()
should behave transactionally.
end note

@enduml