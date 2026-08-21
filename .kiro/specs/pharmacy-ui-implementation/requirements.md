# Requirements Document

## Introduction

The Pharmacy UI Implementation feature delivers the complete server-rendered desktop user interface for the Pharmacy Inventory & Prescription System. It realises the 32 primary UI surfaces described in the high-fidelity Spring Boot + Java + JSP Implementation Plan (`.docs/Pharmacy_SpringBoot_Java_JSP_High_Fidelity_Implementation_Plan_FIXED_STRUCTURE.md`, hereafter the "Implementation Plan"), styled through the tokens and component classes defined in the PharmaCare stylesheet (`.docs/pharmacare.css`), and bound to the existing MVC + Storage backend already scoped by the `pharmacy-inventory-prescription-system` spec.

The UI covers all ten use cases (UCD-01 through UCD-10) at the presentation layer: authentication and access denial (UCD-04), self-service profile management (UCD-05), administrative user account management (UCD-06), clinical prescription authoring (UCD-01), clinical status transitions (UCD-07), patient prescription and fulfilment visibility (UCD-02), patient notifications (UCD-03), pharmacist dispensing (UCD-08), medicine inventory (UCD-10), and administrator reporting (UCD-09). Role-based access is enforced for the four operational roles defined in the source specification: Patient, Doctor, Pharmacist, and Administrator (one operational role per account).

Scope boundaries carried from the Implementation Plan: the Java package/class structure is fixed and MUST NOT be extended with new architectural layers such as `service`, `dto`, `repository`, `mapper`, or `config`; JSP views bind to existing boundary View classes rather than introducing parallel DTOs; all state mutation routes through the fixed controllers and `*Storage` classes; JSPs contain no domain logic; and the PharmaCare stylesheet is the single source of visual truth. Domain rules (FEFO allocation, one-hour session expiry, one-month prescription expiry, mandatory adjustment reasons, persisted report snapshots, etc.) remain the responsibility of the backend spec; this spec constrains only their UI presentation and interaction.

These requirements are numbered 1..17 and are traceable to (a) the ten user stories in the underlying backend spec `.kiro/specs/pharmacy-inventory-prescription-system/requirements.md`, and (b) the 32 primary UI surfaces enumerated in Section 3 of the Implementation Plan.

## Glossary

- **System**: The Pharmacy Inventory & Prescription System UI as a whole (server-rendered JSP + progressive JavaScript running against Spring MVC).
- **JSP_View**: A server-rendered JSP page under `src/main/webapp/WEB-INF/jsp/` that produces the HTML for one primary UI surface (for example `prescription/list.jsp`) or for a shared JSP fragment under `WEB-INF/jsp/fragments/`.
- **UI_Surface**: One of the 32 primary screens enumerated in Section 3 of the Implementation Plan (identified by a code such as AUTH-01, RX-02, DISP-03).
- **App_Shell**: The persistent authenticated desktop shell composed of the sidebar (`.sidebar`), the top header (`.topbar`), and the main workspace (`.main-panel` / `.page`) as defined in `pharmacare.css`.
- **Role_Navigation**: The role-filtered navigation region rendered in the sidebar, showing only the modules permitted for the authenticated account's Operational_Role.
- **Operational_Role**: One of exactly four roles: Patient, Doctor, Pharmacist, Administrator.
- **Design_System**: The PharmaCare stylesheet at `.docs/pharmacare.css`, including its CSS custom properties (design tokens beginning with `--pc-`) and its component classes (for example `.btn`, `.btn-primary`, `.status`, `.status-success`, `.card`, `.data-table`).
- **Design_Token**: A CSS custom property defined at `:root` in the Design_System, such as `--pc-primary`, `--pc-danger`, `--pc-radius-md`.
- **Component_Class**: A CSS class defined in the Design_System, such as `.btn-primary`, `.data-table`, `.status-warning`, `.alert-danger`.
- **Fixed_Controller**: One of the Spring MVC controllers in the fixed Java structure (for example `AuthenticateAuthoriseController`, `ManagePrescriptionController`). New Spring controller classes MUST NOT be created.
- **Boundary_View_Class**: A supplied Java View class (for example `LoginFormView`, `PrescriptionFormView`, `DispenseResultView`) used as a `@ModelAttribute` form-backing object or as page state, in place of DTOs.
- **Storage_Class**: One of the fixed `*Storage.java` classes (for example `PrescriptionStorage`, `InventoryStorage`).
- **Route**: An HTTP path served by a Fixed_Controller (for example `GET /doctor/prescriptions/{id}`).
- **Session_Context**: The authenticated user context established by `SessionController` after successful authentication, valid for one hour.
- **Permitted_Function**: A UI action or module that the authenticated account's Operational_Role is authorised to invoke.
- **Read_Only_Field**: A field that is rendered as static text or with a `readonly`/`disabled` attribute and cannot be edited through the UI regardless of client-side manipulation.
- **Empty_State**: A JSP-rendered state shown when a data query returns zero records, distinguished from Filtered_Empty_State (zero results after applying filters).
- **Filtered_Empty_State**: A JSP-rendered state shown when a filtered query returns zero records; active filters remain visible and can be cleared.
- **Loading_Skeleton**: A JSP-rendered placeholder shaped like the target content, used during initial page load only.
- **Inline_Alert**: A page-level or section-level alert rendered using the Design_System `.alert` component (with `.alert-success`, `.alert-warning`, or `.alert-danger` variant).
- **Field_Error**: An inline validation message rendered adjacent to an input using the Design_System `.field-error` class and `.input-error` on the offending input.
- **Sticky_Action_Bar**: The bottom-anchored Save/Cancel region rendered using `.sticky-actions` from the Design_System.
- **Critical_Confirmation**: A focused confirmation surface (modal or dedicated page) used for irreversible or high-impact actions such as prescription cancellation, account state change, final dispensing, and manual stock adjustment.
- **Status_Badge**: A rendered status indicator using the Design_System `.status` class plus a semantic variant (`.status-success`, `.status-warning`, `.status-info`, `.status-neutral`, `.status-danger`) with readable text content.
- **Clinical_Status**: The prescription lifecycle state (Draft, Issued, On Hold, Cancelled, Expired) rendered as a Status_Badge.
- **Fulfilment_Status**: The pharmacy dispensing progress state (for example Pending, Preparing, Ready, Dispensed) rendered as a separate Status_Badge distinct from Clinical_Status.
- **CSRF_Token**: A Spring Security cross-site request forgery token embedded in every POST form and validated server-side.
- **PRG_Pattern**: The Post/Redirect/Get pattern where a successful POST issues an HTTP 302 redirect to a GET route so that the browser back button does not re-submit the mutation.

