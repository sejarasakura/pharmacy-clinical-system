# Implementation Plan: Pharmacy Inventory & Prescription System

## Overview

This plan implements the Java desktop application using the MVC + Storage architecture (no Service/Repository/DAO layer) under the package root `src/main/java/pharmacy_system/`. Work proceeds bottom-up and incrementally: build scaffolding first, then domain models (with behaviour and unit tests), then Storage contracts and in-memory implementations, then the common controllers, then per-UCD controllers, then the property-based tests for the 16 correctness properties, then Views wired to controllers, and finally `Main.java` wiring with end-to-end integration tests.

Tooling: JUnit 5 for unit/integration tests, jqwik for property-based tests. In-memory Storage implementations back all tests because the persistence technology is deferred. Every mutable aggregate carries a `version` field for optimistic concurrency.

## Tasks

- [x] 1. Set up project scaffolding and build configuration
  - Create the Gradle build (`build.gradle`, `settings.gradle`) targeting a modern Java LTS, with dependencies on JUnit 5 (`junit-jupiter`) and jqwik, and the test task configured to run both engines
  - Create the package directory structure under `src/main/java/pharmacy_system/` for `model/`, `view/`, `controller/`, and `storage/` including the five functional-domain subpackages (`security_user`, `clinical_prescription`, `patient_information`, `pharmacy_operations`, `management_dss`)
  - Create the mirrored test source root `src/test/java/pharmacy_system/`
  - Add a placeholder `Main.java` entry point so the project compiles
  - _Requirements: 1.1_

- [x] 2. Implement security/user domain models
  - [x] 2.1 Implement AccountStatus enum and RolePermission model
    - Create `AccountStatus` (PENDING, ACTIVE, DISABLED, LOCKED) in `model/security_user/`
    - Implement `RolePermission` with `roleName`, `permissionCodes`, `active`, and `hasPermission(permissionCode)`
    - _Requirements: 1.1, 1.3, 2.1_

  - [x] 2.2 Implement UserAccount model with lifecycle behaviour
    - Implement `UserAccount` with `status`, `registrationApproved`, `version`, and methods `isActive()`, `canAuthenticate()` (ACTIVE + approved + not locked), `approveRegistration()`, `enable()`, `disable(reason)`, `lock()`, `unlock()`, `validateRequiredFields()`
    - Enforce exactly one operational role per account and DISABLED accounts cannot authenticate
    - _Requirements: 1.1, 2.1, 2.2_

  - [x] 2.3 Implement Credential model with verification and password policy
    - Implement `Credential` with salted `passwordHash` (algorithm deferred behind a hashing seam), `failedAttempts`, `lockedUntil`, and methods `verifyPassword()`, `recordFailedAttempt()`, `resetFailedAttempts()`, `isTemporarilyLocked()`, `changePasswordHash()`
    - Enforce the password policy: reject any new/changed password shorter than 8 characters
    - _Requirements: 1.2, 1.5_
    - _Property 9: Password policy_

  - [x] 2.4 Implement UserProfile hierarchy including Patient business record
    - Implement abstract `UserProfile` with `validateProfileData()` and `applyChanges(changes)` that rejects restricted fields (role, account status, privileged permissions, system identifiers)
    - Implement `DoctorProfile`, `PatientProfile`, `PharmacyProfile`, `AdminProfile`; `PatientProfile` doubles as the Patient business record (dateOfBirth, emergencyContact) and may exist without a linked UserAccount
    - Enforce field validation: text fields max 255 chars, email of form local@domain, phone 7-15 digits, date of birth not later than current date, mandatory identifier and full name
    - _Requirements: 2.3, 2.4, 3.1, 3.4, 3.5_
    - _Property 12: Self-service field protection_

  - [x] 2.5 Write unit tests for security/user models
    - Test `canAuthenticate()` across all statuses, disable/enable/lock/unlock transitions, password verification and lockout, password length rejection, and profile field validation including restricted-field rejection
    - _Requirements: 1.2, 1.5, 2.2, 2.4, 3.4, 3.5_

