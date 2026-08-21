@startuml
title Pharmacy Inventory & Prescription System
UML Package Diagram - Overall Architecture

skinparam packageStyle rectangle
skinparam linetype ortho

package "pharmacy_system" {

    note as MAIN_FILES
    Main.java
    end note

    package "model" as MODEL {

        package "security_user" as M_SU {
            note as M_SU_FILES
            UserAccount.java
            Credential.java
            RolePermission.java
            UserProfile.java
            DoctorProfile.java
            PatientProfile.java
            PharmacyProfile.java
            AdminProfile.java
            end note
        }

        package "clinical_prescription" as M_CP {
            note as M_CP_FILES
            Prescription.java
            PrescriptionItem.java
            PrescriptionStatus.java
            end note
        }

        package "patient_information" as M_PI {
            note as M_PI_FILES
            PrescriptionStatusSummary.java
            Notification.java
            end note
        }

        package "pharmacy_operations" as M_PO {
            note as M_PO_FILES
            DispenseRecord.java
            Medicine.java
            InventoryItem.java
            StockMovement.java
            end note
        }

        package "management_dss" as M_DSS {
            note as M_DSS_FILES
            Report.java
            ReportCriteria.java
            end note
        }
    }

    package "view" as VIEW {

        package "security_user" as V_SU {

            package "ucd04_authenticate_authorise" as V04 {
                note as V04_FILES
                AuthenticateAuthoriseView.java
                LoginFormView.java
                AccessDeniedView.java
                end note
            }

            package "ucd05_manage_profile" as V05 {
                note as V05_FILES
                ManageProfileView.java
                ProfileDetailsView.java
                ProfileFormView.java
                end note
            }

            package "ucd06_manage_user_account" as V06 {
                note as V06_FILES
                ManageUserAccountView.java
                UserAccountListView.java
                UserAccountFormView.java
                end note
            }
        }

        package "clinical_prescription" as V_CP {

            package "ucd01_manage_prescription" as V01 {
                note as V01_FILES
                ManagePrescriptionView.java
                PrescriptionFormView.java
                PrescriptionItemView.java
                end note
            }

            package "ucd07_update_prescription_status" as V07 {
                note as V07_FILES
                UpdatePrescriptionStatusView.java
                PrescriptionStatusFormView.java
                end note
            }
        }

        package "patient_information" as V_PI {

            package "ucd02_view_prescription_status" as V02 {
                note as V02_FILES
                ViewPrescriptionStatusView.java
                PrescriptionStatusDetailsView.java
                end note
            }

            package "ucd03_send_alerts_notifications" as V03 {
                note as V03_FILES
                SendAlertsNotificationsView.java
                NotificationView.java
                end note
            }
        }

        package "pharmacy_operations" as V_PO {

            package "ucd08_dispense_medication" as V08 {
                note as V08_FILES
                DispenseMedicationView.java
                DispenseFormView.java
                DispenseResultView.java
                end note
            }

            package "ucd10_manage_medicine_inventory" as V10 {
                note as V10_FILES
                ManageMedicineInventoryView.java
                MedicineListView.java
                MedicineFormView.java
                StockAdjustmentView.java
                end note
            }
        }

        package "management_dss" as V_DSS {

            package "ucd09_generate_reports" as V09 {
                note as V09_FILES
                GenerateReportsView.java
                ReportCriteriaView.java
                ReportResultView.java
                end note
            }
        }
    }

    package "controller" as CONTROLLER {

        package "common" as C_COMMON {
            note as C_COMMON_FILES
            NavigationController.java
            SessionController.java
            end note
        }

        package "security_user" as C_SU {
            note as C_SU_FILES
            AuthenticateAuthoriseController.java
            ManageProfileController.java
            ManageUserAccountController.java
            end note
        }

        package "clinical_prescription" as C_CP {
            note as C_CP_FILES
            ManagePrescriptionController.java
            UpdatePrescriptionStatusController.java
            end note
        }

        package "patient_information" as C_PI {
            note as C_PI_FILES
            ViewPrescriptionStatusController.java
            SendAlertsNotificationsController.java
            end note
        }

        package "pharmacy_operations" as C_PO {
            note as C_PO_FILES
            DispenseMedicationController.java
            ManageMedicineInventoryController.java
            end note
        }

        package "management_dss" as C_DSS {
            note as C_DSS_FILES
            GenerateReportsController.java
            end note
        }
    }

    package "storage" as STORAGE {

        package "security_user" as S_SU {
            note as S_SU_FILES
            UserAccountStorage.java
            CredentialStorage.java
            RolePermissionStorage.java
            ProfileStorage.java
            end note
        }

        package "clinical_prescription" as S_CP {
            note as S_CP_FILES
            PrescriptionStorage.java
            end note
        }

        package "patient_information" as S_PI {
            note as S_PI_FILES
            NotificationStorage.java
            end note
        }

        package "pharmacy_operations" as S_PO {
            note as S_PO_FILES
            DispenseStorage.java
            MedicineStorage.java
            InventoryStorage.java
            end note
        }

        package "management_dss" as S_DSS {
            note as S_DSS_FILES
            ReportStorage.java
            end note
        }
    }
}


' =====================================================
' GENERAL MVC + STORAGE ARCHITECTURAL DEPENDENCIES
' =====================================================

VIEW ..> CONTROLLER : <<uses>>

CONTROLLER ..> MODEL : <<uses>>

CONTROLLER ..> STORAGE : <<persistence>>

STORAGE ..> MODEL : <<persists>>


' =====================================================
' GENERAL SHARED CONTROLLER CAPABILITY
' =====================================================

CONTROLLER ..> C_COMMON : <<session / navigation>>


' =====================================================
' GENERAL CROSS-DOMAIN DEPENDENCY
' =====================================================

CONTROLLER ..> STORAGE : <<cross-domain read / validation>>

note on link
Controllers may read required information
from another domain's storage.

Examples:
- Patient Information reads prescription
  and fulfilment status
- Pharmacy Operations validates prescription
- Management / DSS reads operational data
end note


' =====================================================
' EVENT-DRIVEN CROSS-DOMAIN COMMUNICATION
' =====================================================

MODEL ..> CONTROLLER : <<domain event>>

note on link
Relevant prescription or fulfilment
state changes may trigger downstream
patient notification processing.
end note

@enduml