## Requirements

### Requirement 1: Application Shell and Role-Filtered Navigation

**User Story:** As an authenticated user of any operational role, I want a consistent application shell with only the modules my role can use, so that the interface remains predictable and I am not exposed to actions I cannot perform.

#### Acceptance Criteria

1. WHEN an authenticated user requests any protected route, THE System SHALL render the App_Shell with the sidebar, top header, and main workspace regions defined by the `.app-shell`, `.sidebar`, `.topbar`, and `.main-panel` classes of the Design_System.
2. WHEN the App_Shell renders for an authenticated account, THE Role_Navigation SHALL include only the navigation items whose destination is a Permitted_Function for the account's Operational_Role, and SHALL exclude every navigation item for which the account's Operational_Role has no permission.
3. WHEN the Role_Navigation renders, THE System SHALL apply the `.nav-item.active` Component_Class to exactly the one navigation item whose route matches the currently rendered UI_Surface, and SHALL apply the `.nav-item` Component_Class without the `active` modifier to every other rendered navigation item.
4. WHEN the App_Shell renders for any authenticated Operational_Role, THE System SHALL display in the top header a text label identifying the current module or record context, a text label identifying the authenticated user, and a text label identifying the authenticated user's Operational_Role using the `.role-badge` Component_Class.
5. IF an unauthenticated request targets any route other than `/login`, `/password/recovery`, `/password/reset`, or `/access-denied`, THEN THE System SHALL redirect to `/login` and SHALL NOT render the App_Shell.
6. IF an authenticated user requests a route whose destination is not a Permitted_Function for the account's Operational_Role, THEN THE System SHALL render the AUTH-04 Access Denied UI_Surface and SHALL NOT render the requested route's content.
7. WHEN a user selects the logout control in the top header, THE System SHALL invalidate the Session_Context, redirect to `/login`, and prevent browser back navigation from rendering previously protected content by setting the `Cache-Control: no-store` header on every protected response.

### Requirement 2: PharmaCare Design System Compliance

**User Story:** As a maintainer of the UI, I want every rendered surface to consume the PharmaCare stylesheet exclusively, so that the visual language stays consistent and future token changes propagate without touching individual pages.

#### Acceptance Criteria

1. THE System SHALL include `.docs/pharmacare.css` (served from `src/main/resources/static/css/pharmacare.css`) as the only application-wide stylesheet on every JSP_View through a shared fragment `WEB-INF/jsp/fragments/head.jspf`.
2. WHEN a JSP_View renders a button, THE System SHALL use the Component_Class `.btn` plus exactly one of the Design_System variants (`.btn-primary`, `.btn-secondary`, `.btn-link`, `.btn-add`, or a `.btn` element with `.btn-danger` for destructive text emphasis), and SHALL NOT introduce alternative button classes.
3. WHEN a JSP_View renders a data table, THE System SHALL use the Component_Class `.table-wrap` around a `<table class="data-table">` element and SHALL use the `.data-table th` and `.data-table td` styling defined in the Design_System without redefining table typography inline.
4. WHEN a JSP_View renders a status indicator, THE System SHALL use the Component_Class `.status` combined with exactly one variant among `.status-success`, `.status-warning`, `.status-info`, `.status-neutral`, and `.status-danger`, and SHALL place readable status text inside the badge element.
5. WHEN a JSP_View renders a page-level or section-level alert, THE System SHALL use the Component_Class `.alert` combined with exactly one variant among `.alert-success`, `.alert-warning`, and `.alert-danger`.
6. WHEN a JSP_View renders a form field, THE System SHALL wrap the label and input in a container with the Component_Class `.field`, and SHALL render inline validation errors using the Component_Class `.field-error` on a sibling element and `.input-error` on the offending input.
7. IF a JSP_View requires a colour, spacing, radius, or shadow value that is defined in the Design_System, THEN THE System SHALL reference the corresponding Design_Token (for example `var(--pc-primary)`, `var(--pc-radius-md)`) rather than hard-coding a literal value.
8. THE System SHALL NOT include inline `style="..."` attributes on any element for values covered by the Design_System, and SHALL NOT include page-scoped CSS overrides that redefine any Design_Token or Component_Class listed in `pharmacare.css`.
9. WHEN the viewport width is at or below 1100 pixels, THE System SHALL apply the Design_System's built-in narrow-desktop rules (sidebar reduced to 220 pixels, horizontal scroll on `.table-wrap`) without introducing additional media queries.

### Requirement 3: Authentication UI (UCD-04)

**User Story:** As a user of any operational role, I want to sign in, recover a forgotten password, complete a password reset, and see a clear access-denied page when I lack permission, so that I can access the application safely without exposure to account-enumeration or security internals.

#### Acceptance Criteria

1. WHEN an unauthenticated user requests `GET /login`, THE System SHALL render the AUTH-01 Login UI_Surface containing exactly the following inputs: one identifier field, one password field with a show/hide toggle control, and one submit control labelled "Sign In", together with one text link to `GET /password/recovery`.
2. WHEN a user submits the login form with an empty identifier or an empty password, THE System SHALL render the AUTH-01 UI_Surface again with a Field_Error adjacent to each empty required field, SHALL NOT submit the credentials to `SessionController`, and SHALL preserve any non-empty entered identifier value.
3. WHEN a user submits the login form with a non-empty identifier and a non-empty password that do not authenticate successfully, THE System SHALL render the AUTH-01 UI_Surface with a single Inline_Alert using `.alert-danger` containing a message that does not indicate whether the identifier or the password was incorrect, and SHALL clear the password field.
4. WHEN a user submits the login form with credentials that authenticate successfully, THE System SHALL establish the Session_Context and issue an HTTP 302 redirect to the default route of the authenticated account's Operational_Role: `/doctor/prescriptions` for Doctor, `/patient/prescriptions` for Patient, `/pharmacy/dispensing` for Pharmacist, and `/admin/users` for Administrator.
5. WHEN an unauthenticated user requests `GET /password/recovery`, THE System SHALL render the AUTH-02 Password Recovery UI_Surface with one identity field and one submit control, and THE System SHALL respond to any submission with an Inline_Alert whose wording does not confirm or deny the existence of an account for the submitted identity value.
6. WHEN an unauthenticated user requests `GET /password/reset` with a valid reset context, THE System SHALL render the AUTH-03 Reset Password UI_Surface containing two password fields (new password and confirm new password), each with a show/hide toggle, and one submit control labelled "Reset Password".
7. IF the submitted new password on the AUTH-03 UI_Surface contains fewer than eight characters or does not match the confirmation value, THEN THE System SHALL render the AUTH-03 UI_Surface again with a Field_Error explaining the specific rule violated (length or mismatch), and SHALL NOT invoke the credential update.
8. IF an authenticated user's request resolves to Access Denied per Requirement 1.6, THEN THE System SHALL render the AUTH-04 Access Denied UI_Surface containing a heading, a description that does not reveal permission internals, one link to the authenticated account's default role route, and one link labelled "Back" that returns to the previous authorised page.

