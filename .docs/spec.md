# Feature Specification: Pharmacy Inventory & Prescription System

**Feature Branch**: `001-pharmacy-inventory-prescription-system`  
**Created**: 2026-08-19  
**Status**: Clarified / Ready for Planning  
**Input**: Build a simple integrated healthcare/pharmacy information system that digitises routine work, keeps records for future use, provides clear workflows, and applies role-based permissions for Patient, Doctor, Pharmacist, and Administrator.

## User Scenarios & Testing

### User Story 1 - Authenticate and Access Permitted Functions (Priority: P1)

As a system user, I want to sign in and access only the functions permitted for my role so that the system remains simple, secure, and relevant to my responsibilities.

**Why this priority**: All protected workflows depend on authenticated and authorised access.

**Independent Test**: Can be tested by creating one account for each supported role, signing in, verifying successful access to permitted functions, and verifying that non-permitted functions are unavailable.

**Acceptance Scenarios**

1. **Given** an active account with valid credentials, **When** the user signs in, **Then** the system establishes an authenticated session and presents functions allowed for the assigned role.
2. **Given** invalid credentials, **When** the user attempts to sign in, **Then** access is denied and a clear generic authentication error is shown.
3. **Given** an authenticated user without permission for a protected function, **When** the user attempts to access that function, **Then** the system denies access.
4. **Given** an authenticated session older than one hour, **When** the user attempts to continue using a protected function, **Then** the system requires authentication again.
5. **Given** a new or changed password shorter than eight characters, **When** the user submits it, **Then** the system rejects the password.

---

### User Story 2 - Manage User Accounts and Profiles (Priority: P1)

As an Administrator, I want to create and manage user accounts and access roles, while each authenticated user can maintain permitted personal information, so that account administration and self-service profile management remain clearly separated.

**Why this priority**: The system requires controlled access for all other workflows.

**Independent Test**: Can be tested by creating an account, assigning one role, enabling/disabling the account, signing in as that user, and verifying that the user can update permitted profile fields but cannot change protected access information.

**Acceptance Scenarios**

1. **Given** an authorised Administrator, **When** the Administrator creates a valid user account and assigns one supported role, **Then** the account is stored and can later be used for access.
2. **Given** an existing user account, **When** an authorised Administrator disables it, **Then** the account cannot authenticate.
3. **Given** an authenticated user, **When** the user edits permitted personal profile information and submits valid values, **Then** the profile is updated.
4. **Given** an authenticated user, **When** the user attempts to modify role, account status, privileged permissions, or system-owned identifiers through self-service profile management, **Then** the change is rejected.
5. **Given** an existing Patient business record without login access, **When** an authorised Administrator provisions access, **Then** the Patient record is linked to a user account without creating a duplicate Patient record.

---

### User Story 3 - Maintain Patient Business Records (Priority: P1)

As a Doctor, I want to create the Patient business record required for clinical work even when the Patient does not yet have login access, so that treatment records can be created without requiring prior portal registration.

**Why this priority**: Prescription creation depends on an identifiable Patient record.

**Independent Test**: Can be tested by creating a Patient without a user account, using the Patient in a prescription workflow, and later linking login access through Administrator account management.

**Acceptance Scenarios**

1. **Given** an authorised Doctor, **When** the Doctor creates a Patient with the required information, **Then** the Patient business record is stored even without a user account.
2. **Given** a Patient without login access, **When** the Patient is used in an authorised prescription workflow, **Then** the system allows the clinical workflow to continue.
3. **Given** a Patient business record, **When** a Doctor attempts to administer the Patient's login credentials or account permissions, **Then** the system does not permit that action.

---

### User Story 4 - Manage Prescriptions (Priority: P1)

As a Doctor, I want to create, search, view, modify, and cancel prescriptions containing one or more medication items so that clinical prescription records can be maintained digitally.

**Why this priority**: Prescription management is a central clinical workflow.

**Independent Test**: Can be tested by creating a prescription with multiple items, retrieving it, editing permitted clinical content, and cancelling it.

**Acceptance Scenarios**

1. **Given** an authorised Doctor and an existing Patient, **When** the Doctor enters valid prescription information, **Then** the system stores the prescription and its one or more prescription items.
2. **Given** an existing editable prescription, **When** the Doctor modifies permitted clinical information, **Then** the system validates and stores the update.
3. **Given** a cancelled prescription, **When** a user later views it, **Then** it remains cancelled and cannot be restored through the current workflow.
4. **Given** a prescription in Draft state, **When** the Patient views their prescription list, **Then** the Draft prescription is not shown.
5. **Given** a prescription issued more than one month earlier, **When** the prescription is displayed or evaluated, **Then** the system identifies it as Expired.

