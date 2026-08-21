@startuml
title UCD-02 — View Prescription Status
Class Diagram

skinparam classAttributeIconSize 0
skinparam linetype ortho
hide empty members


package "view.patient_information.ucd02_view_prescription_status" {

    class ViewPrescriptionStatusView <<boundary>> {

        -selectedPrescriptionId : long
        -summaries : List<PrescriptionStatusSummary>

        +requestPrescriptionStatus(patientId : long) : void

        +displayPrescriptionList(
            summaries : List<PrescriptionStatusSummary>
        ) : void

        +selectPrescription(
            prescriptionId : long
        ) : void

        +showNoPrescriptions() : void

        +showPrescriptionUnavailable() : void

        +showLoadError(
            message : String
        ) : void
    }


    class PrescriptionStatusDetailsView <<boundary>> {

        -displayedSummary : PrescriptionStatusSummary

        +displayStatusDetails(
            summary : PrescriptionStatusSummary
        ) : void

        +clearDetails() : void
    }
}


package "controller.patient_information" {

    class ViewPrescriptionStatusController <<control>> {

        +requestPrescriptionStatus(
            patientId : long
        ) : List<PrescriptionStatusSummary>

        +viewPrescriptionDetails(
            patientId : long,
            prescriptionId : long
        ) : PrescriptionStatusSummary

        -buildStatusSummary(
            prescription : Prescription,
            dispenseRecord : DispenseRecord
        ) : PrescriptionStatusSummary

        -verifyPatientOwnership(
            patientId : long,
            prescription : Prescription
        ) : boolean
    }
}


package "model.patient_information" {

    class PrescriptionStatusSummary <<entity>> {

        -prescriptionId : long
        -patientId : long
        -doctorId : long

        -prescribedAt : LocalDateTime

        -clinicalStatus : String
        -fulfilmentStatus : String

        -lastUpdatedAt : LocalDateTime

        -hasFulfilmentRecord : boolean

        +buildFromPrescription(
            prescription : Prescription
        ) : void

        +applyFulfilment(
            dispenseRecord : DispenseRecord
        ) : void

        +combineClinicalAndFulfilmentStatus() : void

        +getCurrentDisplayStatus() : String

        +hasFulfilment() : boolean

        +isCancelled() : boolean

        +isDispensed() : boolean
    }
}


package "storage.clinical_prescription" {

    class PrescriptionStorage <<repository>> {

        +findByPatientId(
            patientId : long
        ) : List<Prescription>

        +findById(
            prescriptionId : long
        ) : Prescription
    }
}


package "storage.pharmacy_operations" {

    class DispenseStorage <<repository>> {

        +findFulfilmentByPrescriptionId(
            prescriptionId : long
        ) : DispenseRecord
    }
}


package "model.clinical_prescription" {

    class Prescription <<entity>>
}


package "model.pharmacy_operations" {

    class DispenseRecord <<entity>>
}


' =====================================================
' VIEW
' =====================================================

ViewPrescriptionStatusView *-- PrescriptionStatusDetailsView

ViewPrescriptionStatusView ..> ViewPrescriptionStatusController

PrescriptionStatusDetailsView ..> PrescriptionStatusSummary : displays


' =====================================================
' CONTROLLER
' =====================================================

ViewPrescriptionStatusController ..> PrescriptionStorage : reads
ViewPrescriptionStatusController ..> DispenseStorage : reads

ViewPrescriptionStatusController ..> PrescriptionStatusSummary : builds


' =====================================================
' READ SOURCES
' =====================================================

PrescriptionStorage ..> Prescription : retrieves

DispenseStorage ..> DispenseRecord : retrieves


' =====================================================
' READ MODEL
' =====================================================

PrescriptionStatusSummary ..> Prescription : derives clinical status
PrescriptionStatusSummary ..> DispenseRecord : derives fulfilment status


note right of PrescriptionStatusSummary
Read-only patient-facing model.

It combines:
1. Clinical prescription state
2. Fulfilment state

It does NOT update either source.
end note


note bottom of ViewPrescriptionStatusController
patientId must originate from the
authenticated patient context.

The controller must verify ownership
again when a prescriptionId is selected.

This prevents a patient from reading
another patient's prescription by
changing an identifier.
end note


note bottom of DispenseStorage
No fulfilment record is a valid result.

In that case the summary contains
clinicalStatus only and
hasFulfilmentRecord = false.
end note

@enduml