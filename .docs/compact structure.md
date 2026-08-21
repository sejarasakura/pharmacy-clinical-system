src/main/java/pharmacy_system/
├── Main.java
├── model/
│   ├── security_user/
│   │   ├── UserAccount.java
│   │   ├── Credential.java
│   │   ├── RolePermission.java
│   │   └── profile/
│   │       ├── UserProfile.java
│   │       ├── DoctorProfile.java
│   │       ├── PatientProfile.java
│   │       ├── PharmacyProfile.java
│   │       └── AdminProfile.java
│   ├── clinical_prescription/
│   │   ├── Prescription.java
│   │   ├── PrescriptionItem.java
│   │   └── PrescriptionStatus.java
│   ├── patient_information/
│   │   ├── PrescriptionStatusSummary.java
│   │   └── Notification.java
│   ├── pharmacy_operations/
│   │   ├── DispenseRecord.java
│   │   ├── Medicine.java
│   │   ├── InventoryItem.java
│   │   └── StockMovement.java
│   └── management_dss/
│       ├── Report.java
│       └── ReportCriteria.java
│
├── view/
│   ├── security_user/
│   │   ├── ucd04_authenticate_authorise/
│   │   │   ├── AuthenticateAuthoriseView.java
│   │   │   └── components/
│   │   │       ├── LoginFormView.java
│   │   │       └── AccessDeniedView.java
│   │   ├── ucd05_manage_profile/
│   │   │   ├── ManageProfileView.java
│   │   │   └── components/
│   │   │       ├── ProfileDetailsView.java
│   │   │       └── ProfileFormView.java
│   │   └── ucd06_manage_user_account/
│   │       ├── ManageUserAccountView.java
│   │       └── components/
│   │           ├── UserAccountListView.java
│   │           └── UserAccountFormView.java
│   ├── clinical_prescription/
│   │   ├── ucd01_manage_prescription/
│   │   │   ├── ManagePrescriptionView.java
│   │   │   └── components/
│   │   │       ├── PrescriptionFormView.java
│   │   │       └── PrescriptionItemView.java
│   │   └── ucd07_update_prescription_status/
│   │       ├── UpdatePrescriptionStatusView.java
│   │       └── components/
│   │           └── PrescriptionStatusFormView.java
│   ├── patient_information/
│   │   ├── ucd02_view_prescription_status/
│   │   │   ├── ViewPrescriptionStatusView.java
│   │   │   └── components/
│   │   │       └── PrescriptionStatusDetailsView.java
│   │   └── ucd03_send_alerts_notifications/
│   │       ├── SendAlertsNotificationsView.java
│   │       └── components/
│   │           └── NotificationView.java
│   ├── pharmacy_operations/
│   │   ├── ucd08_dispense_medication/
│   │   │   ├── DispenseMedicationView.java
│   │   │   └── components/
│   │   │       ├── DispenseFormView.java
│   │   │       └── DispenseResultView.java
│   │   └── ucd10_manage_medicine_inventory/
│   │       ├── ManageMedicineInventoryView.java
│   │       └── components/
│   │           ├── MedicineListView.java
│   │           ├── MedicineFormView.java
│   │           └── StockAdjustmentView.java
│   └── management_dss/
│       └── ucd09_generate_reports/
│           ├── GenerateReportsView.java
│           └── components/
│               ├── ReportCriteriaView.java
│               └── ReportResultView.java
│
├── controller/
│   ├── common/
│   │   ├── NavigationController.java
│   │   └── SessionController.java
│   ├── security_user/
│   │   ├── AuthenticateAuthoriseController.java
│   │   ├── ManageProfileController.java
│   │   └── ManageUserAccountController.java
│   ├── clinical_prescription/
│   │   ├── ManagePrescriptionController.java
│   │   └── UpdatePrescriptionStatusController.java
│   ├── patient_information/
│   │   ├── ViewPrescriptionStatusController.java
│   │   └── SendAlertsNotificationsController.java
│   ├── pharmacy_operations/
│   │   ├── DispenseMedicationController.java
│   │   └── ManageMedicineInventoryController.java
│   └── management_dss/
│       └── GenerateReportsController.java
│
└── storage/
    ├── security_user/
    │   ├── UserAccountStorage.java
    │   ├── CredentialStorage.java
    │   ├── RolePermissionStorage.java
    │   └── ProfileStorage.java
    ├── clinical_prescription/
    │   └── PrescriptionStorage.java
    ├── patient_information/
    │   └── NotificationStorage.java
    ├── pharmacy_operations/
    │   ├── DispenseStorage.java
    │   ├── MedicineStorage.java
    │   └── InventoryStorage.java
    └── management_dss/
        └── ReportStorage.java