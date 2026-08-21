# Requirements Document

## Introduction

The Pharmacy Inventory & Prescription System is a single-organisation Java desktop application (MVC + Storage architecture) that digitises the core clinical and pharmacy workflows for four operational roles: Patient, Doctor, Pharmacist, and Administrator. The system authenticates and authorises users, lets Doctors author and manage clinical prescriptions, lets Pharmacists dispense medication and maintain medicine inventory, gives Patients read-only visibility of their prescription and fulfilment progress with persisted notifications, and lets Administrators manage user accounts and generate persisted operational reports.

These requirements are derived from the approved design document (`design.md`) and the authoritative source specification (`spec.md`, functional requirements FR-001..FR-060 and success criteria SC-001..SC-018). Each requirement below corresponds to one of the ten user stories in the source specification, numbered 1..10 to preserve traceability with the correctness properties in the design document, which reference acceptance criteria using the `**Validates: Requirements X.Y**` notation.

Scope boundaries carried from the source specification: the system supports a single organisational context; each user has exactly one operational role; only the four named roles are in scope; partial dispensing, refills, and repeat dispensing are out of scope; cancelled prescriptions are not restored; in-system notification is the only delivery channel; reporting is Administrator-facing. Persistence technology, UI toolkit, PDF-generation library, and JSON serialization mechanism are deferred to planning.

## Glossary

- **System**: The Pharmacy Inventory & Prescription System as a whole.
- **Authentication_Service**: The component that verifies user credentials and establishes authenticated sessions.
- **Authorisation_Service**: The component that evaluates role-based permissions for protected functions.
- **Session**: An authenticated user context that expires one hour after establishment.
- **User_Account**: The login identity, assigned operational role, and account status for a system user.
- **Credential**: The authentication information (salted password hash and security state) associated with a User_Account.
- **Operational_Role**: One of exactly four roles: Patient, Doctor, Pharmacist, Administrator.
- **User_Profile**: The self-service personal information a user may view and update.
- **Patient_Business_Record**: Patient information used for clinical operations; may exist without a linked User_Account.
- **Account_Manager**: The administrative function (used by an Administrator) that creates and manages User_Accounts and role assignment.
- **Prescription**: One clinical prescription associated with a Patient and a Doctor, containing one or more Prescription_Items.
- **Prescription_Item**: One medication entry within a Prescription (medicine, dosage, quantity, frequency, instructions).
- **Prescription_Status**: The clinical lifecycle state of a Prescription (DRAFT, ISSUED, ON_HOLD, CANCELLED, EXPIRED).
- **Fulfilment_Status**: The pharmacy dispensing progress state, distinct from Prescription_Status.
- **Prescription_Status_Summary**: The Patient-facing read-only view combining authorised clinical and fulfilment status.
- **Dispense_Record**: The pharmacy fulfilment transaction record for an eligible Prescription.
- **Medicine**: Medicine master information, independent of stock quantity.
- **Inventory_Batch**: A stock position for a Medicine batch, including quantity on hand and expiry information.
- **Stock_Movement**: A recorded stock receipt or adjustment activity and its reason where applicable.
- **Notification**: A persisted Patient-facing notification with delivery outcome and read state.
- **Report**: A generated historical operational report snapshot.
- **Report_Criteria**: The criteria used to generate a Report.
- **FEFO**: First-Expiry-First-Out allocation; eligible stock is consumed from the earliest-expiring batch first.
- **Eligible_Stock**: Non-expired stock available for dispensing (expired stock contributes zero quantity).

## Requirements

### Requirement 1: Authenticate and Access Permitted Functions

**User Story:** As a system user, I want to sign in and access only the functions permitted for my role, so that the system remains simple, secure, and relevant to my responsibilities.

#### Acceptance Criteria

1. WHEN a user submits credentials that match an existing active account, THE Authentication_Service SHALL establish an authenticated Session and grant access to the set of functions permitted for the account's assigned Operational_Role (one of Patient, Doctor, Pharmacist, or Administrator).
2. IF a user submits credentials that do not match an existing active account, THEN THE Authentication_Service SHALL deny access, leave no Session established, and return a generic authentication error that does not indicate whether the username or the password was incorrect.
3. IF an authenticated user attempts to access a protected function for which the account's assigned Operational_Role lacks permission, THEN THE Authorisation_Service SHALL deny execution of the function, leave application state unchanged, and return an authorisation error indicating that the function is not permitted for the assigned role.
4. IF a user attempts to use a protected function while the Session is older than one hour, THEN THE System SHALL deny the function and require authentication again.
5. IF a submitted new or changed password contains fewer than eight characters, THEN THE System SHALL reject the password.

### Requirement 2: Manage User Accounts and Profiles