### Requirement 4: My Profile UI (UCD-05)

**User Story:** As an authenticated user of any operational role, I want to view and update my own personal profile, change my password, and configure my notification preferences, so that I can maintain my own information without administrative help.

#### Acceptance Criteria

1. WHEN an authenticated user requests `GET /profile`, THE System SHALL render the PROF-01 My Profile UI_Surface containing the current user's profile subtype data, and SHALL render every Operational_Role, account status, and privileged-permission field as a Read_Only_Field.
2. WHEN the PROF-01 UI_Surface renders, THE System SHALL include exactly three navigation controls: one link to `GET /profile/edit`, one link to `GET /profile/password`, and one link to `GET /profile/preferences`.
3. WHEN an authenticated user requests `GET /profile/edit`, THE System SHALL render the PROF-02 Edit Profile UI_Surface as a form bound to `ProfileFormView` with editable fields limited to the current-user personal fields permitted for the account's profile subtype, and SHALL render every Read_Only_Field either as static text or omit it from the form entirely.
4. IF a user submits PROF-02 with any field value that fails the server-side format validation defined by `UserProfile.validateProfileData()`, THEN THE System SHALL render PROF-02 again with a Field_Error adjacent to each invalid field, an Inline_Alert summarising the failure, and all previously entered non-invalid values preserved.
5. WHEN a user submits PROF-02 successfully, THE System SHALL persist the changes through `ProfileStorage.update(profile, expectedVersion)` and SHALL issue an HTTP 302 redirect to `GET /profile` following the PRG_Pattern with a one-time success flash rendered as an `.alert-success` Inline_Alert on the next `GET /profile` page.
6. WHEN an authenticated user requests `GET /profile/password`, THE System SHALL render the PROF-03 Change Password UI_Surface as a form with three password fields (current, new, confirm), each with a show/hide toggle.
7. IF the PROF-03 submission contains a new password shorter than eight characters or a confirm value that does not match the new password, THEN THE System SHALL render PROF-03 again with a Field_Error identifying the specific rule violated, and SHALL NOT invoke any credential update.
8. WHEN an authenticated user requests `GET /profile/preferences`, THE System SHALL render the PROF-04 Notification Preferences UI_Surface with one control per supported preference and one Sticky_Action_Bar containing "Save" and "Reset" controls.

### Requirement 5: User Account Management UI (UCD-06)

**User Story:** As an Administrator, I want to search, create, edit, configure the role of, and enable/disable/unlock user accounts, so that I can administer access without exposing account administration to other roles.

#### Acceptance Criteria

1. WHEN an authenticated Administrator requests `GET /admin/users`, THE System SHALL render the IAM-01 User Account Management UI_Surface containing a `.data-table` of user accounts with columns for username, email, Operational_Role, account status, and one trailing action column, together with a filter toolbar (search input, role select, account-state select, Refresh control) and one primary control labelled "Add Account" linking to `GET /admin/users/new`.
2. WHEN the IAM-01 data table renders with zero matching accounts and no active filters, THE System SHALL render an Empty_State containing an explanatory message and one primary control labelled "Add Account".
3. WHEN the IAM-01 data table renders with zero matching accounts while at least one filter is active, THE System SHALL render a Filtered_Empty_State that keeps the active filter controls populated and provides one control labelled "Clear Filters" that removes all active filter values.
4. WHEN an authenticated Administrator requests `GET /admin/users/new`, THE System SHALL render the IAM-02 Create/Edit Account UI_Surface as a form bound to `UserAccountFormView` with fields for username, email, and Operational_Role, and SHALL NOT render any input for raw password value or password hash.
5. IF an IAM-02 submission targets Patient as the Operational_Role, THEN THE System SHALL render a Patient business-record lookup section on the same form so that an existing Patient business record can be selected before submission.
6. IF an IAM-02 submission includes a username or email value that duplicates an existing User_Account according to `UserAccountStorage`, THEN THE System SHALL render IAM-02 again with a Field_Error adjacent to the duplicated field and preserve every other entered value.
7. WHEN an authenticated Administrator requests `GET /admin/users/{id}/state` for an existing account, THE System SHALL render the IAM-04 Disable/Enable/Unlock UI_Surface as a Critical_Confirmation containing the account identity, the current status, the requested target status, and one confirm control matching the requested transition (Enable, Disable, or Unlock).
8. IF a non-Administrator authenticated user requests any route under `/admin/users`, THEN THE System SHALL resolve the request to AUTH-04 per Requirement 1.6 and SHALL NOT render any account data.

### Requirement 6: Manage Prescription UI (UCD-01)

**User Story:** As a Doctor, I want to search, view, create, edit, and cancel prescriptions containing one or more medication items, so that I can maintain clinical prescription records for my patients.

#### Acceptance Criteria