---

### User Story 5 - Manage Clinical Prescription Status (Priority: P1)

As a Doctor, I want to change a prescription's clinical lifecycle state using only permitted transitions so that the clinical status remains valid and understandable.

**Why this priority**: Pharmacy fulfilment depends on a valid clinical prescription state.

**Independent Test**: Can be tested by moving an eligible prescription to On Hold, returning it to an allowed state where supported, cancelling it, and verifying that invalid transitions are rejected.

**Acceptance Scenarios**

1. **Given** an eligible prescription, **When** the Doctor selects an allowed target clinical status, **Then** the system stores the transition.
2. **Given** a status change for which a reason is required, **When** the Doctor does not provide a reason, **Then** the system prevents the transition.
3. **Given** a prescription controlled by another role, **When** a non-Doctor attempts to place it On Hold, **Then** the system rejects the action.
4. **Given** an expired prescription, **When** it is evaluated for fulfilment, **Then** it is treated as clinically ineligible.

---

### User Story 6 - View Prescription and Fulfilment Progress (Priority: P1)

As a Patient, I want to view my own prescription information and current clinical and pharmacy fulfilment status so that I understand the progress of my medication.

**Why this priority**: Updated status visibility is one of the minimum system requirements.

**Independent Test**: Can be tested by signing in as a Patient, viewing issued prescriptions, opening prescription details, and verifying clinical and fulfilment status while confirming that Draft or another Patient's records are inaccessible.

**Acceptance Scenarios**

1. **Given** an authenticated Patient with accessible prescriptions, **When** the Patient opens prescription status, **Then** the system shows only that Patient's visible prescriptions.
2. **Given** a visible prescription, **When** the Patient opens its details, **Then** the system shows medication information, clinical status, and fulfilment status where available.
3. **Given** a Draft prescription, **When** the Patient views their prescriptions, **Then** the Draft is excluded.
4. **Given** an Expired prescription, **When** the Patient views it, **Then** the Expired status is clearly displayed.
5. **Given** another Patient's prescription identifier, **When** a Patient attempts to access it, **Then** access is denied.

---

### User Story 7 - Receive and Review Notifications (Priority: P2)

As a Patient, I want the system to record and display relevant prescription and fulfilment notifications so that I can review important updates later.

**Why this priority**: Notification supports status awareness but is downstream from the core prescription and dispensing workflows.

**Independent Test**: Can be tested by generating a supported prescription or fulfilment event, verifying that a notification record appears for the Patient, opening it, and marking it as read.

**Acceptance Scenarios**

1. **Given** a supported notification event, **When** the event is processed, **Then** an in-system Patient notification is created and stored.
2. **Given** an unread notification, **When** the Patient opens it, **Then** it can be marked as read.
3. **Given** an equivalent notification already successfully processed for the same event, **When** the event is received again, **Then** the system prevents a duplicate notification.
4. **Given** a notification delivery failure, **When** processing finishes, **Then** the failure outcome remains recorded without reversing the underlying prescription or dispensing transaction.

---

### User Story 8 - Dispense Medication (Priority: P1)

As a Pharmacist, I want to verify a valid prescription, verify the Patient, confirm sufficient usable stock, and complete dispensing so that medication is handed over accurately and inventory remains consistent.

**Why this priority**: This is the main pharmacy fulfilment transaction.

**Independent Test**: Can be tested with an eligible prescription requiring stock from one or more non-expired batches, confirming successful dispensing, inventory deduction, and duplicate-dispense prevention.

**Acceptance Scenarios**

1. **Given** an eligible prescription and sufficient non-expired stock across one or more batches, **When** the Pharmacist confirms dispensing, **Then** the full required quantity is allocated and the transaction is recorded as Dispensed.
2. **Given** total eligible stock less than the full required quantity, **When** the Pharmacist attempts dispensing, **Then** the transaction is blocked and no stock is deducted.
3. **Given** sufficient eligible stock distributed across multiple batches, **When** dispensing is completed, **Then** stock is allocated from the earliest-expiring eligible batches first.
4. **Given** a prescription already fully dispensed, **When** another dispensing attempt is made, **Then** the system rejects the duplicate transaction.
5. **Given** an expired batch with remaining physical quantity, **When** stock availability is calculated, **Then** the expired quantity is excluded.

---

### User Story 9 - Manage Medicine Inventory (Priority: P1)