**User Story:** As an Administrator, I want to create and manage user accounts and access roles while each authenticated user maintains permitted personal information, so that account administration and self-service profile management remain clearly separated.

#### Acceptance Criteria

1. WHEN an authorised Administrator submits a User_Account with a unique account identifier, all mandatory identifying fields populated, and exactly one supported Operational_Role assigned, THE Account_Manager SHALL persist the User_Account and make it available for subsequent authentication.
2. WHEN an authorised Administrator disables an existing User_Account, THE Account_Manager SHALL set the User_Account status to disabled and reject every subsequent authentication attempt for that User_Account until it is re-enabled.
3. WHEN an authenticated user submits changes limited to permitted personal profile fields and every submitted value passes field validation, THE System SHALL persist the updated User_Profile and retain all non-submitted fields unchanged.
4. IF an authenticated user attempts, through self-service profile management, to modify Operational_Role, account status, privileged permissions, or system-owned identifiers, THEN THE System SHALL reject the entire change, leave all targeted fields unchanged, and return an indication that the fields are not user-modifiable.
5. WHEN an authorised Administrator provisions login access for an existing Patient_Business_Record that is not already linked to a User_Account, THE Account_Manager SHALL create one User_Account, link it to the existing Patient_Business_Record, and SHALL NOT create an additional Patient_Business_Record.

### Requirement 3: Maintain Patient Business Records

**User Story:** As a Doctor, I want to create the Patient business record required for clinical work even when the Patient does not yet have login access, so that treatment records can be created without requiring prior portal registration.

#### Acceptance Criteria

1. WHEN an authorised Doctor submits a Patient_Business_Record containing valid values for all mandatory fields (Patient identifier and full name), THE System SHALL store the Patient_Business_Record without a linked User_Account and confirm successful creation to the Doctor.
2. WHERE a Patient_Business_Record has no linked User_Account, WHEN the Patient is used in an authorised prescription workflow, THE System SHALL allow the clinical workflow to proceed without requiring login access.
3. IF a Doctor attempts to create, modify, or delete a Patient's login credentials or account permissions, THEN THE System SHALL reject the action, leave any existing credentials and permissions unchanged, and return an error indicating that credential administration is not permitted for the Doctor role.
4. THE System SHALL support the following Patient_Business_Record fields, each text field limited to a maximum of 255 characters: Patient identifier (mandatory), full name (mandatory), email (optional), phone number (optional), date of birth (optional), address (optional), and emergency contact (optional).
5. IF a Doctor submits a Patient_Business_Record that omits a mandatory field, or contains an email not matching the form local@domain, a phone number outside 7 to 15 digits, a date of birth later than the current date, or any text field exceeding 255 characters, THEN THE System SHALL reject the submission, indicate which field failed validation, and SHALL NOT create or partially store the Patient_Business_Record.

### Requirement 4: Manage Prescriptions

**User Story:** As a Doctor, I want to create, search, view, modify, and cancel prescriptions containing one or more medication items, so that clinical prescription records can be maintained digitally.

#### Acceptance Criteria

1. WHEN an authorised Doctor submits prescription information for an existing Patient that contains between 1 and 50 Prescription_Items and has all mandatory prescription and item fields populated, THE System SHALL store the Prescription together with its Prescription_Items.
2. WHEN a Doctor submits modifications to a Prescription that is not in the CANCELLED or EXPIRED state, and the modified Prescription still contains between 1 and 50 Prescription_Items with all mandatory prescription and item fields populated, THE System SHALL store the updated Prescription.
3. WHEN a user views a Prescription that is in the CANCELLED state, THE System SHALL retain the CANCELLED status and SHALL NOT provide any workflow action that changes the Prescription to another status.
4. WHILE a Prescription is in the DRAFT state, THE System SHALL exclude the Prescription from the Patient's prescription list.
5. WHEN a Prescription in the ISSUED state whose issue date is more than one calendar month before the current date is displayed or evaluated, THE System SHALL identify the Prescription as EXPIRED.
6. IF a Doctor submits prescription information for a Patient that does not exist, or with fewer than 1 or more than 50 Prescription_Items, or with any mandatory prescription or item field missing, THEN THE System SHALL reject the submission, SHALL NOT store any new Prescription record, and SHALL display an error message indicating the reason for rejection.
7. IF a Doctor submits modifications to a Prescription that is in the CANCELLED or EXPIRED state, THEN THE System SHALL reject the modification, SHALL retain the existing Prescription unchanged, and SHALL display an error message indicating that the Prescription cannot be edited.

### Requirement 5: Manage Clinical Prescription Status