1. WHEN an authenticated Doctor requests `GET /doctor/prescriptions`, THE System SHALL render the RX-01 Prescription Workspace UI_Surface containing a `.data-table` of the Doctor's prescriptions with columns for prescription identifier, patient name, issue date, medication summary, Clinical_Status badge, and one trailing action column, together with a filter toolbar and one primary control labelled "Create Prescription" linking to `GET /doctor/prescriptions/new`.
2. WHEN the RX-01 data table renders a prescription whose issue date is more than one calendar month before the current date, THE System SHALL render the Clinical_Status column for that row using a Status_Badge with the text "Expired" and the `.status-warning` variant.
3. WHEN an authenticated Doctor requests `GET /doctor/prescriptions/{id}` for a prescription owned by that Doctor, THE System SHALL render the RX-02 Prescription Details UI_Surface containing a sticky record-identity header, a medication items table listing every `PrescriptionItem` with medicine, dosage, quantity, frequency, and instructions, and a contextual read-only Fulfilment_Status section when a matching `DispenseRecord` exists.
4. WHEN the RX-02 UI_Surface renders for a prescription whose current status is CANCELLED or EXPIRED, THE System SHALL omit the "Edit" and "Change Status" controls from the record-header command bar and SHALL render a non-dismissable Inline_Alert using `.alert-warning` explaining that the prescription cannot be modified.
5. WHEN an authenticated Doctor requests `GET /doctor/prescriptions/new` or `GET /doctor/prescriptions/{id}/edit`, THE System SHALL render the RX-03 Create/Edit Prescription UI_Surface as a form bound to `PrescriptionFormView` with a patient selector section, one or more repeatable Prescription_Item rows using `PrescriptionItemView`, controls to add and remove item rows, and a Sticky_Action_Bar with "Save" and "Cancel" controls.
6. IF an RX-03 submission contains fewer than one Prescription_Item, more than fifty Prescription_Items, or any Prescription_Item with a missing mandatory field (medicine, dosage, quantity, frequency), THEN THE System SHALL render RX-03 again with a Field_Error adjacent to each invalid Prescription_Item and an Inline_Alert summarising the count-rule violation when applicable.
7. WHEN an authenticated Doctor selects the "Cancel Prescription" control on the RX-02 UI_Surface, THE System SHALL render the RX-04 Cancel Prescription UI_Surface as a Critical_Confirmation containing the prescription identifier, the patient name, the current Clinical_Status, a reason input, and one destructive control labelled "Cancel Prescription".
8. IF a non-Doctor authenticated user requests any route under `/doctor/prescriptions`, THEN THE System SHALL resolve the request to AUTH-04 per Requirement 1.6.

### Requirement 7: Update Prescription Status UI (UCD-07)

**User Story:** As a Doctor, I want to change a prescription's clinical lifecycle state using only permitted transitions, so that the clinical status remains valid.

#### Acceptance Criteria

1. WHEN an authenticated Doctor requests `GET /doctor/prescriptions/{id}/status` for a prescription owned by that Doctor, THE System SHALL render the PST-01 Clinical Status Management UI_Surface containing a Status_Badge for the current Clinical_Status, an issue/expiry summary, and one control per allowed transition returned by `UpdatePrescriptionStatusController.getAllowedTransitions(id)`.
2. WHEN the PST-01 UI_Surface renders for a prescription whose current status has no allowed transitions, THE System SHALL render an Inline_Alert using `.alert-warning` explaining that no transitions are permitted and SHALL render zero transition control buttons.
3. WHEN a Doctor selects a transition control on PST-01, THE System SHALL render the PST-02 Status Transition UI_Surface as a Critical_Confirmation showing the current Clinical_Status, the target Clinical_Status, and a reason input.
4. IF a PST-02 submission targets a status that requires a reason and the submitted reason is empty or whitespace-only, THEN THE System SHALL render PST-02 again with a Field_Error on the reason input and SHALL NOT invoke the status change.
5. IF a PST-02 submission targets ON_HOLD and the submitting user is not a Doctor, THEN THE System SHALL resolve the request to AUTH-04 per Requirement 1.6.
6. WHEN a PST-02 submission is rejected by the backend because of an invalid transition or a version conflict, THE System SHALL render PST-02 again with an Inline_Alert using `.alert-danger` explaining the failure and one control labelled "Reload latest" that issues a new `GET /doctor/prescriptions/{id}/status`.

### Requirement 8: View Prescription Status UI (UCD-02)

**User Story:** As a Patient, I want to view my own prescriptions and their current clinical and fulfilment status in read-only form, so that I understand the progress of my medication.

#### Acceptance Criteria

1. WHEN an authenticated Patient requests `GET /patient/prescriptions`, THE System SHALL render the PTR-01 My Prescriptions UI_Surface containing a `.data-table` or card list of the Patient's own prescriptions with columns for issue date, Doctor, medication summary, a Clinical_Status badge, and a separate Fulfilment_Status badge.
2. WHEN the PTR-01 UI_Surface renders, THE System SHALL exclude every prescription whose current Clinical_Status is DRAFT and SHALL NOT provide any control that would edit, cancel, or dispense a prescription.
3. WHEN an authenticated Patient requests `GET /patient/prescriptions/{id}` for a prescription owned by the authenticated Patient, THE System SHALL render the PTR-02 Prescription Status Details UI_Surface as a read-only page with medication instructions and a Fulfilment_Status timeline.
4. IF an authenticated Patient requests `GET /patient/prescriptions/{id}` for a prescription not owned by the authenticated Patient or with a substituted identifier, THEN THE System SHALL resolve the request to a not-found response using the `.alert-warning` styling within the Patient shell, and SHALL NOT render any of the requested prescription's data.
5. WHEN a Prescription's Clinical_Status is EXPIRED or CANCELLED, THE PTR-02 UI_Surface SHALL render the Clinical_Status area with a non-dismissable Inline_Alert explaining that the prescription cannot be fulfilled.

### Requirement 9: Notifications UI (UCD-03)

**User Story:** As a Patient, I want to see my persisted notifications and read their full content, so that I can review important updates about my prescriptions.

#### Acceptance Criteria

1. WHEN an authenticated Patient requests `GET /patient/notifications`, THE System SHALL render the NOT-01 Notification Centre UI_Surface containing a chronological list of the Patient's own notifications with an unread-emphasis treatment on unread items (bolder text weight and a dot indicator).
2. WHEN the NOT-01 UI_Surface renders and any Notification has failed delivery, THE System SHALL render the failure state as an inline metadata label on the affected row using `.status-danger` and SHALL NOT hide the notification.
3. WHEN an authenticated Patient selects a notification row, THE System SHALL issue `POST /patient/notifications/{id}/read` following the PRG_Pattern and SHALL then render the NOT-02 Notification Detail UI_Surface with the full title, message body, event type, and related prescription reference.
4. WHEN the NOT-02 UI_Surface renders and a related prescription reference exists and is visible to the authenticated Patient, THE System SHALL render one control labelled with the prescription identifier that links to `GET /patient/prescriptions/{prescriptionId}`.
5. IF an authenticated Patient requests `GET /patient/notifications/{id}` for a notification not owned by the authenticated Patient, THEN THE System SHALL resolve the request to a not-found response and SHALL NOT render any notification data.