- [x] 3. Implement clinical prescription domain models
  - [x] 3.1 Implement PrescriptionStatus enum with allowed transitions
    - Implement `PrescriptionStatus` (DRAFT, ISSUED, ON_HOLD, CANCELLED, EXPIRED) with `allowedTransitions()` encoding DRAFT→ISSUED|CANCELLED, ISSUED→ON_HOLD|CANCELLED, ON_HOLD→ISSUED|CANCELLED, and CANCELLED/EXPIRED terminal
    - _Requirements: 5.1, 5.5_
    - _Property 16: Valid transitions only_

  - [x] 3.2 Implement PrescriptionItem model
    - Implement `PrescriptionItem` with medicineId, medicineName, dosage, frequency, instructions, quantity, and mandatory-field validation
    - _Requirements: 4.1, 4.6_

  - [x] 3.3 Implement Prescription aggregate with status and expiry behaviour (Algorithm 3)
    - Implement `Prescription` with `items` (1..50), `version`, and methods `canTransitionTo(target)`, `changeStatus(target, changedBy, reason)`, `isExpired(today)` (issuedAt + 1 month, Algorithm 3), `isEligibleForDispensing()`, `isTerminalState()`
    - Enforce reason-required transitions, item count bounds (1..50), and rejection of edits/transitions on CANCELLED/EXPIRED prescriptions
    - _Requirements: 4.1, 4.2, 4.5, 4.7, 5.1, 5.2, 5.4, 5.5_
    - _Property 7: Expiry monotonicity; Property 16: Valid transitions only_

  - [x] 3.4 Write property test for prescription expiry
    - **Property 7: Expiry monotonicity**
    - **Validates: Requirements 4.5**

  - [x] 3.5 Write property test for valid status transitions
    - **Property 16: Valid transitions only**
    - **Validates: Requirements 5.1**

  - [x] 3.6 Write unit tests for prescription models
    - Test item-count bounds, mandatory field rejection, terminal-state edit rejection, reason-required transitions, and role-gated ON_HOLD guard expectations
    - _Requirements: 4.1, 4.2, 4.6, 4.7, 5.2_

- [x] 4. Implement patient information domain models
  - [x] 4.1 Implement PrescriptionStatusSummary read model
    - Implement `PrescriptionStatusSummary` built from a `Prescription` plus optional `DispenseRecord`, exposing clinicalStatus, fulfilmentStatus, and `hasFulfilmentRecord`; it never mutates either source
    - _Requirements: 6.1, 6.2, 6.4_

  - [x] 4.2 Implement Notification model with dedup key and state transitions
    - Implement `Notification` with `deduplicationKey` (patientId + prescriptionId + eventType), delivery/read state, and methods `markDelivered()`, `markDeliveryFailed(reason)`, `markRead()`
    - _Requirements: 7.1, 7.2, 7.5_
    - _Property 13: Notification de-duplication_

  - [x] 4.3 Write unit tests for patient information models
    - Test summary construction with and without a fulfilment record, dedup key generation, and read/delivery state transitions
    - _Requirements: 6.2, 7.1, 7.2_

- [x] 5. Implement pharmacy operations domain models
  - [x] 5.1 Implement Medicine and StockMovement models
    - Implement `Medicine` master data (no quantity) with name validation (1..255 chars) and required-field checks
    - Implement `StockMovementType` (RECEIVE, ADJUSTMENT, DISPENSE) and `StockMovement` audit record with mandatory reason for ADJUSTMENT
    - _Requirements: 9.1, 9.2, 9.3, 9.6_

  - [x] 5.2 Implement InventoryItem model with expiry and deduction behaviour
    - Implement `InventoryItem` with `version` and methods `getAvailableQuantity()` (0 if expired), `isExpired()`, `canDeduct(quantity)`, `deduct(quantity)`, `adjust(delta)` preventing negative balance
    - _Requirements: 9.4, 9.5, 8.5_
    - _Property 5: Non-negative stock_

  - [x] 5.3 Implement DispenseRecord model with fulfilment state machine
    - Implement `DispenseRecord` with requiredQuantities/dispensedQuantities, status (PENDING/VERIFIED/DISPENSED/FAILED), `version`, and methods `verifyPatient(patientId)`, `confirmDispensing(quantities)`, `completeFulfilment(pharmacistId)`, `isCompleted()`
    - _Requirements: 8.1, 8.4, 8.6_

  - [x] 5.4 Write property test for non-negative stock
    - **Property 5: Non-negative stock**
    - **Validates: Requirements 9.5**

  - [x] 5.5 Write unit tests for pharmacy operations models
    - Test expired-batch available quantity of zero, deduction guards, adjustment reason enforcement, medicine name validation, and dispense-record state transitions
    - _Requirements: 8.5, 9.1, 9.3, 9.4, 9.5_

