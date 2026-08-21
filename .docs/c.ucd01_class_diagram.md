@startuml
title UCD-01 — Manage Prescription
Class Diagram

skinparam classAttributeIconSize 0
skinparam linetype ortho
hide empty members


package "view.clinical_prescription.ucd01_manage_prescription" {

    class ManagePrescriptionView <<boundary>> {
        -selectedPrescriptionId : long

        +openPrescriptionManagement() : void
        +displayPrescription(prescription : Prescription) : void
        +displayPrescriptionList(prescriptions : List<Prescription>) : void

        +showCreateSuccess() : void
        +showUpdateSuccess() : void
        +showCancellationSuccess() : void

        +showPrescriptionNotFound() : void
        +showOperationError(message : String) : void
    }


    class PrescriptionFormView <<boundary>> {
        -patientId : long
        -clinicalNotes : String

        +collectPrescriptionData() : Map<String, Object>
        +populateForm(prescription : Prescription) : void
        +submitPrescription() : void

        +showValidationErrors(errors : List<String>) : void
        +showSaveError() : void
    }


    class PrescriptionItemView <<boundary>> {
        -medicineId : long
        -medicineName : String
        -dosage : String
        -quantity : int
        -frequency : String
        -route : String
        -instructions : String

        +collectItemData() : Map<String, Object>
        +populateItem(item : PrescriptionItem) : void
        +showItemValidationErrors(errors : List<String>) : void
    }
}


package "controller.clinical_prescription" {

    class ManagePrescriptionController <<control>> {

        +loadPrescription(
            prescriptionId : long
        ) : Prescription

        +loadDoctorPrescriptions(
            doctorId : long
        ) : List<Prescription>

        +createPrescription(
            patientId : long
        ) : Prescription

        +addPrescriptionItem(
            prescriptionId : long,
            item : PrescriptionItem
        ) : boolean

        +updatePrescriptionItem(
            prescriptionId : long,
            itemId : long,
            changes : Map<String, Object>
        ) : boolean

        +removePrescriptionItem(
            prescriptionId : long,
            itemId : long
        ) : boolean

        +updatePrescription(
            prescriptionId : long,
            changes : Map<String, Object>
        ) : boolean

        +savePrescription(
            prescription : Prescription
        ) : boolean

        +cancelPrescription(
            prescriptionId : long,
            reason : String
        ) : boolean

        -requireDoctorAccess() : long
        -validateForPersistence(
            prescription : Prescription
        ) : List<String>
    }
}


package "controller.common" {

    class SessionController <<control>> {
        +getCurrentUserId() : long
        +isAuthenticated() : boolean
        +hasPermission(permissionCode : String) : boolean
        +requirePermission(permissionCode : String) : void
    }
}


package "model.clinical_prescription" {

    class Prescription <<entity>> {

        -prescriptionId : long
        -patientId : long
        -doctorId : long

        -clinicalNotes : String

        -status : PrescriptionStatus

        -createdAt : LocalDateTime
        -updatedAt : LocalDateTime

        -cancelledAt : LocalDateTime
        -cancellationReason : String

        -version : long

        -items : List<PrescriptionItem>

        +addItem(item : PrescriptionItem) : void

        +updateItem(
            itemId : long,
            changes : Map<String, Object>
        ) : boolean

        +removeItem(itemId : long) : boolean

        +findItem(itemId : long) : PrescriptionItem

        +updateClinicalNotes(notes : String) : void

        +validateClinicalContent() : List<String>

        +canBeEdited() : boolean
        +canBeCancelled() : boolean

        +cancel(
            reason : String,
            cancelledBy : long
        ) : void

        +getStatus() : PrescriptionStatus
    }


    class PrescriptionItem <<entity>> {

        -prescriptionItemId : long

        -medicineId : long
        -medicineName : String

        -dosage : String
        -dosageUnit : String

        -quantity : int

        -frequency : String
        -route : String
        -durationDays : int

        -instructions : String

        +validate() : List<String>

        +updateDosage(
            dosage : String,
            unit : String
        ) : void

        +updateQuantity(quantity : int) : void

        +updateFrequency(frequency : String) : void

        +updateInstructions(
            instructions : String
        ) : void
    }


    enum PrescriptionStatus {
        DRAFT
        ISSUED
        ON_HOLD
        CANCELLED
        EXPIRED
    }
}


package "storage.clinical_prescription" {

    class PrescriptionStorage <<repository>> {

        +findById(
            prescriptionId : long
        ) : Prescription

        +findByPatientId(
            patientId : long
        ) : List<Prescription>

        +findByDoctorId(
            doctorId : long
        ) : List<Prescription>

        +save(
            prescription : Prescription
        ) : boolean

        +update(
            prescription : Prescription,
            expectedVersion : long
        ) : boolean

        +existsById(
            prescriptionId : long
        ) : boolean
    }
}


' =====================================================
' VIEW COMPOSITION
' =====================================================

ManagePrescriptionView *-- PrescriptionFormView
PrescriptionFormView *-- PrescriptionItemView

ManagePrescriptionView ..> ManagePrescriptionController
PrescriptionFormView ..> ManagePrescriptionController
PrescriptionItemView ..> ManagePrescriptionController


' =====================================================
' CONTROLLER DEPENDENCIES
' =====================================================

ManagePrescriptionController ..> SessionController : authorisation

ManagePrescriptionController ..> PrescriptionStorage : persistence

ManagePrescriptionController ..> Prescription : manages
ManagePrescriptionController ..> PrescriptionItem : manages


' =====================================================
' DOMAIN AGGREGATE
' =====================================================

Prescription "1" *-- "0..*" PrescriptionItem : contains >

Prescription --> PrescriptionStatus : current state


' =====================================================
' STORAGE
' =====================================================

PrescriptionStorage ..> Prescription : persists aggregate


note right of Prescription
Aggregate root.

PrescriptionItem does not require
a separate storage class.

Draft prescriptions may temporarily
contain 0 items, but a clinically
usable prescription should require
at least one valid item.
end note


note bottom of PrescriptionItem
medicineId is a reference identifier.

The supplied architecture does not
define a MedicineStorage dependency
for UCD-01, therefore medicine lookup
is intentionally not invented here.
end note


note bottom of PrescriptionStorage
version supports optimistic locking
to prevent one doctor update from
silently overwriting another update.
end note

@enduml