### Requirement 10: Dispensing UI (UCD-08)

**User Story:** As a Pharmacist, I want to see the queue of eligible prescriptions, verify patient and stock, and complete a full-quantity dispensing transaction with a clear result, so that medication is handed over accurately.

#### Acceptance Criteria

1. WHEN an authenticated Pharmacist requests `GET /pharmacy/dispensing`, THE System SHALL render the DISP-01 Dispensing Queue UI_Surface containing a `.data-table` of eligible prescriptions with columns for patient, prescription identifier, medication summary, clinical eligibility, and Fulfilment_Status, together with a filter toolbar.
2. WHEN the DISP-01 UI_Surface renders a prescription that is CANCELLED, EXPIRED, or already fully Dispensed, THE System SHALL render that row with a non-actionable state (no "Dispense" control) and a Status_Badge explaining the block reason.
3. WHEN an authenticated Pharmacist opens an eligible prescription, THE System SHALL render the DISP-02 Verification & Dispensing UI_Surface as a workflow page with a stepper (Verify Patient → Verify Medication → Check Stock → Confirm Handover), a right-aligned sticky transaction summary showing every Prescription_Item and its full required quantity, and a FEFO batch allocation preview when multiple batches would be used.
4. WHEN the DISP-02 UI_Surface renders and the aggregate Eligible_Stock across all eligible non-expired batches is less than the full required quantity for any Prescription_Item, THE System SHALL render an Inline_Alert using `.alert-danger` labelled "Insufficient Stock" and SHALL disable the "Confirm Handover" control.
5. THE DISP-02 UI_Surface SHALL NOT render any editable input for prescribed quantity and SHALL NOT provide any control that would submit a partial quantity for dispensing.
6. WHEN an authenticated Pharmacist submits `POST /pharmacy/dispensing/{dispenseId}/confirm` and the transaction commits successfully, THE System SHALL redirect to `GET /pharmacy/dispensing/{dispenseId}/result` following the PRG_Pattern.
7. WHEN the DISP-03 Dispensing Result UI_Surface renders after a successful transaction, THE System SHALL display a `.alert-success` outcome banner containing the DispenseRecord identifier, patient identity, dispensed medications with quantities, and the transaction timestamp, and SHALL provide one control labelled "Back to Queue" linking to `GET /pharmacy/dispensing`.
8. WHEN the DISP-03 UI_Surface renders after a failed transaction, THE System SHALL display a `.alert-danger` outcome banner explaining the failure, SHALL NOT display any success language, and SHALL provide one control labelled "Back to Verification" linking to `GET /pharmacy/dispensing/{dispenseId}/verify`.

### Requirement 11: Medicine Inventory UI (UCD-10)

**User Story:** As a Pharmacist, I want to view the inventory, add and edit medicines, and receive or adjust stock with a mandatory reason for adjustments, so that inventory records stay accurate.

#### Acceptance Criteria

1. WHEN an authenticated Pharmacist requests `GET /pharmacy/inventory`, THE System SHALL render the INV-01 Inventory List UI_Surface containing a `.data-table` of medicines with columns for medicine code, name, formulation, dispensable stock, nearest expiry, and one trailing action column, together with a filter toolbar and one primary control labelled "Add Medicine" linking to `GET /pharmacy/inventory/new`.
2. WHEN the INV-01 UI_Surface renders a medicine whose aggregate dispensable stock (excluding expired batches) is at or below its reorder level, THE System SHALL render the stock column with a Status_Badge using `.status-warning` and the text "Low".
3. WHEN an authenticated Pharmacist requests `GET /pharmacy/inventory/{medicineId}`, THE System SHALL render the INV-02 Medicine/Batch Details UI_Surface containing a medicine summary, an aggregate dispensable stock figure, a batch `.data-table` with columns for batch identifier, expiry date, quantity on hand, and eligibility, and one collapsible movement-history section.
4. WHEN the INV-02 batch table renders a batch whose expiry date is earlier than the current system date, THE System SHALL render that row with a Status_Badge using `.status-danger` and the text "Expired", and SHALL NOT include that batch's quantity in the aggregate dispensable stock figure.
5. WHEN an authenticated Pharmacist requests `GET /pharmacy/inventory/{id}/stock`, THE System SHALL render the INV-04 Receive/Adjust Stock UI_Surface as a right drawer or modal bound to `StockAdjustmentView` with an operation-type selector (Receive or Adjust), a quantity input, a batch selector when operation is Adjust, a batch-number and expiry-date input when operation is Receive, and a reason textarea.
6. IF an INV-04 submission with operation type Adjust contains a reason value that is empty or whitespace-only, THEN THE System SHALL render INV-04 again with a Field_Error on the reason input and SHALL NOT invoke any stock change.
7. IF an INV-04 submission would reduce an InventoryItem's balance below zero, THEN THE System SHALL render INV-04 again with an Inline_Alert using `.alert-danger` explaining insufficient stock and SHALL NOT invoke any stock change.
8. WHEN an INV-04 submission is being computed, THE System SHALL render side-by-side the current balance and the projected balance, updating the projected value as the quantity input changes through progressive JavaScript enhancement.

### Requirement 12: Reports UI (UCD-09)

**User Story:** As an Administrator, I want to select a report type, provide criteria, view the persisted snapshot result, and export that snapshot as PDF, so that I can review operational information.

#### Acceptance Criteria

1. WHEN an authenticated Administrator requests `GET /admin/reports`, THE System SHALL render the REP-01 Reports Home / Saved Reports UI_Surface containing exactly four report-type command cards (Prescription, Dispensing, Inventory, User/Access) linking to `GET /admin/reports/new`, together with a `.data-table` of saved reports with columns for report type, generated timestamp, generated-by identifier, criteria summary, and row count.
2. WHEN an authenticated Administrator requests `GET /admin/reports/new`, THE System SHALL render the REP-02 Report Criteria UI_Surface as a form bound to `ReportCriteriaView` with a report-type selector, a start-date input, an end-date input, type-specific filter inputs supported by the selected report type, and one primary control labelled "Generate".
3. IF a REP-02 submission has an end-date value earlier than its start-date value, THEN THE System SHALL render REP-02 again with a Field_Error on the date range and SHALL NOT invoke report generation.
4. WHEN a REP-02 submission generates zero matching records, THE System SHALL persist an empty Report snapshot and issue an HTTP 302 redirect to `GET /admin/reports/{reportId}` following the PRG_Pattern.
5. WHEN an authenticated Administrator requests `GET /admin/reports/{reportId}`, THE System SHALL render the REP-03 Report Result / Snapshot UI_Surface containing the report title, the original criteria, the generated timestamp, the row count, an appropriate summary or `.data-table` of the saved snapshot content, and one control labelled "Export PDF" linking to `GET /admin/reports/{reportId}/export`.
6. WHEN the REP-03 UI_Surface renders for a snapshot with zero rows, THE System SHALL render an Empty_State inside the result region explaining that no records matched the criteria and SHALL preserve the "Export PDF" control.
7. IF a non-Administrator authenticated user requests any route under `/admin/reports`, THEN THE System SHALL resolve the request to AUTH-04 per Requirement 1.6.

