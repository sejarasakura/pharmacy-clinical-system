@startuml
title UCD-07 — Update Prescription Status
Class Diagram

skinparam classAttributeIconSize 0
skinparam linetype ortho
hide empty members


package "view.clinical_prescription.ucd07_update_prescription_status" {

    class UpdatePrescriptionStatusView <<boundary>> {
        -selectedPrescriptionId : long

        +openStatusManagement() : void

        +displayPrescription(
            prescription : Prescription
        ) : void

        +showCurrentStatus(
            status : PrescriptionStatus
        ) : void

        +showUpdateSuccess(
            status : PrescriptionStatus
        ) : void

        +showPrescriptionNotFound() : void
        +showOperationError(message : String) : void
    }


    class PrescriptionStatusFormView <<boundary>> {
        -selectedStatus : PrescriptionStatus
        -reason : String

        +displayAllowedStatuses(
            statuses : Set<PrescriptionStatus>
        ) : void

        +getSelectedStatus() : PrescriptionStatus
        +getReason() : String

        +submitStatusUpdate() : void

        +showInvalidTransition() : void
        +showConcurrentUpdateError() : void
        +showSaveError() : void
    }
}


package "controller.clinical_prescription" {

    class UpdatePrescriptionStatusController <<control>> {

        +loadPrescription(
            prescriptionId : long
        ) : Prescription

        +getAllowedTransitions(
            prescriptionId : long
        ) : Set<PrescriptionStatus>

        +updateStatus(
            prescriptionId : long,
            targetStatus : PrescriptionStatus,
            reason : String
        ) : boolean

        +issuePrescription(
            prescriptionId : long
        ) : boolean

        +placeOnHold(
            prescriptionId : long,
            reason : String
        ) : boolean

        +cancelPrescription(
            prescriptionId : long,
            reason : String
        ) : boolean

        +resumePrescription(
            prescriptionId : long
        ) : boolean

        -requireDoctorAccess() : long

        -validateTransition(
            currentStatus : PrescriptionStatus,
            targetStatus : PrescriptionStatus
        ) : boolean
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

        -statusChangedAt : LocalDateTime
        -statusChangedBy : long
        -statusChangeReason : String

        -createdAt : LocalDateTime
        -updatedAt : LocalDateTime

        -version : long

        -items : List<PrescriptionItem>

        +getStatus() : PrescriptionStatus

        +canTransitionTo(
            target : PrescriptionStatus
        ) : boolean

        +changeStatus(
            target : PrescriptionStatus,
            changedBy : long,
            reason : String
        ) : void

        +issue(changedBy : long) : void

        +placeOnHold(
            changedBy : long,
            reason : String
        ) : void

        +cancel(
            changedBy : long,
            reason : String
        ) : void

        +resume(changedBy : long) : void

        +isTerminalState() : boolean
    }


    enum PrescriptionStatus {

        DRAFT
        ISSUED
        ON_HOLD
        CANCELLED
        EXPIRED

        +canTransitionTo(
            target : PrescriptionStatus
        ) : boolean

        +allowedTransitions() : Set<PrescriptionStatus>
    }


    class PrescriptionItem <<entity>> {
        -prescriptionItemId : long
        -medicineId : long
        -medicineName : String
        -dosage : String
        -quantity : int
        -frequency : String
        -instructions : String
    }
}


package "storage.clinical_prescription" {

    class PrescriptionStorage <<repository>> {

        +findById(
            prescriptionId : long
        ) : Prescription

        +save(
            prescription : Prescription
        ) : boolean

        +update(
            prescription : Prescription,
            expectedVersion : long
        ) : boolean
    }
}


' =====================================================
' VIEW
' =====================================================

UpdatePrescriptionStatusView *-- PrescriptionStatusFormView

UpdatePrescriptionStatusView ..> UpdatePrescriptionStatusController
PrescriptionStatusFormView ..> UpdatePrescriptionStatusController


' =====================================================
' CONTROLLER
' =====================================================

UpdatePrescriptionStatusController ..> SessionController : authorisation

UpdatePrescriptionStatusController ..> PrescriptionStorage : persistence

UpdatePrescriptionStatusController ..> Prescription : commands
UpdatePrescriptionStatusController ..> PrescriptionStatus : validates


' =====================================================
' DOMAIN
' =====================================================

Prescription --> PrescriptionStatus : current status

Prescription "1" *-- "0..*" PrescriptionItem : contains >


' =====================================================
' STORAGE
' =====================================================

PrescriptionStorage ..> Prescription : persists aggregate


note right of PrescriptionStatus
Recommended transition rules:

DRAFT -> ISSUED
DRAFT -> CANCELLED

ISSUED -> ON_HOLD
ISSUED -> CANCELLED

ON_HOLD -> ISSUED
ON_HOLD -> CANCELLED

CANCELLED -> no transition
EXPIRED -> no transition
end note


note right of Prescription
Status mutation belongs to the
domain object, not directly to
the View or Storage.

The controller coordinates the
transaction and authorisation.
end note


note bottom of PrescriptionStorage
update(... expectedVersion)
implements optimistic concurrency.

This directly supports the
"Concurrent Modification"
exception in the sequence.
end note

@enduml