As a Pharmacist, I want to maintain medicine records and stock batches so that available inventory, expiry information, and stock movements remain accurate.

**Why this priority**: Accurate inventory is required for safe dispensing.

**Independent Test**: Can be tested by adding a medicine, receiving multiple batches, adjusting stock with a reason, and verifying low/expired stock visibility.

**Acceptance Scenarios**

1. **Given** an authorised Pharmacist, **When** valid medicine information is entered, **Then** the medicine record is created.
2. **Given** an existing medicine, **When** stock is received with batch, expiry, and quantity information, **Then** the inventory balance is increased and the stock movement is recorded.
3. **Given** an existing inventory batch, **When** the Pharmacist performs a manual stock adjustment, **Then** a reason is required before the adjustment can be committed.
4. **Given** an expired batch, **When** inventory is viewed, **Then** the batch remains visible for recordkeeping but is marked as non-dispensable.
5. **Given** a stock change that would create an invalid negative balance, **When** the Pharmacist submits the adjustment, **Then** the system rejects it.

---

### User Story 10 - Generate and Export Operational Reports (Priority: P2)

As an Administrator, I want to generate and later retrieve operational reports so that recorded system information can support review and management.

**Why this priority**: Reporting is a minimum requirement but is downstream from transaction capture.

**Independent Test**: Can be tested by generating each required report type, retrieving a previously generated report, and exporting the same saved report content without recalculating it from changed live records.

**Acceptance Scenarios**

1. **Given** an authorised Administrator, **When** the Administrator selects supported criteria for a Prescription Report, **Then** the system generates and stores a report snapshot.
2. **Given** the same conditions, **When** the Administrator generates a Dispensing, Inventory, or User/Access Report, **Then** the corresponding report snapshot is stored.
3. **Given** a previously generated report, **When** the Administrator retrieves it later, **Then** the stored report content represents the report as originally generated.
4. **Given** a previously generated report, **When** the Administrator requests PDF export, **Then** the export is produced from the saved report snapshot rather than recalculating current live operational data.
5. **Given** no matching data for selected criteria, **When** a report is generated, **Then** the system returns an empty report result without modifying operational records.

---

## Edge Cases

- A Patient business record exists but has no login account.
- An Administrator later links login access to that existing Patient.
- A Doctor attempts to manage Patient account credentials.
- A prescription contains multiple medication items.
- A Draft prescription exists but must remain invisible to the Patient.
- A prescription reaches one month from issue and must display as Expired.
- A cancelled prescription is reopened by URL or stale UI state.
- A status transition becomes invalid because the record changed after it was loaded.
- Stock exists physically but all available batches are expired.
- Total stock is sufficient only when quantities from several non-expired batches are combined.
- A dispensing request is submitted twice.
- Stock changes after availability was checked but before dispensing is confirmed.
- A manual stock adjustment would produce a negative balance.
- A notification event is received more than once.
- A saved report is exported after the underlying live records have changed.
- A user session expires while the user is on a protected screen.

## Requirements

### Functional Requirements

