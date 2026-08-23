# Consolidated analysis class diagram — full version

This diagram consolidates UCD-01 to UCD-10 into one non-repeating analysis model. It describes business information and responsibilities in human language. Programming-language data types, database structures, framework details and technical method signatures are deliberately excluded.

Stereotypes: `boundary` is a user-system interaction point; `control` coordinates a use case or policy; `entity` is durable business information; `information store` represents the need to retain and retrieve records; and `enumeration` is an approved set of business states.

```mermaid
classDiagram
direction LR

%% SECURITY AND USER MANAGEMENT
class UserAccount["User Account"] {
  <<entity>>
  Account identity
  Login name
  Contact email
  Current account status
  Assigned operational role
  Approval and status history
  +register account
  +approve account
  +disable or reactivate account
  +lock or unlock account
}
class Credential["Login Credential"] {
  <<entity>>
  Account holder
  Protected password record
  Last password change
  Failed sign-in history
  Recovery status
  +verify submitted password
  +replace password
  +begin account recovery
  +invalidate compromised credential
}
class RolePermission["Role and Permission Assignment"] {
  <<entity>>
  Account holder
  Operational role
  Authorised activities
  Assignment status and history
  +assign operational role
  +check permitted activity
  +withdraw access authority
}
class AccountStatus["Account Status"] {
  <<enumeration>>
  Pending approval
  Active
  Disabled
  Locked
}
class UserProfile["User Profile"] {
  <<entity>>
  Account holder
  Full name
  Contact details
  Communication preferences
  Profile completion status
  +maintain personal details
  +choose communication preferences
  +review profile information
}
class AdminProfile["Administrator Profile"] {
  <<entity>>
  Administrative responsibility
  Organisational position
  Account-management authority
  +administer member accounts
  +review access assignments
}
class DoctorProfile["Doctor Profile"] {
  <<entity>>
  Professional registration
  Clinical speciality
  Practice contact details
  Prescribing authority
  +maintain professional details
  +issue prescriptions
}
class PatientProfile["Patient Profile"] {
  <<entity>>
  Patient identity
  Contact details
  Date of birth
  Medication communication preference
  +maintain patient details
  +view personal medication information
}
class PharmacyProfile["Pharmacy Staff Profile"] {
  <<entity>>
  Pharmacy staff identity
  Professional registration
  Dispensing responsibility
  Workplace details
  +maintain professional details
  +perform authorised pharmacy work
}
class AuthenticateAuthoriseController["Authentication and Authorisation Coordination"] {
  <<control>>
  Sign-in request
  Identity verification outcome
  Account eligibility decision
  Access decision
  +verify sign-in details
  +confirm active account
  +determine permitted activities
  +establish or end session
}
class ManageUserAccountController["User Account Management Coordination"] {
  <<control>>
  Account request
  Approval decision
  Role assignment decision
  Status-change reason
  +register new account
  +approve or reject account
  +assign operational role
  +change account status
  +resolve locked account
}
class ManageProfileController["Profile Management Coordination"] {
  <<control>>
  Current profile
  Proposed profile changes
  Restricted-information rules
  Validation outcome
  +present current profile
  +validate proposed changes
  +apply permitted changes
  +reject restricted changes
}
class SessionController["User Session Coordination"] {
  <<control>>
  Signed-in user
  Session validity
  Current role
  Granted permissions
  +confirm authenticated user
  +check required permission
  +renew valid session
  +end session
}
class NavigationController["Role-Based Navigation Coordination"] {
  <<control>>
  Current user role
  Available work areas
  Requested destination
  +show permitted destinations
  +direct user to starting area
  +prevent unauthorised navigation
}
class UserAccountStorage["User Account Information Store"] {
  <<information store>>
  Registered accounts
  Account status history
  Approval history
  +retain account record
  +retrieve account by identity
  +find accounts awaiting approval
  +preserve account changes
}
class CredentialStorage["Credential Information Store"] {
  <<information store>>
  Protected credentials
  Password-change history
  Recovery state
  +retain protected credential
  +retrieve credential for verification
  +replace obsolete credential
}
class RolePermissionStorage["Role and Permission Information Store"] {
  <<information store>>
  Available operational roles
  Role permissions
  Account role assignments
  +retain role assignment
  +retrieve account permissions
  +list permissions for role
}
class ProfileStorage["Profile Information Store"] {
  <<information store>>
  User profiles
  Role-specific profile details
  Profile-change history
  +retain profile
  +retrieve profile for account
  +preserve permitted changes
}
class AuthenticateAuthoriseView["Sign-In and Access Boundary"] {
  <<boundary>>
  Sign-in journey
  Authentication outcome
  Access guidance
  +request sign-in details
  +communicate sign-in outcome
  +direct authorised user
}
class LoginFormView["Login Form Boundary"] {
  <<boundary>>
  Login name entry
  Password entry
  Recovery request
  Validation guidance
  +collect sign-in details
  +request authentication
  +offer password recovery
}
class AccessDeniedView["Access Denied Boundary"] {
  <<boundary>>
  Denied activity
  Reason for denial
  Safe destination
  +explain access restriction
  +offer permitted next action
}
class ManageUserAccountView["Account Management Boundary"] {
  <<boundary>>
  Account-management workspace
  Selected member account
  Current account state
  +present account options
  +request account action
  +communicate action outcome
}
class UserAccountFormView["Account Details Form Boundary"] {
  <<boundary>>
  Member identity details
  Contact details
  Operational role selection
  Validation feedback
  +collect account details
  +submit account request
  +show correction guidance
}
class UserAccountListView["Account List Boundary"] {
  <<boundary>>
  Member account summaries
  Approval and status filters
  Selected account
  +present member accounts
  +filter accounts by status
  +open selected account
}
class ManageProfileView["Profile Management Boundary"] {
  <<boundary>>
  Current personal profile
  Available profile actions
  Change outcome
  +present profile
  +request profile update
  +communicate update outcome
}
class ProfileDetailsView["Profile Details Boundary"] {
  <<boundary>>
  Personal details
  Role-specific details
  Communication preferences
  +display profile information
  +identify editable information
}
class ProfileFormView["Profile Form Boundary"] {
  <<boundary>>
  Proposed personal details
  Proposed contact details
  Proposed preferences
  Validation guidance
  +collect profile changes
  +submit permitted changes
  +show restricted fields
}

%% CLINICAL PRESCRIPTION
class Prescription["Prescription"] {
  <<entity>>
  Prescription reference
  Patient
  Prescribing doctor
  Clinical notes
  Current prescription status
  Creation and update history
  Cancellation details
  +add prescribed medicine
  +amend clinical details
  +remove prescribed medicine
  +issue prescription
  +place prescription on hold
  +cancel prescription
}
class PrescriptionItem["Prescription Item"] {
  <<entity>>
  Prescribed medicine
  Dose and dose unit
  Quantity prescribed
  Administration frequency
  Route of administration
  Treatment duration
  Patient instructions
  +validate medication instruction
  +revise dose
  +revise quantity
  +revise administration instructions
}
class PrescriptionStatus["Prescription Status"] {
  <<enumeration>>
  Draft
  Issued
  On hold
  Cancelled
  Expired
}
class ManagePrescriptionController["Prescription Management Coordination"] {
  <<control>>
  Selected patient
  Working prescription
  Clinical validation outcome
  Prescribing authority
  +begin prescription
  +add or amend medication item
  +validate clinical content
  +save prescription
  +cancel eligible prescription
}
class UpdatePrescriptionStatusController["Prescription Status Coordination"] {
  <<control>>
  Current prescription status
  Requested new status
  Transition reason
  Authorised user
  +present permitted status changes
  +validate requested transition
  +record status change
  +reject invalid transition
}
class PrescriptionStorage["Prescription Information Store"] {
  <<information store>>
  Prescriptions
  Prescription items
  Status and amendment history
  +retain prescription
  +retrieve prescription by reference
  +find prescriptions for doctor
  +find prescriptions for patient
}
class ManagePrescriptionView["Prescription Management Boundary"] {
  <<boundary>>
  Doctor prescription workspace
  Selected prescription
  Prescription summaries
  Operation outcome
  +present prescriptions
  +start new prescription
  +request amendment or cancellation
  +communicate outcome
}
class PrescriptionFormView["Prescription Form Boundary"] {
  <<boundary>>
  Selected patient
  Clinical notes
  Prescribed medicine entries
  Validation guidance
  +collect prescription details
  +add medication entry
  +submit prescription
  +show clinical corrections
}
class PrescriptionItemView["Prescription Item Boundary"] {
  <<boundary>>
  Selected medicine
  Dose instruction
  Quantity and frequency
  Route and duration
  Patient instruction
  +collect medication instruction
  +request item amendment
  +show medication-entry errors
}
class UpdatePrescriptionStatusView["Prescription Status Management Boundary"] {
  <<boundary>>
  Prescription summary
  Current status
  Permitted next statuses
  +present status position
  +request status change
  +communicate transition outcome
}
class PrescriptionStatusFormView["Prescription Status Form Boundary"] {
  <<boundary>>
  Current status
  Selected new status
  Reason for change
  +collect status-change decision
  +submit status change
  +show invalid-transition guidance
}

%% PATIENT INFORMATION AND NOTIFICATIONS
class PrescriptionStatusSummary["Prescription Status Summary"] {
  <<entity>>
  Prescription reference
  Medicine summary
  Current clinical status
  Fulfilment status
  Collection readiness
  Latest update
  +combine clinical and dispensing position
  +indicate collection readiness
  +present patient-appropriate summary
}
class Notification["Patient Notification"] {
  <<entity>>
  Notification reference
  Intended recipient
  Message subject and content
  Delivery time
  Read status
  Related prescription
  +prepare notification
  +mark as delivered
  +mark as read
}
class ViewPrescriptionStatusController["Patient Prescription Status Coordination"] {
  <<control>>
  Signed-in patient
  Accessible prescriptions
  Clinical status information
  Dispensing information
  +find patient prescriptions
  +combine clinical and supply status
  +prepare status summary
  +protect another patient's information
}
class SendAlertsNotificationsController["Alert and Notification Coordination"] {
  <<control>>
  Notification event
  Intended recipient
  Message purpose
  Delivery outcome
  +create relevant notification
  +send notification to patient
  +list recipient notifications
  +record notification as read
}
class NotificationStorage["Notification Information Store"] {
  <<information store>>
  Patient notifications
  Delivery status
  Read status
  +retain notification
  +retrieve recipient notifications
  +preserve delivery and read changes
}
class ViewPrescriptionStatusView["Patient Prescription Status Boundary"] {
  <<boundary>>
  Patient prescription list
  Selected prescription
  Current fulfilment position
  +present patient prescriptions
  +open prescription status
  +explain current position
}
class PrescriptionStatusDetailsView["Prescription Status Details Boundary"] {
  <<boundary>>
  Prescription summary
  Clinical status
  Dispensing status
  Collection guidance
  +display combined status
  +present latest update
  +show appropriate next step
}
class SendAlertsNotificationsView["Notification Centre Boundary"] {
  <<boundary>>
  Notification list
  Unread notification count
  Selected notification
  +present notifications
  +filter unread notifications
  +open selected notification
}
class NotificationView["Notification Details Boundary"] {
  <<boundary>>
  Notification subject
  Message content
  Delivery time
  Related prescription
  +display notification
  +request mark as read
  +open related information
}

%% PHARMACY OPERATIONS
class Medicine["Medicine"] {
  <<entity>>
  Medicine reference
  Medicine name and generic name
  Strength description
  Dosage form
  Supplier details
  Availability status
  +maintain medicine details
  +activate medicine
  +discontinue medicine
}
class InventoryItem["Inventory Item"] {
  <<entity>>
  Stocked medicine
  Quantity currently available
  Reorder threshold
  Stock availability position
  Last stock review
  +receive stock
  +reserve stock for supply
  +reduce stock after supply
  +identify low-stock condition
}
class StockMovement["Stock Movement"] {
  <<entity>>
  Movement reference
  Inventory item
  Movement category
  Quantity changed
  Reason for movement
  Responsible staff member
  Date and time
  +record stock receipt
  +record stock issue
  +record authorised adjustment
}
class StockMovementType["Stock Movement Type"] {
  <<enumeration>>
  Stock received
  Stock issued
  Stock adjusted
}
class DispenseRecord["Dispense Record"] {
  <<entity>>
  Dispensing reference
  Prescription fulfilled
  Medicines and quantities supplied
  Responsible pharmacist
  Supply date and time
  Dispensing outcome
  +confirm prescription checks
  +record medicine supply
  +record partial fulfilment
  +record non-supply reason
}
class ManageMedicineInventoryController["Medicine and Inventory Coordination"] {
  <<control>>
  Medicine catalogue request
  Current stock position
  Proposed stock movement
  Stock validation outcome
  +register medicine
  +amend medicine details
  +record stock receipt or adjustment
  +prevent invalid stock reduction
  +identify low-stock medicines
}
class DispenseMedicationController["Medication Dispensing Coordination"] {
  <<control>>
  Presented prescription
  Prescription eligibility
  Required medicine quantities
  Available stock
  Dispensing outcome
  +verify prescription is dispensable
  +confirm medicine availability
  +authorise medicine supply
  +reduce supplied stock
  +record dispensing outcome
}
class MedicineStorage["Medicine Information Store"] {
  <<information store>>
  Medicine catalogue
  Availability status
  Medicine-detail history
  +retain medicine record
  +retrieve medicine by reference
  +find medicine by name
  +preserve catalogue changes
}
class InventoryStorage["Inventory Information Store"] {
  <<information store>>
  Current stock positions
  Reorder thresholds
  Stock movement history
  +retain inventory position
  +retrieve stock for medicine
  +retain stock movement
  +find low-stock items
}
class DispenseStorage["Dispensing Information Store"] {
  <<information store>>
  Dispensing records
  Prescription fulfilment history
  Pharmacist activity history
  +retain dispensing record
  +retrieve prescription supply history
  +retrieve pharmacist activity
}
class ManageMedicineInventoryView["Medicine and Inventory Boundary"] {
  <<boundary>>
  Medicine catalogue
  Stock summaries
  Selected medicine
  Inventory operation outcome
  +present medicines and stock
  +request medicine maintenance
  +request stock action
  +communicate operation outcome
}
class MedicineFormView["Medicine Form Boundary"] {
  <<boundary>>
  Medicine identity details
  Strength and dosage form
  Supplier details
  Availability choice
  Validation guidance
  +collect medicine details
  +submit catalogue change
  +show required corrections
}
class MedicineListView["Medicine List Boundary"] {
  <<boundary>>
  Medicine summaries
  Stock availability indicators
  Search and filter criteria
  Selected medicine
  +present medicine catalogue
  +search or filter medicines
  +open selected medicine
}
class StockAdjustmentView["Stock Adjustment Boundary"] {
  <<boundary>>
  Selected medicine
  Current stock position
  Adjustment quantity
  Adjustment reason
  +collect stock adjustment
  +show projected stock position
  +submit authorised adjustment
}
class DispenseMedicationView["Medication Dispensing Boundary"] {
  <<boundary>>
  Dispensing queue
  Selected prescription
  Verification outcome
  Supply outcome
  +present prescriptions awaiting supply
  +begin dispensing check
  +confirm or stop supply
  +communicate dispensing outcome
}
class DispenseFormView["Dispensing Verification Boundary"] {
  <<boundary>>
  Patient and prescription summary
  Prescribed medicine details
  Stock availability
  Pharmacist confirmation
  +present required checks
  +collect dispensing confirmation
  +submit supply decision
}
class DispenseResultView["Dispensing Result Boundary"] {
  <<boundary>>
  Dispensing outcome
  Medicines and quantities supplied
  Remaining stock position
  Follow-up guidance
  +present supply confirmation
  +explain unsuccessful supply
  +offer appropriate next action
}

%% MANAGEMENT DECISION SUPPORT
class Report["Management Report"] {
  <<entity>>
  Report purpose
  Reporting period
  Selected operational measures
  Findings and totals
  Generation date
  Intended recipient
  +summarise operational performance
  +present report findings
  +prepare report for export
}
class ReportCriteria["Report Criteria"] {
  <<entity>>
  Report category
  Reporting period
  Selected filters
  Grouping choice
  +define report scope
  +validate reporting period
  +confirm required criteria
}
class GenerateReportsController["Report Generation Coordination"] {
  <<control>>
  Report request
  Validated criteria
  Relevant operational information
  Generated findings
  +validate report request
  +gather authorised information
  +calculate report measures
  +prepare report
  +request report export
}
class ReportStorage["Report Information Store"] {
  <<information store>>
  Generated reports
  Report criteria
  Generation history
  +retain generated report
  +retrieve previous report
  +list reports by period or category
}
class GenerateReportsView["Report Generation Boundary"] {
  <<boundary>>
  Reporting workspace
  Available report categories
  Current report result
  +present reporting choices
  +request report generation
  +present or export result
}
class ReportCriteriaView["Report Criteria Boundary"] {
  <<boundary>>
  Report category choice
  Reporting period
  Available filters
  Validation guidance
  +collect reporting criteria
  +submit report request
  +show criteria corrections
}
class ReportResultView["Report Result Boundary"] {
  <<boundary>>
  Report heading and period
  Operational findings
  Totals and summaries
  Export choices
  +display report findings
  +request report export
  +return to reporting criteria
}

%% CORE BUSINESS STRUCTURE
UserProfile <|-- AdminProfile : specialises
UserProfile <|-- DoctorProfile : specialises
UserProfile <|-- PatientProfile : specialises
UserProfile <|-- PharmacyProfile : specialises
UserAccount "1" *-- "1" Credential : secures
UserAccount "1" -- "1" RolePermission : receives authority through
UserAccount "1" -- "1" UserProfile : represents
UserAccount --> AccountStatus : has current state
DoctorProfile "1" -- "0..*" Prescription : issues
PatientProfile "1" -- "0..*" Prescription : receives
Prescription "1" *-- "1..*" PrescriptionItem : contains
PrescriptionItem "0..*" --> "1" Medicine : specifies
Prescription --> PrescriptionStatus : has current state
Medicine "1" -- "0..1" InventoryItem : has stock position
InventoryItem "1" *-- "0..*" StockMovement : records changes through
StockMovement --> StockMovementType : is classified as
Prescription "1" -- "0..*" DispenseRecord : is fulfilled through
DispenseRecord "0..*" --> "1" PharmacyProfile : performed by
DispenseRecord "0..*" --> "1..*" PrescriptionItem : supplies
PrescriptionStatusSummary --> Prescription : summarises clinical state
PrescriptionStatusSummary ..> DispenseRecord : reflects fulfilment
PatientProfile "1" -- "0..*" Notification : receives
Notification "0..*" --> "0..1" Prescription : may concern
Report --> ReportCriteria : follows
Report ..> Prescription : analyses
Report ..> InventoryItem : analyses
Report ..> DispenseRecord : analyses

%% INFORMATION RETENTION
UserAccountStorage ..> UserAccount : retains
CredentialStorage ..> Credential : retains
RolePermissionStorage ..> RolePermission : retains
ProfileStorage ..> UserProfile : retains
PrescriptionStorage ..> Prescription : retains
NotificationStorage ..> Notification : retains
MedicineStorage ..> Medicine : retains
InventoryStorage ..> InventoryItem : retains
InventoryStorage ..> StockMovement : retains
DispenseStorage ..> DispenseRecord : retains
ReportStorage ..> Report : retains

%% USE-CASE COORDINATION
AuthenticateAuthoriseController ..> UserAccountStorage : checks account
AuthenticateAuthoriseController ..> CredentialStorage : verifies credential
AuthenticateAuthoriseController ..> RolePermissionStorage : determines authority
AuthenticateAuthoriseController ..> SessionController : establishes session
AuthenticateAuthoriseController ..> NavigationController : selects destination
ManageUserAccountController ..> UserAccount : manages lifecycle
ManageUserAccountController ..> RolePermission : manages authority
ManageUserAccountController ..> UserAccountStorage : retains account
ManageUserAccountController ..> RolePermissionStorage : retains assignment
ManageUserAccountController ..> SessionController : confirms administrator
ManageProfileController ..> UserProfile : manages details
ManageProfileController ..> ProfileStorage : retains profile
ManageProfileController ..> SessionController : identifies account holder
ManagePrescriptionController ..> Prescription : manages clinical record
ManagePrescriptionController ..> PrescriptionItem : manages medication instruction
ManagePrescriptionController ..> PrescriptionStorage : retains prescription
ManagePrescriptionController ..> SessionController : confirms prescribing authority
UpdatePrescriptionStatusController ..> Prescription : changes lifecycle state
UpdatePrescriptionStatusController ..> PrescriptionStatus : validates transition
UpdatePrescriptionStatusController ..> PrescriptionStorage : preserves change
UpdatePrescriptionStatusController ..> SessionController : confirms authority
ViewPrescriptionStatusController ..> PrescriptionStorage : retrieves prescription
ViewPrescriptionStatusController ..> DispenseStorage : retrieves fulfilment
ViewPrescriptionStatusController ..> PrescriptionStatusSummary : prepares summary
ViewPrescriptionStatusController ..> SessionController : identifies patient
SendAlertsNotificationsController ..> Notification : prepares message
SendAlertsNotificationsController ..> NotificationStorage : retains notification
SendAlertsNotificationsController ..> Prescription : identifies event
ManageMedicineInventoryController ..> Medicine : manages catalogue concept
ManageMedicineInventoryController ..> InventoryItem : manages stock position
ManageMedicineInventoryController ..> StockMovement : records stock change
ManageMedicineInventoryController ..> MedicineStorage : retains medicine
ManageMedicineInventoryController ..> InventoryStorage : retains stock history
DispenseMedicationController ..> Prescription : verifies eligibility
DispenseMedicationController ..> InventoryItem : confirms availability
DispenseMedicationController ..> DispenseRecord : records supply
DispenseMedicationController ..> PrescriptionStorage : retrieves prescription
DispenseMedicationController ..> InventoryStorage : updates stock position
DispenseMedicationController ..> DispenseStorage : retains outcome
DispenseMedicationController ..> SessionController : confirms pharmacist
GenerateReportsController ..> ReportCriteria : interprets request
GenerateReportsController ..> Report : prepares findings
GenerateReportsController ..> UserAccountStorage : reads account activity
GenerateReportsController ..> PrescriptionStorage : reads prescribing activity
GenerateReportsController ..> InventoryStorage : reads stock activity
GenerateReportsController ..> DispenseStorage : reads supply activity
GenerateReportsController ..> ReportStorage : retains report
GenerateReportsController ..> SessionController : confirms authority

%% BOUNDARY COMPOSITION AND DELEGATION
AuthenticateAuthoriseView *-- LoginFormView : contains
AuthenticateAuthoriseView *-- AccessDeniedView : contains
AuthenticateAuthoriseView ..> AuthenticateAuthoriseController : requests access decision
LoginFormView ..> AuthenticateAuthoriseController : submits sign-in request
ManageUserAccountView *-- UserAccountFormView : contains
ManageUserAccountView *-- UserAccountListView : contains
ManageUserAccountView ..> ManageUserAccountController : requests account action
UserAccountFormView ..> ManageUserAccountController : submits account details
UserAccountListView ..> ManageUserAccountController : requests account list
ManageProfileView *-- ProfileDetailsView : contains
ManageProfileView *-- ProfileFormView : contains
ManageProfileView ..> ManageProfileController : requests profile action
ProfileFormView ..> ManageProfileController : submits profile changes
ManagePrescriptionView *-- PrescriptionFormView : contains
PrescriptionFormView *-- PrescriptionItemView : contains
ManagePrescriptionView ..> ManagePrescriptionController : requests prescription action
PrescriptionFormView ..> ManagePrescriptionController : submits prescription
PrescriptionItemView ..> ManagePrescriptionController : submits medication item
UpdatePrescriptionStatusView *-- PrescriptionStatusFormView : contains
UpdatePrescriptionStatusView ..> UpdatePrescriptionStatusController : requests status action
PrescriptionStatusFormView ..> UpdatePrescriptionStatusController : submits transition
ViewPrescriptionStatusView *-- PrescriptionStatusDetailsView : contains
ViewPrescriptionStatusView ..> ViewPrescriptionStatusController : requests patient status
PrescriptionStatusDetailsView ..> PrescriptionStatusSummary : displays
SendAlertsNotificationsView *-- NotificationView : contains
SendAlertsNotificationsView ..> SendAlertsNotificationsController : requests notifications
NotificationView ..> SendAlertsNotificationsController : requests read update
ManageMedicineInventoryView *-- MedicineFormView : contains
ManageMedicineInventoryView *-- MedicineListView : contains
ManageMedicineInventoryView *-- StockAdjustmentView : contains
ManageMedicineInventoryView ..> ManageMedicineInventoryController : requests inventory action
MedicineFormView ..> ManageMedicineInventoryController : submits medicine details
MedicineListView ..> ManageMedicineInventoryController : requests catalogue
StockAdjustmentView ..> ManageMedicineInventoryController : submits stock change
DispenseMedicationView *-- DispenseFormView : contains
DispenseMedicationView *-- DispenseResultView : contains
DispenseMedicationView ..> DispenseMedicationController : requests dispensing action
DispenseFormView ..> DispenseMedicationController : submits supply decision
GenerateReportsView *-- ReportCriteriaView : contains
GenerateReportsView *-- ReportResultView : contains
GenerateReportsView ..> GenerateReportsController : requests report
ReportCriteriaView ..> GenerateReportsController : submits criteria
ReportResultView ..> GenerateReportsController : requests export
```

## Reading the model

The diagram combines business entities, use-case controls and interaction boundaries. Information-store classes express only the need to retain and retrieve business records; they do not prescribe a database product, table design, file format or programming interface.

The model remains traceable to the original ten use-case diagrams while removing duplicate declarations of shared classes such as `SessionController`, `Prescription`, `PrescriptionStorage`, `UserAccountStorage`, `InventoryStorage` and `DispenseStorage`.