### Requirement 13: Form Validation, Feedback, and Concurrency Handling

**User Story:** As any authenticated user submitting a form, I want clear inline validation, safe preservation of my entered values on failure, and honest feedback when the underlying record has changed, so that I do not lose work or overwrite someone else's changes.

#### Acceptance Criteria

1. WHEN any JSP_View renders a form that binds to a Boundary_View_Class, THE System SHALL include the CSRF_Token as a hidden input in every `<form method="post">` and SHALL reject any POST that lacks a valid token.
2. IF a form submission fails server-side validation, THEN THE System SHALL render the same JSP_View again with a Field_Error adjacent to each invalid field, a summary Inline_Alert using `.alert-danger` when two or more fields fail, and every non-sensitive entered value preserved.
3. WHEN a form submission fails server-side validation, THE System SHALL clear every password field before re-rendering, and SHALL move keyboard focus to the first invalid field on page load using an `autofocus` attribute on that field.
4. WHEN a form submission fails with a version-conflict error returned by any `*Storage.update(record, expectedVersion)` call, THE System SHALL render the same JSP_View again with an Inline_Alert using `.alert-warning` containing the text "Record has changed" and one control labelled "Reload latest" that issues a fresh GET for the record.
5. WHEN a form submission succeeds, THE System SHALL follow the PRG_Pattern by issuing an HTTP 302 redirect to a GET route and SHALL render a one-time success flash on that GET using `.alert-success`.
6. WHEN a mutation is in progress on a submitted form, THE System SHALL disable the submit control by setting the `disabled` attribute through a progressive-enhancement JavaScript handler until the response is received, and the server SHALL treat any duplicate POST with the same idempotency key or version token as a single logical operation.

### Requirement 14: Loading, Empty, Not-Found, and Session-Expired States

**User Story:** As any authenticated user, I want distinguishable feedback for initial loading, empty results, filtered-empty results, not-found records, session expiry, and permission denial, so that I can react to each situation correctly.

#### Acceptance Criteria

1. WHEN a JSP_View renders any `.data-table` region backed by a query that has completed with zero records and no active filters, THE System SHALL render an Empty_State containing an explanatory heading, a short description, and at most one primary control appropriate to the Operational_Role.
2. WHEN a JSP_View renders any `.data-table` region backed by a query with active filters that returned zero records, THE System SHALL render a Filtered_Empty_State that keeps the active filter values displayed and provides one control labelled "Clear Filters".
3. WHEN a JSP_View is fetched over HTTPS and the server has not yet returned the response, THE System SHALL rely on the browser's default page-load indicator and SHALL NOT render any partial or fake data.
4. WHEN a JSP_View is requested for a record identifier that does not exist in the relevant Storage_Class, THE System SHALL render a Not_Found variant of the containing UI_Surface with an Inline_Alert explaining that the requested record could not be retrieved, and one link to the parent list route.
5. IF an authenticated user attempts a protected mutation more than one hour after Session_Context establishment, THEN THE System SHALL redirect to `GET /login` with a `returnTo` query parameter carrying the intended route, and SHALL NOT execute the requested mutation.
6. WHEN a JSP_View renders during initial server-side page load, THE System SHALL NOT render any Loading_Skeleton for content that is already available in the response.

### Requirement 15: Accessibility

**User Story:** As a user of assistive technology, I want the UI to be operable with a keyboard, correctly labelled for screen readers, and legible without reliance on colour, so that I can use every feature independently.

#### Acceptance Criteria

1. WHEN any JSP_View renders a form input, THE System SHALL associate the input with a persistent `<label>` element through matching `for`/`id` attributes, and SHALL NOT rely on a placeholder attribute as the sole label.
2. WHEN any JSP_View renders a data table, THE System SHALL use `<th scope="col">` for every column header and SHALL include a `<caption>` or an `aria-label` on the `<table>` element identifying the table content.
3. WHEN any JSP_View renders a Status_Badge, THE System SHALL include human-readable text describing the status inside the badge element, and SHALL NOT encode status meaning solely through the CSS variant colour.
4. WHEN any JSP_View renders a Field_Error, THE System SHALL associate the error message with the offending input using an `aria-describedby` attribute pointing to the error element's `id`.
5. WHEN any JSP_View renders an interactive control (button, link, form field, or menu), THE System SHALL preserve keyboard focus visibility through the Design_System's `:focus-visible` outline and SHALL NOT set `outline: none` on any focusable element.
6. WHEN any JSP_View renders a Critical_Confirmation modal, THE System SHALL trap keyboard focus inside the modal while it is open, SHALL return focus to the invoking control on close, and SHALL support closing with the Escape key when the modal action is not destructive or in-progress.
7. THE System SHALL preserve the semantic source order sidebar → top header → page heading → primary task → secondary content in every JSP_View, and SHALL NOT reorder that sequence through CSS such that assistive technology receives a different order.

### Requirement 16: Responsive Desktop Behaviour

**User Story:** As a user on a desktop display between 1280 and 1920 pixels wide, I want the interface to remain usable when the window is narrower than the reference frame, so that I do not lose access to primary data or actions.

#### Acceptance Criteria

