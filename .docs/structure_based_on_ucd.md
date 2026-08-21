```
UCD-01 — Manage Prescription
├── Model
│   ├── Prescription.java
│   └── PrescriptionItem.java
├── View
│   ├── ManagePrescriptionView.java
│   ├── PrescriptionFormView.java
│   └── PrescriptionItemView.java
├── Controller
│   └── ManagePrescriptionController.java
└── Storage
    └── PrescriptionStorage.java

```

```
UCD-02 — View Prescription Status
├── Model
│   └── PrescriptionStatusSummary.java
├── View
│   ├── ViewPrescriptionStatusView.java
│   └── PrescriptionStatusDetailsView.java
├── Controller
│   └── ViewPrescriptionStatusController.java
└── Storage
    ├── PrescriptionStorage.java
    └── DispenseStorage.java

```

```
UCD-03 — Send Alerts & Notifications
├── Model
│   └── Notification.java
├── View
│   ├── SendAlertsNotificationsView.java
│   └── NotificationView.java
├── Controller
│   └── SendAlertsNotificationsController.java
└── Storage
    └── NotificationStorage.java

```

```
UCD-04 — Authenticate & Authorise
├── Model
│   ├── UserAccount.java
│   ├── Credential.java
│   └── RolePermission.java
├── View
│   ├── AuthenticateAuthoriseView.java
│   ├── LoginFormView.java
│   └── AccessDeniedView.java
├── Controller
│   └── AuthenticateAuthoriseController.java
└── Storage
    ├── UserAccountStorage.java
    ├── CredentialStorage.java
    └── RolePermissionStorage.java

```

```
UCD-05 — Manage Profile
├── Model
│   ├── UserProfile.java
│   ├── DoctorProfile.java
│   ├── PatientProfile.java
│   ├── PharmacyProfile.java
│   └── AdminProfile.java
├── View
│   ├── ManageProfileView.java
│   ├── ProfileDetailsView.java
│   └── ProfileFormView.java
├── Controller
│   └── ManageProfileController.java
└── Storage
    └── ProfileStorage.java

```

```
UCD-06 — Manage User Account
├── Model
│   ├── UserAccount.java
│   └── RolePermission.java
├── View
│   ├── ManageUserAccountView.java
│   ├── UserAccountListView.java
│   └── UserAccountFormView.java
├── Controller
│   └── ManageUserAccountController.java
└── Storage
    ├── UserAccountStorage.java
    └── RolePermissionStorage.java

```

```
UCD-07 — Update Prescription Status
├── Model
│   ├── Prescription.java
│   └── PrescriptionStatus.java
├── View
│   ├── UpdatePrescriptionStatusView.java
│   └── PrescriptionStatusFormView.java
├── Controller
│   └── UpdatePrescriptionStatusController.java
└── Storage
    └── PrescriptionStorage.java

```

```
UCD-08 — Dispense Medication
├── Model
│   ├── DispenseRecord.java
│   └── InventoryItem.java
├── View
│   ├── DispenseMedicationView.java
│   ├── DispenseFormView.java
│   └── DispenseResultView.java
├── Controller
│   └── DispenseMedicationController.java
└── Storage
    ├── DispenseStorage.java
    └── InventoryStorage.java

```

```
UCD-09 — Generate Reports
├── Model
│   ├── Report.java
│   └── ReportCriteria.java
├── View
│   ├── GenerateReportsView.java
│   ├── ReportCriteriaView.java
│   └── ReportResultView.java
├── Controller
│   └── GenerateReportsController.java
└── Storage
    ├── Existing relevant storages
    └── ReportStorage.java (only if reports are persisted)

```

```
UCD-10 — Manage Medicine Inventory
├── Model
│   ├── Medicine.java
│   ├── InventoryItem.java
│   └── StockMovement.java
├── View
│   ├── ManageMedicineInventoryView.java
│   ├── MedicineListView.java
│   ├── MedicineFormView.java
│   └── StockAdjustmentView.java
├── Controller
│   └── ManageMedicineInventoryController.java
└── Storage
    ├── MedicineStorage.java
    └── InventoryStorage.java
```