- **FR-001**: The system MUST support exactly four operational user roles in the current scope: Patient, Doctor, Pharmacist, and Administrator.
- **FR-002**: Each user account MUST have one operational role in the current scope.
- **FR-003**: The system MUST authenticate users before protected functions are accessed.
- **FR-004**: The system MUST restrict functions and data according to the authenticated user's assigned role and permitted access.
- **FR-005**: Passwords MUST contain at least eight characters.
- **FR-006**: Authenticated sessions MUST expire after one hour and protected access MUST require authentication again after expiry.
- **FR-007**: An authorised Administrator MUST be able to create, locate, activate, deactivate, unlock, and manage user accounts and role assignment.
- **FR-008**: Authenticated users MUST be able to view and update permitted personal profile information.
- **FR-009**: Self-service profile management MUST NOT allow modification of role assignment, account activation status, privileged permissions, or system-owned identifiers.
- **FR-010**: A Patient business record MUST be allowed to exist without a login account.
- **FR-011**: An authorised Doctor MUST be able to create a Patient business record required for clinical work.
- **FR-012**: A Doctor MUST NOT administer a Patient's login credentials or account permissions.
- **FR-013**: An authorised Administrator MUST be able to provision and link login access to an existing Patient business record.
- **FR-014**: A Patient business record MUST support Patient ID, full name, email, phone number, date of birth, address, and emergency contact.
- **FR-015**: The system MUST allow an authorised Doctor to create, search, view, modify, and cancel prescription records.
- **FR-016**: A Prescription MUST contain one or more Prescription Items.
- **FR-017**: A Prescription Item MUST record medicine, dosage, quantity, frequency, and instructions.
- **FR-018**: Prescription clinical content MUST remain independent from current medicine inventory availability.
- **FR-019**: Draft prescriptions MUST NOT be visible to Patients.
- **FR-020**: Cancelled prescriptions MUST remain final in the current scope.
- **FR-021**: A prescription MUST be treated as Expired one month after its issue date.
- **FR-022**: Expired prescriptions MUST be clearly identified to users where relevant and MUST NOT be eligible for dispensing.
- **FR-023**: The system MUST allow an authorised Doctor to perform only permitted clinical prescription status transitions.
- **FR-024**: Only a Doctor MUST be permitted to place a prescription On Hold in the current scope.
- **FR-025**: A status-change reason MUST be captured whenever the defined status operation requires one.
- **FR-026**: Clinical prescription status and pharmacy fulfilment status MUST remain distinct concepts.
- **FR-027**: An authenticated Patient MUST be able to view only their own accessible prescription information and status.
- **FR-028**: Patient prescription status views MUST present both clinical status and fulfilment status where applicable.
- **FR-029**: Patient prescription status views MUST be read-only.
- **FR-030**: The system MUST create and persist in-system Patient notifications for supported prescription and fulfilment events.
- **FR-031**: Supported notification events MUST include Prescription Issued, Prescription Cancelled, Medication Preparing, Medication Ready for Collection, and Medication Dispensed.
- **FR-032**: The system MUST preserve notification read and delivery outcome information.
- **FR-033**: The system MUST prevent duplicate successful notifications for the same supported business event.
- **FR-034**: Notification failure MUST NOT reverse an otherwise successful prescription or dispensing transaction.
- **FR-035**: The system MUST allow an authorised Pharmacist to dispense medication only for an eligible prescription.
- **FR-036**: The system MUST verify the Patient and required medication information before dispensing is completed.
- **FR-037**: Partial dispensing MUST NOT be supported in the current scope.
- **FR-038**: Dispensing MUST be allowed only when the total eligible non-expired stock is at least the full required quantity.
- **FR-039**: The system MUST evaluate all eligible batches before deducting any stock for a dispensing transaction.
- **FR-040**: When multiple eligible batches are required, stock MUST be allocated from the earliest-expiring eligible batch first.
- **FR-041**: Stock MUST be deducted only when final dispensing is confirmed.
- **FR-042**: A completed dispensing transaction MUST NOT be repeated for the same prescription fulfilment.
- **FR-043**: The system MUST preserve consistency between the dispensing record, inventory deduction, and final fulfilment status.
- **FR-044**: The system MUST allow an authorised Pharmacist to create and update medicine records.
- **FR-045**: The system MUST allow an authorised Pharmacist to receive stock, adjust stock, and view stock, batch, and expiry information.
- **FR-046**: Expired stock MUST remain visible for recordkeeping but MUST be excluded from dispensable inventory.
- **FR-047**: Manual stock adjustment MUST require a reason.
- **FR-048**: Inventory operations MUST NOT permit an invalid negative stock balance.
- **FR-049**: The system MUST allow an authorised Administrator to generate Prescription, Dispensing, Inventory, and User/Access reports.
- **FR-050**: Generated reports MUST be read-only with respect to operational records.
- **FR-051**: Each successfully generated report MUST be persisted as a historical report snapshot.
- **FR-052**: A saved report MUST preserve the report content as it existed at generation time.
- **FR-053**: A previously generated report MUST be retrievable later by an authorised user.
- **FR-054**: PDF export MUST use the saved historical report snapshot rather than regenerate the report from current operational data.
- **FR-055**: Authorised users MUST be able to search or retrieve records appropriate to their role.
- **FR-056**: The system MUST provide clear validation feedback when submitted data is missing, invalid, stale, or not permitted.
- **FR-057**: The system MUST prevent duplicate, unauthorised, or inconsistent updates where the corresponding workflow defines such protection.
- **FR-058**: The system MUST preserve operational records required for later authorised retrieval and reporting.
- **FR-059**: The user interface MUST use consistent terminology for the same business concept across workflows.
- **FR-060**: The user interface MUST clearly distinguish editable information, read-only information, status information, successful completion, and failure states.

### Key Entities

