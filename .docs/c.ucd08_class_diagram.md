@startuml
title UCD-08 — Dispense Medication
Class Diagram

skinparam classAttributeIconSize 0
skinparam linetype ortho
hide empty members


package "view.pharmacy_operations.ucd08_dispense_medication" {

    class DispenseMedicationView <<boundary>> {

        -selectedPrescriptionId : long
        -currentRecord : DispenseRecord

        +openDispenseMedication() : void

        +requestEligiblePrescription(
            prescriptionId : long
        ) : void

        +displayDispensingRecord(
            record : DispenseRecord
        ) : void

        +showDispensingUnavailable() : void
        +showOperationError(message : String) : void
    }


    class DispenseFormView <<boundary>> {

        -patientId : long
        -dispenseQuantities : Map<Long, Integer>

        +displayDispensingDetails(
            record : DispenseRecord
        ) : void

        +collectDispenseQuantities() : Map<Long, Integer>

        +submitDispensingConfirmation() : void

        +showPatientVerificationError() : void
        +showPrescriptionNotEligible() : void
        +showInsufficientStock() : void
        +showQuantityMismatch() : void
        +showConcurrentStockError() : void
        +showInventoryUpdateError() : void
    }


    class DispenseResultView <<boundary>> {

        -result : DispenseRecord

        +displayDispenseResult(
            record : DispenseRecord
        ) : void

        +showDispensingFailed() : void
    }
}


package "controller.pharmacy_operations" {

    class DispenseMedicationController <<control>> {

        +requestEligiblePrescription(
            prescriptionId : long
        ) : DispenseRecord

        +verifyPrescriptionAndPatient(
            prescriptionId : long,
            patientId : long
        ) : boolean

        +checkStockAvailability(
            quantities : Map<Long, Integer>
        ) : boolean

        +confirmDispensing(
            dispenseId : long,
            quantities : Map<Long, Integer>
        ) : boolean

        +completeDispensing(
            dispenseId : long
        ) : DispenseRecord

        -validatePrescriptionEligibility(
            prescription : Prescription
        ) : boolean

        -validateDispenseQuantities(
            record : DispenseRecord,
            quantities : Map<Long, Integer>
        ) : boolean
    }
}


package "model.pharmacy_operations" {

    class DispenseRecord <<entity>> {

        -dispenseId : long
        -prescriptionId : long
        -patientId : long
        -pharmacistId : long

        -requiredQuantities : Map<Long, Integer>
        -dispensedQuantities : Map<Long, Integer>

        -status : String

        -verifiedAt : LocalDateTime
        -dispensedAt : LocalDateTime
        -createdAt : LocalDateTime
        -updatedAt : LocalDateTime

        -failureReason : String
        -version : long

        +verifyPatient(
            patientId : long
        ) : boolean

        +validateQuantities(
            quantities : Map<Long, Integer>
        ) : boolean

        +confirmDispensing(
            quantities : Map<Long, Integer>
        ) : void

        +completeFulfilment(
            pharmacistId : long
        ) : void

        +markFailed(
            reason : String
        ) : void

        +isCompleted() : boolean
        +isEligibleForDispensing() : boolean
    }


    class InventoryItem <<entity>> {

        -inventoryId : long
        -medicineId : long

        -batchNumber : String
        -expiryDate : LocalDate

        -quantityOnHand : int

        -updatedAt : LocalDateTime
        -version : long

        +getAvailableQuantity() : int

        +hasAvailableStock(
            requiredQuantity : int
        ) : boolean

        +canDeduct(
            quantity : int
        ) : boolean

        +deduct(
            quantity : int
        ) : void

        +isExpired() : boolean
    }
}


package "storage.pharmacy_operations" {

    class DispenseStorage <<repository>> {

        +findById(
            dispenseId : long
        ) : DispenseRecord

        +findByPrescriptionId(
            prescriptionId : long
        ) : DispenseRecord

        +save(
            record : DispenseRecord
        ) : boolean

        +update(
            record : DispenseRecord,
            expectedVersion : long
        ) : boolean

        +existsCompletedDispense(
            prescriptionId : long
        ) : boolean
    }


    class InventoryStorage <<repository>> {

        +findByMedicineId(
            medicineId : long
        ) : List<InventoryItem>

        +checkStockAvailability(
            quantities : Map<Long, Integer>
        ) : boolean

        +deductInventory(
            quantities : Map<Long, Integer>
        ) : boolean

        +update(
            item : InventoryItem,
            expectedVersion : long
        ) : boolean
    }
}


package "storage.clinical_prescription" {

    class PrescriptionStorage <<repository>> {

        +findById(
            prescriptionId : long
        ) : Prescription
    }
}


package "model.clinical_prescription" {

    class Prescription <<entity>> {

        -prescriptionId : long
        -patientId : long
        -status : PrescriptionStatus

        +getPatientId() : long
        +getStatus() : PrescriptionStatus
        +isEligibleForDispensing() : boolean
    }

    enum PrescriptionStatus
}


' =====================================================
' VIEW
' =====================================================

DispenseMedicationView *-- DispenseFormView
DispenseMedicationView *-- DispenseResultView

DispenseMedicationView ..> DispenseMedicationController
DispenseFormView ..> DispenseMedicationController


' =====================================================
' CONTROLLER
' =====================================================

DispenseMedicationController ..> DispenseStorage
DispenseMedicationController ..> InventoryStorage

DispenseMedicationController ..> PrescriptionStorage : <<cross-domain read>>

DispenseMedicationController ..> DispenseRecord
DispenseMedicationController ..> InventoryItem
DispenseMedicationController ..> Prescription


' =====================================================
' STORAGE
' =====================================================

DispenseStorage ..> DispenseRecord : persists
InventoryStorage ..> InventoryItem : persists
PrescriptionStorage ..> Prescription : retrieves


note right of DispenseRecord
One completed prescription should
not be dispensed twice.

existsCompletedDispense()
provides duplicate fulfilment protection.
end note


note bottom of InventoryItem
version provides optimistic locking.

Stock must be revalidated at
deduction time because the quantity
may change after the initial check.
end note


note bottom of DispenseMedicationController
The controller coordinates:

1. Prescription validation
2. Patient verification
3. Quantity validation
4. Inventory deduction
5. Dispense persistence

The View never changes stock directly.
end note

@enduml