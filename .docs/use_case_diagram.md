```mermaid
flowchart TB

%% =====================================================
%% ACTORS
%% =====================================================

Users["👤 Users"]

Admin["👤 Administrator"]
Doctor["👤 Doctor"]
Pharmacist["👤 Pharmacist"]
Patient["👤 Patient"]

%% Actor generalisation
Admin -->|generalisation| Users
Doctor -->|generalisation| Users
Pharmacist -->|generalisation| Users
Patient -->|generalisation| Users


%% =====================================================
%% PHARMACY MANAGEMENT SYSTEM
%% =====================================================

subgraph PMS["Pharmacy Management System"]

    direction TB

    MUA([Manage User Accounts])

    MP([Manage Prescription])

    UPS(["Update Prescription Status<br/><br/>
    Extension points:<br/>
    • When patient needs it<br/>
    • Amount must match"])

    GR([Generate Reports])

    DM([Dispense Medication])

    SAN(["Send Alerts and Notifications<br/><br/>
    Extension points:<br/>
    • Medical status dispense<br/>
    • Out of inventory"])

    MMI([Manage Medicine Inventory])

    AAU([Authenticate and<br/>Authorise User])

    PROFILE([Manage Profile])

    VPS([View Prescription Status])

end


%% =====================================================
%% ACTOR — USE CASE ASSOCIATIONS
%% =====================================================

Admin --> MUA
Admin --> GR

Doctor --> MP
Doctor --> UPS

Pharmacist --> DM
Pharmacist --> MMI
Pharmacist --> GR

Users --> AAU
Users --> PROFILE

Patient --> VPS


%% =====================================================
%% USE CASE RELATIONSHIPS
%% =====================================================

UPS -.->|«extend»| DM

DM -.->|«include»| MMI

SAN -.->|«extend»| DM

SAN -.->|«extend»| MMI

SAN --> Patient


%% =====================================================
%% STYLING
%% =====================================================

classDef actor fill:#ffffff,stroke:#444,stroke-width:1px;
classDef usecase fill:#ffffff,stroke:#555,stroke-width:1.5px;

class Users,Admin,Doctor,Pharmacist,Patient actor;
class MUA,MP,UPS,GR,DM,SAN,MMI,AAU,PROFILE,VPS usecase;

```

Users
├── Administrator
│   ├── Manage User Accounts
│   └── Generate Reports
│
├── Doctor
│   ├── Manage Prescription
│   └── Update Prescription Status
│
├── Pharmacist
│   ├── Dispense Medication
│   ├── Manage Medicine Inventory
│   └── Generate Reports
│
└── Patient
    └── View Prescription Status

Users
├── Authenticate and Authorise User
└── Manage Profile

Update Prescription Status
    ──«extend»──> Dispense Medication

Send Alerts and Notifications
    ├──«extend»──> Dispense Medication
    └──«extend»──> Manage Medicine Inventory

Dispense Medication
    ──«include»──> Manage Medicine Inventory