- **User Account**: Represents login identity, assigned role, account status, and access eligibility.
- **Credential**: Represents authentication information and password/security state associated with a user account.
- **Role Permission**: Represents the permissions associated with an operational role.
- **User Profile**: Represents self-service personal information for a system user.
- **Patient Profile / Patient Business Record**: Represents Patient information used for healthcare operations and may exist without login access.
- **Prescription**: Represents one clinical prescription associated with a Patient and Doctor.
- **Prescription Item**: Represents one medication entry within a Prescription; a Prescription contains one or more items.
- **Prescription Status**: Represents the clinical lifecycle state of a Prescription.
- **Prescription Status Summary**: Represents Patient-facing read-only status information combining authorised clinical and fulfilment progress.
- **Dispense Record**: Represents the pharmacy fulfilment transaction for an eligible prescription.
- **Medicine**: Represents medicine master information independently of stock quantity.
- **Inventory Item / Batch**: Represents stock quantity for a medicine batch and its expiry information.
- **Stock Movement**: Represents recorded stock receipt or adjustment activity and its reason where applicable.
- **Notification**: Represents a persisted Patient-facing notification, delivery outcome, and read state.
- **Report**: Represents a generated historical operational report snapshot.
- **Report Criteria**: Represents the criteria used to generate a report.

## Assumptions

- The current system supports a single organisational context; multi-branch, multi-tenant, and multi-organisation behaviour is outside the current scope.
- Users have one operational role each.
- Only Patient, Doctor, Pharmacist, and Administrator actors are in scope.
- Automatic duplicate Patient detection, merge, or reconciliation is handled by users and is not a system responsibility.
- Partial dispensing, refills, staged dispensing, and repeat dispensing are outside the current scope.
- Cancelled prescriptions are not restored.
- In-system notification is the baseline channel; external email, SMS, or push delivery is outside the current scope.
- Reporting is Administrator-facing in the current scope.
- Advanced enterprise controls such as multi-factor authentication, offline operation, formal disaster-recovery targets, and advanced compliance automation are outside the current feature scope.
- High-impact actions may use explicit confirmation in the UI where accidental execution would create meaningful operational risk.

## Implementation Constraints Handoff

The following clarified constraints are intentionally recorded for the later planning phase and are not intended to drive business-facing success criteria:

- Generated report snapshots are to be stored as compact/minified JSON in a long-text-capable field.
- Insignificant formatting whitespace is not required in the stored JSON; spaces inside legitimate string values are preserved.
- PDF export should reconstruct the export from the persisted report snapshot.
- The exact UI framework, PDF-generation framework, JSON serialization mechanism, and storage technology are deferred to planning.

## Success Criteria

### Measurable Outcomes

- **SC-001**: 100% of tested role scenarios prevent users from accessing functions outside their assigned role.
- **SC-002**: A Doctor can create a new Patient business record and use it in a prescription workflow without first creating a Patient login account.
- **SC-003**: An Administrator can later provision login access for that Patient without creating a second Patient business record.
- **SC-004**: A Doctor can create a valid prescription containing one or more medication items and retrieve the stored prescription afterward.
- **SC-005**: Patients see 0 Draft prescriptions in their Patient-facing prescription list.
- **SC-006**: Prescriptions older than one month are identified as Expired in all tested expiry scenarios and are blocked from dispensing.
- **SC-007**: In 100% of tested insufficient-stock scenarios, dispensing is blocked before any stock quantity is deducted.
- **SC-008**: In 100% of tested multi-batch dispensing scenarios, the system uses eligible stock in earliest-expiry order until the required quantity is satisfied.
- **SC-009**: In 100% of tested duplicate-dispensing scenarios, inventory is deducted no more than once for the completed fulfilment.
- **SC-010**: In 100% of tested expired-batch scenarios, expired stock remains visible for recordkeeping but contributes zero quantity to dispensable stock.
- **SC-011**: Every tested manual stock adjustment requires a reason before successful completion.
- **SC-012**: Supported prescription and fulfilment events create a retrievable Patient notification record, and the Patient can mark the notification as read.
- **SC-013**: Administrators can successfully generate all four required report categories: Prescription, Dispensing, Inventory, and User/Access.
- **SC-014**: Exporting a saved historical report after its underlying operational data changes produces the report content that was originally saved, not a recalculated current-state report.
- **SC-015**: Password values shorter than eight characters are rejected in 100% of tested password-change or creation scenarios.
- **SC-016**: A session older than one hour no longer permits access to protected functions without re-authentication.
- **SC-017**: For each primary user story, a user can identify the main action, current status, validation failure, and successful completion without relying on implementation-specific information.
- **SC-018**: All ten primary UCD functional areas can be traced to at least one user story and at least one testable functional requirement in this specification.
