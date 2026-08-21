@startuml
title UCD-09 — Generate Reports
Class Diagram

skinparam classAttributeIconSize 0
skinparam linetype ortho
hide empty members


package "view.management_dss.ucd09_generate_reports" {

    class GenerateReportsView <<boundary>> {

        -currentReport : Report

        +openReports() : void

        +showCriteriaForm() : void

        +showReport(
            report : Report
        ) : void

        +showGenerationError(
            message : String
        ) : void
    }


    class ReportCriteriaView <<boundary>> {

        -reportType : String
        -startDate : LocalDate
        -endDate : LocalDate
        -filters : Map<String, String>

        +collectCriteria() : ReportCriteria

        +submitCriteria() : void

        +showValidationError(
            errors : List<String>
        ) : void
    }


    class ReportResultView <<boundary>> {

        -displayedReport : Report

        +displayReport(
            report : Report
        ) : void

        +displayNoDataResult() : void

        +requestExport(
            format : String
        ) : void

        +provideExport(
            data : byte[]
        ) : void

        +showExportError() : void
    }
}


package "controller.management_dss" {

    class GenerateReportsController <<control>> {

        +requestReportGeneration() : void

        +generateReport(
            criteria : ReportCriteria
        ) : Report

        +persistReport(
            report : Report
        ) : boolean

        +exportReport(
            report : Report,
            format : String
        ) : byte[]

        -queryReportData(
            criteria : ReportCriteria
        ) : List<Map<String, Object>>

        -hasReportingPermission() : boolean
    }
}


package "model.management_dss" {

    class ReportCriteria <<entity>> {

        -reportType : String

        -startDate : LocalDate
        -endDate : LocalDate

        -filters : Map<String, String>

        +validateCriteria() : List<String>

        +hasValidDateRange() : boolean

        +hasFilter(
            key : String
        ) : boolean

        +getFilter(
            key : String
        ) : String
    }


    class Report <<entity>> {

        -reportId : long
        -reportType : String
        -title : String

        -criteria : ReportCriteria

        -generatedBy : long
        -generatedAt : LocalDateTime

        -rowCount : int

        -data : List<Map<String, Object>>

        +generatePrescriptionReport(
            data : List<Map<String, Object>>
        ) : void

        +generateDispensingReport(
            data : List<Map<String, Object>>
        ) : void

        +generateInventoryReport(
            data : List<Map<String, Object>>
        ) : void

        +generateUserAccessReport(
            data : List<Map<String, Object>>
        ) : void

        +aggregateAndFormatData() : void

        +isEmpty() : boolean

        +generateExport(
            format : String
        ) : byte[]
    }
}


package "storage.management_dss" {

    class ReportStorage <<repository>> {

        +findById(
            reportId : long
        ) : Report

        +save(
            report : Report
        ) : boolean
    }
}


package "storage.clinical_prescription" {

    class PrescriptionStorage <<repository>> {

        +queryForReport(
            startDate : LocalDate,
            endDate : LocalDate,
            filters : Map<String, String>
        ) : List<Map<String, Object>>
    }
}


package "storage.pharmacy_operations" {

    class DispenseStorage <<repository>> {

        +queryForReport(
            startDate : LocalDate,
            endDate : LocalDate,
            filters : Map<String, String>
        ) : List<Map<String, Object>>
    }


    class InventoryStorage <<repository>> {

        +queryForReport(
            filters : Map<String, String>
        ) : List<Map<String, Object>>
    }
}


package "storage.security_user" {

    class UserAccountStorage <<repository>> {

        +queryForReport(
            startDate : LocalDate,
            endDate : LocalDate,
            filters : Map<String, String>
        ) : List<Map<String, Object>>
    }
}


' =====================================================
' VIEW
' =====================================================

GenerateReportsView *-- ReportCriteriaView
GenerateReportsView *-- ReportResultView

GenerateReportsView ..> GenerateReportsController
ReportCriteriaView ..> GenerateReportsController
ReportResultView ..> GenerateReportsController


' =====================================================
' CONTROLLER -> MODEL
' =====================================================

GenerateReportsController ..> ReportCriteria
GenerateReportsController ..> Report


' =====================================================
' CROSS-DOMAIN READ
' =====================================================

GenerateReportsController ..> PrescriptionStorage : <<read>>
GenerateReportsController ..> DispenseStorage : <<read>>
GenerateReportsController ..> InventoryStorage : <<read>>
GenerateReportsController ..> UserAccountStorage : <<read>>


' =====================================================
' OPTIONAL PERSISTENCE
' =====================================================

GenerateReportsController ..> ReportStorage : <<optional>>

ReportStorage ..> Report : persists


' =====================================================
' MODEL
' =====================================================

Report "1" --> "1" ReportCriteria : generated from >


note right of ReportCriteria
reportType remains String.

No ReportType enum is necessary
because the current system may add
report categories without changing
the class structure.
end note


note right of GenerateReportsController
Operational storages are READ ONLY
from UCD-09.

Generate Reports must never change:
- prescriptions
- dispensing records
- inventory
- user accounts
end note


note bottom of ReportStorage
ReportStorage is optional.

If generated reports are not retained,
generate -> display/export directly
without save().
end note

@enduml