- [x] 6. Implement management/DSS domain models
  - [x] 6.1 Implement ReportCriteria and Report models with snapshot behaviour
    - Implement `ReportCriteria` (transient) with `hasValidDateRange()` and `validateCriteria()`
    - Implement `Report` persisted snapshot with `rowCount`, immutable `data` (minified JSON snapshot), `isEmpty()`, and `generateExport(format)` that renders solely from the stored snapshot
    - _Requirements: 10.1, 10.2, 10.4, 10.5, 10.8_
    - _Property 15: Report snapshot immutability_

  - [x] 6.2 Write unit tests for report models
    - Test criteria validation, empty-snapshot construction, and export derived only from stored data
    - _Requirements: 10.4, 10.5, 10.8_

- [x]* 7. Checkpoint - domain models
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement Storage contracts and in-memory implementations
  - [x] 8.1 Define Storage interfaces for all aggregates
    - Define `UserAccountStorage`, `CredentialStorage`, `RolePermissionStorage`, `UserProfileStorage`, `PrescriptionStorage`, `NotificationStorage`, `DispenseStorage`, `InventoryStorage`, and `ReportStorage` as behavioural contracts, including optimistic-concurrency update signatures (expectedVersion) and query methods needed by controllers (e.g. `findByUsername`, `findByMedicineId`, `existsCompletedDispense`, `existsByDeduplicationKey`, `queryForReport`)
    - _Requirements: 1.1, 7.3, 8.1, 8.4, 10.1_

  - [x] 8.2 Implement in-memory Storage implementations
    - Implement in-memory backing for every Storage contract with version-checked updates that return conflict on stale versions, and combined item+movement persistence for inventory
    - Implement `InventoryStorage.deductInventory(plan, expectedVersion)` and `adjustStock(item, movement, expectedVersion)` atomically per call
    - _Requirements: 8.1, 9.2, 9.5_

  - [x]* 8.3 Write unit tests for in-memory Storage implementations
    - Test CRUD round-trips, version-conflict rejection, dedup-key existence checks, and completed-dispense existence checks
    - _Requirements: 7.3, 8.4_

- [x] 9. Implement common controllers
  - [x] 9.1 Implement SessionController with session-expiry enforcement (Algorithm 4)
    - Implement `SessionController` with `establishSession(account, roles)` setting `expiresAt = now + 1h`, plus `getCurrentUserId()`, `isAuthenticated()`, `isExpired()`, `hasPermission(code)`, `requirePermission(code)` (denies on missing permission or expired session), and `invalidateSession()`
    - _Requirements: 1.3, 1.4_
    - _Property 8: Session expiry_

  - [x] 9.2 Implement NavigationController
    - Implement `NavigationController` with `navigateToAuthorisedHome()`, `navigateToLogin()`, `navigateToAccessDenied()`
    - _Requirements: 1.3, 1.4_

  - [x]* 9.3 Write property test for session expiry
    - **Property 8: Session expiry**
    - **Validates: Requirements 1.4**

  - [x]* 9.4 Write unit tests for common controllers
    - Test permission grant/deny, session-expiry denial and re-authentication routing, and navigation targets
    - _Requirements: 1.3, 1.4_