1. WHEN the viewport width is 1440 pixels or greater, THE App_Shell SHALL render the sidebar at 260 pixels wide and use 32-pixel page padding as defined by `--pc-sidebar-width` and `--pc-page-x` in the Design_System.
2. WHEN the viewport width is between 1101 and 1440 pixels, THE App_Shell SHALL render the sidebar and workspace at the same 1440-pixel proportions without introducing new breakpoints.
3. WHEN the viewport width is at or below 1100 pixels, THE App_Shell SHALL apply the Design_System's built-in 220-pixel sidebar and 24-pixel page padding rules, and every `.data-table` SHALL become horizontally scrollable inside its `.table-wrap` container while preserving the 900-pixel minimum table width defined by the Design_System.
4. WHEN a JSP_View renders a form with a `.form-grid` container, THE System SHALL use the Design_System's 12-column grid at all supported viewport widths and SHALL NOT redefine grid column counts inside individual pages.
5. THE System SHALL NOT introduce mobile navigation patterns (bottom tab bars, hamburger menus that collapse the sidebar below 1101 pixels) because the target platform is a desktop application.

### Requirement 17: Controller and Route Integration

**User Story:** As a maintainer, I want every JSP_View to be served by the existing Fixed_Controller for its owning UCD, bound to existing Boundary_View_Classes, and never to introduce parallel Java architectural layers, so that the fixed structure defined in the Implementation Plan is preserved.

#### Acceptance Criteria

1. THE System SHALL serve every route listed in Section 21 of the Implementation Plan through the Fixed_Controller named in that table, and SHALL NOT introduce any new Spring `@Controller` class outside the existing `controller.security_user`, `controller.clinical_prescription`, `controller.patient_information`, `controller.pharmacy_operations`, `controller.management_dss`, or `controller.common` packages.
2. WHEN a Fixed_Controller handles a GET route that renders a form, THE System SHALL populate a Boundary_View_Class instance (for example `PrescriptionFormView`, `ProfileFormView`, `UserAccountFormView`, `DispenseFormView`, `MedicineFormView`, `StockAdjustmentView`, `ReportCriteriaView`) and expose it as a Spring `@ModelAttribute` for form binding.
3. THE System SHALL NOT introduce any Java package named `service`, `dto`, `repository`, `mapper`, `config`, `facade`, or `usecase` under `pharmacy_system`, and SHALL NOT create classes whose sole purpose is to duplicate an existing Boundary_View_Class as a DTO.
4. WHEN a JSP_View needs presentation-only helper state that is not provided by an existing Boundary_View_Class, THE System SHALL place that helper class only under `view/.../components/` in the same UCD package as the owning View.
5. WHEN a Fixed_Controller persists a mutation, THE System SHALL invoke the corresponding Storage_Class method (for example `PrescriptionStorage.update(prescription, expectedVersion)`, `InventoryStorage.adjustStock(...)`, `UserAccountStorage.update(account, expectedVersion)`) and SHALL NOT bypass the Storage_Class layer.
6. THE System SHALL configure Spring MVC view resolution through `application.properties` (`spring.mvc.view.prefix=/WEB-INF/jsp/` and `spring.mvc.view.suffix=.jsp`), and SHALL NOT introduce a Java `config` package to encode this configuration.
7. THE System SHALL NOT execute any domain-rule logic (prescription expiry evaluation, FEFO allocation, stock deduction, transition-permission calculation, session expiry evaluation) inside a JSP scriptlet, and SHALL retrieve every such derived value from the Fixed_Controller's model.
8. WHEN a Fixed_Controller handles a mutation POST route, THE System SHALL check the Session_Context permission through `SessionController.requirePermission(...)` before invoking any Storage_Class mutation, and SHALL rely on server-side permission checks rather than only on hiding UI controls.

## Correctness Properties

These are universally-quantified properties intended for property-based testing of the UI implementation. Each property maps to one or more acceptance criteria and is expressed in terms of rendered HTML and controller behaviour rather than internal data structures.

### Property 1: Role-Filtered Navigation Invariant

For all authenticated sessions and all rendered pages, the set of navigation items present in the sidebar equals the set of navigation items whose destination route is a Permitted_Function for the session's Operational_Role.

`∀ session, page. renderedNavItems(page, session) = { item | permitted(session.role, item.route) }`

**Validates: Requirements 1.2, 1.3**

### Property 2: Protected-Route Access Control

For all HTTP requests to any route other than `/login`, `/password/recovery`, `/password/reset`, and `/access-denied`, if the request carries no valid Session_Context, then the response is an HTTP 302 to `/login`; and if the request carries a Session_Context whose Operational_Role lacks permission for the requested route, then the response renders AUTH-04.

`∀ req. (¬authenticated(req) ∧ req.path ∉ publicRoutes) ⟹ response(req).status = 302 ∧ response(req).location = "/login"`

**Validates: Requirements 1.5, 1.6, 3.8, 5.8, 6.8, 7.5, 8.4, 9.5, 12.7**

### Property 3: Design System Exclusivity

For all rendered JSP_Views, every button element uses the `.btn` Component_Class combined with exactly one Design_System variant, every table uses the `.data-table` Component_Class, and every status indicator uses the `.status` Component_Class combined with exactly one variant.

`∀ page, btn ∈ buttons(page). classes(btn) ⊇ {".btn"} ∧ |classes(btn) ∩ btnVariants| = 1`

**Validates: Requirements 2.2, 2.3, 2.4, 2.5**

### Property 4: No Inline Style Overrides

For all rendered JSP_Views, no HTML element carries a `style` attribute whose property list intersects the set of properties defined by `pharmacare.css` Design_Tokens (colour, background, border, radius, font-family, box-shadow).

`∀ page, el ∈ elements(page). properties(el.style) ∩ tokenProperties = ∅`

**Validates: Requirement 2.8**

### Property 5: Draft Prescription Invisibility to Patient

For all authenticated Patient sessions, no rendered PTR-01 UI_Surface contains a prescription row whose Clinical_Status is DRAFT, and no `GET /patient/prescriptions/{id}` request for a DRAFT prescription returns a response that renders the prescription data.

`∀ session ∈ patientSessions, page = renderPTR01(session). ¬∃ p ∈ page.prescriptions. p.status = DRAFT`

**Validates: Requirement 8.2**

### Property 6: Patient Prescription Isolation

For all authenticated Patient sessions and all prescription identifiers `id`, if the prescription's `patientId` differs from the session's patient identifier, then the response to `GET /patient/prescriptions/{id}` is a not-found variant and contains none of the prescription's medication or Doctor details.

`∀ session, id. prescription(id).patientId ≠ session.patientId ⟹ ¬renderedFields(response) ∩ prescriptionFields(id)`

**Validates: Requirement 8.4**

### Property 7: Cancel and Edit Controls Absent on Terminal States