**User Story:** As a Doctor, I want to change a prescription's clinical lifecycle state using only permitted transitions, so that the clinical status remains valid and understandable.

#### Acceptance Criteria

1. WHEN a Doctor selects a target Prescription_Status that is an allowed transition from the Prescription's current clinical status (DRAFT→ISSUED|CANCELLED; ISSUED→ON_HOLD|CANCELLED; ON_HOLD→ISSUED|CANCELLED), THE System SHALL update the Prescription's clinical status to the selected target status and persist the updated status to Storage.
2. IF a status change to a target Prescription_Status that requires a reason is attempted and the Doctor does not provide a reason containing at least one non-whitespace character, THEN THE System SHALL reject the transition, retain the current clinical status unchanged, and display an error message indicating that a reason is required.
3. IF a user whose role is not Doctor attempts to change a Prescription's clinical status to ON_HOLD, THEN THE System SHALL reject the action, retain the current clinical status unchanged, and display an error message indicating that the action requires the Doctor role.
4. WHEN a Prescription whose clinical status is EXPIRED is evaluated for fulfilment, THE System SHALL classify the Prescription as clinically ineligible and prevent any fulfilment action against it, without altering its clinical status.
5. IF a Doctor selects a target Prescription_Status that is not an allowed transition from the Prescription's current clinical status, including any transition from the terminal statuses CANCELLED or EXPIRED, THEN THE System SHALL reject the change, retain the current clinical status unchanged, and display an error message indicating that the transition is not permitted.

### Requirement 6: View Prescription and Fulfilment Progress

**User Story:** As a Patient, I want to view my own prescription information and current clinical and pharmacy fulfilment status, so that I understand the progress of my medication.

#### Acceptance Criteria

1. WHEN an authenticated Patient opens prescription status, THE System SHALL display, in read-only form, only prescriptions owned by that authenticated Patient, and SHALL NOT display any prescription owned by another Patient.
2. WHEN a Patient opens the details of a visible Prescription, THE System SHALL display, in read-only form, the medication information and the clinical status, AND SHALL display the Fulfilment_Status when a Fulfilment_Status record exists for that Prescription, or an indication that fulfilment has not yet started when no Fulfilment_Status record exists.
3. WHEN a Patient views their prescriptions, THE System SHALL exclude every Prescription whose clinical status is DRAFT from both the prescription list and any detail view.
4. WHEN a Patient views a Prescription whose validity end date has passed, THE System SHALL display the Prescription's status as Expired.
5. IF a Patient attempts to access a Prescription not owned by that Patient, including access requested by supplying a substituted or tampered Prescription identifier, THEN THE System SHALL deny access, SHALL NOT display any of that Prescription's data, and SHALL present an error indication that access is not permitted.

### Requirement 7: Receive and Review Notifications

**User Story:** As a Patient, I want the system to record and display relevant prescription and fulfilment notifications, so that I can review important updates later.

#### Acceptance Criteria

1. WHEN one of the supported notification events (as listed in criterion 5) is processed, THE System SHALL create and store an in-system Patient Notification in the unread state, recording the associated patient, prescription reference, event type, and creation timestamp.
2. WHEN a Patient opens a Notification that is in the unread state, THE System SHALL transition that Notification to the read state and persist the read state so that it remains read on subsequent retrievals.
3. IF a Notification with the same deduplication key (patientId, prescriptionId, eventType) has already been successfully stored, THEN THE System SHALL prevent creation of a duplicate Notification for that event and retain the existing Notification unchanged.
4. IF a Notification creation or storage failure occurs, THEN THE System SHALL record the failure outcome and SHALL NOT reverse or roll back the underlying prescription or dispensing transaction, leaving that transaction committed.
5. THE System SHALL support the notification events Prescription Issued, Prescription Cancelled, Medication Preparing, Medication Ready for Collection, and Medication Dispensed, and SHALL reject any event outside this set without creating a Notification.

### Requirement 8: Dispense Medication

**User Story:** As a Pharmacist, I want to verify a valid prescription, verify the Patient, confirm sufficient usable stock, and complete dispensing, so that medication is handed over accurately and inventory remains consistent.

#### Acceptance Criteria