- [x] 10. Implement security/user controllers
  - [x] 10.1 Implement AuthenticateAuthoriseController
    - Implement `authenticate(username, password)` (generic failure on missing/invalid, failed-attempt recording, lockout), `authorise(userId, code)`, `requestPasswordReset(email)`, `resetPassword(token, newPassword)` enforcing >= 8 chars, and `logout()`
    - Establish a session with role permissions on success
    - _Requirements: 1.1, 1.2, 1.5_
    - _Property 9: Password policy_

  - [x] 10.2 Implement ManageUserAccountController
    - Implement account lifecycle and role administration: `createUserAccount`, `approveRegistration`, `assignRole`, `removeRole`, `enableAccount`, `disableAccount`, `unlockAccount`, and provisioning login access for an existing unlinked Patient_Business_Record without creating a duplicate record
    - _Requirements: 2.1, 2.2, 2.5_

  - [x] 10.3 Implement ManageProfileController
    - Implement `loadProfile(userId)` and `updateProfile(userId, changes)` that persists permitted changes, retains non-submitted fields, and rejects restricted-field modifications
    - _Requirements: 2.3, 2.4_
    - _Property 12: Self-service field protection_

  - [x]* 10.4 Write property test for self-service field protection
    - **Property 12: Self-service field protection**
    - **Validates: Requirements 2.4**

  - [x]* 10.5 Write unit tests for security/user controllers
    - Test generic auth failure, disabled-account rejection, role separation between account admin and self-service, patient provisioning without duplicate record, and password reset policy
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 2.5_

- [x] 11. Implement clinical prescription controllers
  - [x] 11.1 Implement ManagePrescriptionController and patient business records
    - Implement `createPrescription`, `viewPrescription`, `editPrescription` (editable states only), `cancelPrescription`, plus Doctor creation of a Patient_Business_Record without a linked account and rejection of Doctor attempts at credential administration
    - Enforce patient existence, 1..50 items, mandatory fields, and CANCELLED retention with no re-open action
    - _Requirements: 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4, 4.6, 4.7_

  - [x] 11.2 Implement UpdatePrescriptionStatusController with optimistic concurrency
    - Implement `getAllowedTransitions`, `updateStatus(target, reason)` with version-checked persistence, `issuePrescription`, `placeOnHold` (Doctor only), `cancelPrescription`, `resumePrescription`; reject invalid transitions and non-Doctor ON_HOLD, keeping state unchanged, and treat EXPIRED as ineligible for fulfilment
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
    - _Property 16: Valid transitions only_

  - [x]* 11.3 Write unit tests for clinical prescription controllers
    - Test patient-existence rejection, item bounds, editable-state guards, ON_HOLD role gating, reason requirement, invalid-transition rejection, and concurrent-update handling
    - _Requirements: 4.1, 4.6, 4.7, 5.2, 5.3, 5.5_

- [x] 12. Implement patient information controllers
  - [x] 12.1 Implement ViewPrescriptionStatusController with ownership isolation
    - Implement `requestPrescriptionStatus(patientId)` returning only the patient's prescriptions excluding DRAFT, and `viewPrescriptionDetails(patientId, prescriptionId)` that re-verifies ownership and denies tampered/substituted identifiers, showing fulfilment status or a not-yet-started indication, and presenting expired prescriptions as Expired
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
    - _Property 10: Draft invisibility; Property 11: Patient isolation_

  - [x] 12.2 Implement SendAlertsNotificationsController with de-duplication (Algorithm 6)
    - Implement `publishDomainEvent`, `processNotification` enforcing the supported event set, dedup-key suppression of duplicates, persistence of failed deliveries without rolling back the underlying transaction, and `markNotificationRead`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
    - _Property 13: Notification de-duplication; Property 14: Transaction independence of notification_

  - [x]* 12.3 Write property test for draft invisibility
    - **Property 10: Draft invisibility**
    - **Validates: Requirements 6.3**

  - [x]* 12.4 Write property test for patient isolation
    - **Property 11: Patient isolation**
    - **Validates: Requirements 6.5**

  - [x]* 12.5 Write property test for notification de-duplication
    - **Property 13: Notification de-duplication**
    - **Validates: Requirements 7.3**

  - [x]* 12.6 Write property test for transaction independence of notification
    - **Property 14: Transaction independence of notification**
    - **Validates: Requirements 7.4**

  - [x]* 12.7 Write unit tests for patient information controllers
    - Test cross-patient access denial, DRAFT exclusion, unsupported-event rejection, dedup suppression, and failure persistence without rollback
    - _Requirements: 6.1, 6.5, 7.4, 7.5_