For all rendered RX-02 UI_Surfaces where the prescription's Clinical_Status is CANCELLED or EXPIRED, no "Edit" control and no "Change Status" control appears in the rendered HTML.

`∀ page = renderRX02(p). p.status ∈ {CANCELLED, EXPIRED} ⟹ "Edit" ∉ actions(page) ∧ "Change Status" ∉ actions(page)`

**Validates: Requirement 6.4**

### Property 8: Full-Quantity Dispensing UI Constraint

For all rendered DISP-02 UI_Surfaces, no input element accepts a quantity value below the prescribed full required quantity, and the "Confirm Handover" control's `disabled` attribute is set whenever the aggregate Eligible_Stock for any item is less than that item's required quantity.

`∀ page = renderDISP02(rx, inv). insufficient(rx, inv) ⟹ confirmBtn(page).disabled = true`

**Validates: Requirements 10.4, 10.5**

### Property 9: PRG Pattern After Successful Mutation

For all successful POST responses served by a Fixed_Controller, the HTTP status is 302 and the `Location` header targets a GET route of the same or a related UI_Surface, and the response body does not directly render the mutated record.

`∀ req ∈ successfulPosts. response(req).status = 302 ∧ response(req).location.method = GET`

**Validates: Requirements 4.5, 10.6, 12.4, 13.5**

### Property 10: Adjustment Reason Required in UI

For all POST submissions to `/pharmacy/inventory/{id}/adjust` with a reason value that is empty or whitespace-only, the response re-renders the INV-04 UI_Surface with a Field_Error on the reason input and the underlying `InventoryStorage.adjustStock(...)` is never invoked.

`∀ req = adjust(reason). blank(reason) ⟹ renderedINV04(response).fieldErrors ∋ "reason" ∧ ¬invoked(adjustStock, req)`

**Validates: Requirement 11.6**

### Property 11: Negative-Balance Prevention in UI

For all POST submissions to `/pharmacy/inventory/{inventoryId}/adjust` whose signed quantity would reduce the InventoryItem balance below zero, the response re-renders the INV-04 UI_Surface with an `.alert-danger` Inline_Alert explaining insufficient stock and no stock mutation is executed.

`∀ req. projected(req) < 0 ⟹ response(req).alerts ∋ "insufficient" ∧ inv' = inv`

**Validates: Requirement 11.7**

### Property 12: Session Expiry Redirect Preservation

For all protected mutation requests received more than one hour after Session_Context establishment, the response is an HTTP 302 to `/login` with a `returnTo` query parameter equal to the requested path, and no mutation is invoked.

`∀ req. protectedMutation(req) ∧ sessionAge(req) > 1h ⟹ response.location = "/login?returnTo=" + urlEncode(req.path)`

**Validates: Requirement 14.5**

### Property 13: CSRF Token on Every Mutating Form

For all rendered JSP_Views containing a `<form method="post">`, the form contains a hidden input carrying the CSRF_Token; and for all POST requests without a valid CSRF_Token, the server response is HTTP 403.

`∀ page, form ∈ postForms(page). ∃ input ∈ form.hidden. input.name = csrfParamName ∧ validCsrf(input.value)`

**Validates: Requirement 13.1**

### Property 14: Empty vs Filtered-Empty Distinguishability

For all rendered list UI_Surfaces where the underlying query returned zero records, the presence of at least one active filter value implies that the rendered page contains a "Clear Filters" control; and the absence of any active filter value implies that the rendered page contains only the Operational_Role's primary create/generate control (where such a control is appropriate for the surface).

`∀ page. queryCount(page) = 0 ⟹ (hasActiveFilters(page) ⟺ "Clear Filters" ∈ controls(page))`

**Validates: Requirements 5.2, 5.3, 14.1, 14.2**

### Property 15: Status Semantic Text Presence

For all rendered Status_Badges, the badge element's text content is non-empty and includes at least one word from the status vocabulary (Draft, Issued, On Hold, Cancelled, Expired, Pending, Preparing, Ready, Dispensed, Active, Disabled, Locked, Pending, Normal, Low, Out, Expiring, Inactive).

`∀ page, badge ∈ statusBadges(page). textContent(badge) ≠ "" ∧ textContent(badge) ∩ statusVocab ≠ ∅`

**Validates: Requirements 2.4, 15.3**

### Property 16: Focus Visibility Invariant

For all rendered JSP_Views and all focusable elements (buttons, links, inputs, selects, textareas), the effective computed `outline` style on `:focus-visible` is non-zero width and matches the Design_System's `--pc-primary` colour.

`∀ page, el ∈ focusable(page). computedOutline(el, ":focus-visible").width > 0`

**Validates: Requirement 15.5**

### Property 17: Fixed Controller Route Ownership

For all HTTP routes served by the application, the handler class belongs to one of the packages `controller.security_user`, `controller.clinical_prescription`, `controller.patient_information`, `controller.pharmacy_operations`, `controller.management_dss`, or `controller.common`; and no Java class outside these packages carries a `@Controller` or `@RestController` annotation.

`∀ handler. package(handler) ∈ fixedControllerPackages`

**Validates: Requirement 17.1**

### Property 18: No Forbidden Java Layers

For all Java source files under `src/main/java/pharmacy_system/`, no top-level package name equals `service`, `dto`, `repository`, `mapper`, `config`, `facade`, or `usecase`.

`∀ src ∈ javaSources. topPackage(src) ∉ {"service", "dto", "repository", "mapper", "config", "facade", "usecase"}`

**Validates: Requirement 17.3**

### Property 19: JSP Business-Logic Absence

For all JSP source files under `src/main/webapp/WEB-INF/jsp/`, no `<% %>` scriptlet contains a domain method call (for example expiry evaluation, FEFO allocation, permission evaluation, storage access), and every dynamic value in the rendered HTML originates from an `${...}` EL expression or a JSTL tag reading from the controller-supplied model.

`∀ jsp. scriptletCalls(jsp) ∩ domainMethods = ∅`

**Validates: Requirement 17.7**

### Property 20: Semantic Source Order Invariance

For all rendered JSP_Views, the DOM order of the primary regions is sidebar → top header → page heading → primary task region → secondary/contextual region, and no CSS rule reorders those regions such that assistive technology receives a different sequence.

`∀ page. domOrder(page.regions) = [sidebar, topHeader, pageHeading, primary, secondary]`

**Validates: Requirement 15.7**