1. WHEN a Pharmacist confirms dispensing for an eligible Prescription whose total Eligible_Stock across one or more batches is greater than or equal to the full required quantity, THE System SHALL allocate the full required quantity, perform exactly one stock deduction for the fulfilment, record the transaction as Dispensed with the dispensed quantity and timestamp, and display a confirmation indicating the transaction succeeded.
2. IF the total Eligible_Stock across all eligible batches is less than the full required quantity, THEN THE System SHALL block the dispensing transaction, SHALL NOT deduct any stock from any batch, and SHALL display an error message indicating insufficient stock.
3. WHEN dispensing is completed using stock distributed across multiple eligible batches, THE System SHALL allocate stock from eligible batches in ascending order of expiry date (earliest-expiring first, FEFO), and WHERE two or more eligible batches share the same earliest expiry date, THE System SHALL allocate from those batches in a deterministic order until the required quantity is met.
4. IF a Prescription has a status of fully Dispensed, THEN THE System SHALL reject any subsequent dispensing transaction for that Prescription, SHALL NOT deduct any stock, and SHALL display an error message indicating the Prescription has already been dispensed.
5. WHEN Eligible_Stock is calculated for a Medicine, THE System SHALL exclude from Eligible_Stock the remaining physical quantity of any batch whose expiry date is earlier than the current system date, such that expired quantity contributes zero to Eligible_Stock.
6. WHEN a Pharmacist requests dispensing, THE System SHALL, before any stock is deducted, verify that the Patient identity on the Prescription matches the Patient record and that the required medication information (Medicine, required quantity, and Prescription validity) is present and complete.
7. IF Patient verification or required medication information verification fails, THEN THE System SHALL block the dispensing transaction, SHALL NOT deduct any stock, and SHALL display an error message indicating which verification failed.

### Requirement 9: Manage Medicine Inventory

**User Story:** As a Pharmacist, I want to maintain medicine records and stock batches, so that available inventory, expiry information, and stock movements remain accurate.

#### Acceptance Criteria

1. WHEN an authorised Pharmacist submits medicine information containing all required fields (at minimum a medicine name of 1 to 255 characters), THE System SHALL create the Medicine record and return a confirmation.
2. WHEN a Pharmacist receives stock with a batch identifier, an expiry date, and a quantity greater than 0, THE System SHALL increase the Inventory_Batch balance by that quantity and record a corresponding Stock_Movement entry in the audit trail.
3. IF a Pharmacist performs a manual stock adjustment without providing a reason of at least 1 non-whitespace character, THEN THE System SHALL reject the adjustment, leave the Inventory_Batch balance unchanged, and display an error indicating that a reason is required.
4. WHEN inventory is viewed and an Inventory_Batch has an expiry date earlier than the current system date, THE System SHALL display the Inventory_Batch in the inventory view and mark it as non-dispensable.
5. IF a stock change would reduce an Inventory_Batch balance below 0, THEN THE System SHALL reject the change before committing, leave the Inventory_Batch balance unchanged, and display an error indicating insufficient stock.
6. IF an authorised Pharmacist submits medicine information that is missing any required field or that contains a field exceeding its defined length, THEN THE System SHALL reject the creation and display an error message identifying the invalid field, without creating any Medicine record.
7. IF a Pharmacist submits received stock with a quantity less than or equal to 0 or with a missing expiry date, THEN THE System SHALL reject the stock receipt, leave the Inventory_Batch balance unchanged, record no Stock_Movement, and display an error message identifying the invalid field.

### Requirement 10: Generate and Export Operational Reports

**User Story:** As an Administrator, I want to generate and later retrieve operational reports, so that recorded system information can support review and management.

#### Acceptance Criteria

1. WHEN an authorised Administrator selects supported Report_Criteria for a Prescription Report, THE System SHALL generate a Report snapshot capturing the operational data matching the Report_Criteria as it existed at generation time and persist it as a read-only Report record without modifying operational records.
2. WHEN an authorised Administrator generates a Dispensing, Inventory, or User/Access Report, THE System SHALL generate a Report snapshot capturing the matching operational data as it existed at generation time and persist it as a read-only Report record without modifying operational records.
3. WHEN an authorised Administrator retrieves a previously generated Report, THE System SHALL present stored Report content identical to the content persisted at generation time, without modifying the Report snapshot or any operational records.
4. WHEN an authorised Administrator requests PDF export of a previously generated Report, THE System SHALL produce a PDF export whose content is derived solely from the saved Report snapshot and is identical to the stored Report content, without reading or recalculating live operational data.
5. IF no operational data matches the selected Report_Criteria, THEN THE System SHALL persist a Report snapshot containing zero records and return that empty Report result without modifying operational records.
6. IF a requester who is not an authorised Administrator requests to generate, retrieve, or export a Report, THEN THE System SHALL reject the request and make no change to any Report snapshot or operational records.
7. IF an authorised Administrator requests retrieval or PDF export of a Report that does not exist or whose stored snapshot is unavailable, THEN THE System SHALL reject the request with an indication that the Report was not found and make no change to any operational records.
8. IF an authorised Administrator submits Report_Criteria that are unsupported or invalid for the selected report type, THEN THE System SHALL reject the request without generating or persisting a Report snapshot and without modifying operational records.