- [x] 13. Implement pharmacy operations controllers
  - [x] 13.1 Implement ManageMedicineInventoryController with stock adjustment (Algorithm 5)
    - Implement `createMedicine`, `updateMedicine`, `receiveStock` (quantity > 0, expiry required, records Stock_Movement), `adjustStock` (reason mandatory, reject negative-balance before commit, Algorithm 5), and `viewInventoryDetails` marking expired batches non-dispensable
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_
    - _Property 5: Non-negative stock; Property 6: Adjustment reason required_

  - [x] 13.2 Implement FEFO allocation planner (Algorithm 1)
    - Implement `planFefoAllocation(batches, requiredQty, today)` excluding expired batches, sorting by earliest expiry with a deterministic tiebreak, returning an allocation plan summing exactly to the required quantity or empty when eligible stock is insufficient, without mutating any batch
    - _Requirements: 8.1, 8.2, 8.3, 8.5_
    - _Property 1: No over-dispensing; Property 2: FEFO ordering; Property 3: Allocation conservation_

  - [x] 13.3 Implement DispenseMedicationController orchestration (Algorithm 2)
    - Implement `requestEligiblePrescription`, `verifyPrescriptionAndPatient`, `checkStockAvailability`, `confirmDispensing`, `completeDispensing` with the full guard sequence (eligibility, duplicate-fulfilment block, patient verification, plan-all-before-deduct, single deduction with optimistic concurrency, completion, and dispensed domain event)
    - Enforce no partial dispense, insufficient-stock block with no deduction, and duplicate-dispense rejection
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.6, 8.7_
    - _Property 1: No over-dispensing; Property 4: Idempotent fulfilment_

  - [x]* 13.4 Write property test for no over-dispensing
    - **Property 1: No over-dispensing**
    - **Validates: Requirements 8.2**

  - [x]* 13.5 Write property test for FEFO ordering
    - **Property 2: FEFO ordering**
    - **Validates: Requirements 8.3**

  - [x]* 13.6 Write property test for allocation conservation
    - **Property 3: Allocation conservation**
    - **Validates: Requirements 8.1**

  - [x]* 13.7 Write property test for idempotent fulfilment
    - **Property 4: Idempotent fulfilment**
    - **Validates: Requirements 8.4**

  - [x]* 13.8 Write property test for adjustment reason required
    - **Property 6: Adjustment reason required**
    - **Validates: Requirements 9.3**

  - [x]* 13.9 Write unit tests for pharmacy operations controllers
    - Test stock receipt validation, adjustment reason/negative-balance guards, patient/medication verification failure, insufficient-stock block, and duplicate-dispense rejection
    - _Requirements: 8.2, 8.4, 8.6, 8.7, 9.2, 9.7_

- [x] 14. Implement management/DSS controller
  - [x] 14.1 Implement GenerateReportsController with snapshot persistence (Algorithm 7)
    - Implement `generateReport(criteria)` (authorised, read-only cross-domain queries, empty snapshot when no data), `persistReport`, and `exportReport(report, format)` deriving output solely from the saved snapshot; reject unauthorised requesters, unknown/invalid criteria, and missing-report retrieval/export
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8_
    - _Property 15: Report snapshot immutability_

  - [x]* 14.2 Write property test for report snapshot immutability
    - **Property 15: Report snapshot immutability**
    - **Validates: Requirements 10.4**

  - [x]* 14.3 Write unit tests for the reports controller
    - Test empty-result snapshot, unauthorised rejection, invalid criteria rejection, missing-report handling, and read-only operational access
    - _Requirements: 10.5, 10.6, 10.7, 10.8_

- [x]* 15. Checkpoint - controllers and property tests
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 16. Implement Views wired to controllers
  - [-] 16.1 Implement the authenticated shell, login, and navigation Views
    - Implement the login View and role-filtered application shell wired to `AuthenticateAuthoriseController`, `SessionController`, and `NavigationController`, surfacing generic auth errors, session-expired routing, and access-denied states
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [-] 16.2 Implement clinical prescription Views
    - Implement prescription management and status Views wired to their controllers, surfacing validation errors, editable/read-only presentation, and invalid-transition/concurrent-update errors
    - _Requirements: 4.1, 4.2, 4.3, 5.1, 5.5_

  - [-] 16.3 Implement patient information Views
    - Implement the patient prescription-status View and notification centre wired to their controllers, showing read-only own-prescription data (DRAFT excluded), fulfilment progress, and notification read state
    - _Requirements: 6.1, 6.2, 6.3, 7.2_

  - [-] 16.4 Implement pharmacy operations Views
    - Implement dispensing and inventory Views wired to their controllers, surfacing insufficient-stock, verification-failure, duplicate-dispense, and adjustment-reason/negative-balance errors and non-dispensable expired batches
    - _Requirements: 8.2, 8.7, 9.3, 9.4, 9.5_

  - [x] 16.5 Implement management/DSS Views
    - Implement the report generation, retrieval, and PDF export View wired to `GenerateReportsController`, surfacing empty-result, not-found, and invalid-criteria states
    - _Requirements: 10.1, 10.3, 10.4, 10.5_

  - [-] 16.6 Implement account and profile Views
    - Implement the administrator account-management View and the self-service profile View wired to their controllers, distinguishing administrative fields from self-service fields and rejecting restricted edits
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 17. Wire the application together in Main.java
  - [x] 17.1 Compose dependency wiring and launch the authenticated shell
    - In `Main.java`, instantiate in-memory Storage implementations, common controllers, per-UCD controllers, and Views, wire domain-event publication from dispensing/prescription flows to the notifications controller, and launch the login/authenticated shell
    - _Requirements: 1.1, 7.1_

  - [ ]* 17.2 Write end-to-end integration tests over wired controllers and storage
    - Exercise full flows against in-memory storage: authenticate → create/issue prescription → dispense (FEFO, single deduction, dispensed event) → patient views status → administrator generates and exports a report snapshot; and status transition under optimistic concurrency
    - _Requirements: 1.1, 4.1, 5.1, 8.1, 8.3, 6.2, 10.1, 10.4_

- [~] 18. Final checkpoint - full build and test suite
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional test tasks and can be skipped for a faster MVP; core implementation tasks are never optional.
- Each task references specific requirement acceptance criteria for traceability, and property tasks reference the correctness property they encode.
- The seven key algorithms map to tasks as follows: Algorithm 1 (FEFO) → 13.2, Algorithm 2 (dispensing orchestration) → 13.3, Algorithm 3 (prescription expiry) → 3.3, Algorithm 4 (session expiry) → 9.1, Algorithm 5 (stock adjustment) → 13.1, Algorithm 6 (notification de-duplication) → 12.2, Algorithm 7 (report snapshot persistence) → 14.1.
- All 16 correctness properties are covered by dedicated property-based test sub-tasks using jqwik.
- In-memory Storage implementations back all tests because the persistence technology is deferred.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1", "3.1", "3.2", "4.1", "5.1", "6.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "3.3", "4.2", "5.2", "5.3"] },
    { "id": 3, "tasks": ["2.5", "3.4", "3.5", "3.6", "4.3", "5.4", "5.5", "6.2", "8.1"] },
    { "id": 4, "tasks": ["8.2"] },
    { "id": 5, "tasks": ["8.3", "9.1", "9.2"] },
    { "id": 6, "tasks": ["9.3", "9.4", "10.1", "10.2", "10.3", "11.1", "11.2", "12.1", "12.2", "13.1", "13.2", "14.1"] },
    { "id": 7, "tasks": ["10.4", "10.5", "11.3", "12.3", "12.4", "12.5", "12.6", "12.7", "13.3", "13.4", "13.5", "13.6", "13.8", "13.9", "14.2", "14.3"] },
    { "id": 8, "tasks": ["13.7"] },
    { "id": 9, "tasks": ["16.1", "16.2", "16.3", "16.4", "16.5", "16.6"] },
    { "id": 10, "tasks": ["17.1"] },
    { "id": 11, "tasks": ["17.2"] }
  ]
}
```
