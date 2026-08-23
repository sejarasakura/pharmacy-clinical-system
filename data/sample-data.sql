-- Idempotent development fixtures for PharmaCare.  Values are fictional.
-- Run with: sqlite3 data/pharmacare.db < data/sample-data.sql

PRAGMA foreign_keys = ON;

-- Login-capable sample identities. All use the development-only password Demo@12345.
INSERT OR IGNORE INTO user_accounts (username, email, status, approved, created_at, updated_at, last_login_at, disabled_at, disabled_reason, version) VALUES
  ('pharmacist.demo', 'pharmacist.demo@pharmacare.local', 'ACTIVE', 1, '2026-08-23T21:21:01', '2026-08-23T21:21:01', NULL, NULL, NULL, 1),
  ('aina.patient', 'aina.rahman@pharmacare.local', 'ACTIVE', 1, '2026-08-23T21:21:01', '2026-08-23T21:21:01', NULL, NULL, NULL, 1),
  ('daniel.patient', 'daniel.wong@pharmacare.local', 'ACTIVE', 1, '2026-08-23T21:21:01', '2026-08-23T21:21:01', NULL, NULL, NULL, 1),
  ('kavitha.patient', 'kavitha.devi@pharmacare.local', 'ACTIVE', 1, '2026-08-23T21:21:01', '2026-08-23T21:21:01', NULL, NULL, NULL, 1),
  ('faridah.patient', 'faridah.osman@pharmacare.local', 'ACTIVE', 1, '2026-08-23T21:21:01', '2026-08-23T21:21:01', NULL, NULL, NULL, 1),
  ('marcus.patient', 'marcus.lee@pharmacare.local', 'ACTIVE', 1, '2026-08-23T21:21:01', '2026-08-23T21:21:01', NULL, NULL, NULL, 1),
  ('nurul.patient', 'nurul.huda@pharmacare.local', 'ACTIVE', 1, '2026-08-23T21:21:01', '2026-08-23T21:21:01', NULL, NULL, NULL, 1);

INSERT OR IGNORE INTO credentials (user_id, password_hash, failed_attempts, locked_until, password_updated_at, reset_token_hash, reset_token_expiry, version)
SELECT user_id, password_hash, 0, NULL, '2026-08-23T21:21:01', NULL, NULL, 1
FROM (
  SELECT 'pharmacist.demo' AS username, 'mS6eOcQof8lU6zSMl33PgQ==:ntUgvpumX670c1w6FY6BQuxWVUIaA2uPbYnBAuBUcNM=' AS password_hash
  UNION ALL SELECT 'aina.patient', 'scJ9s5qPL62myo7jxy3AAA==:tz1ku01UtigQHbmHsjGvnh4TKpUriKzzi4xVpOOesb4='
  UNION ALL SELECT 'daniel.patient', 'yRMJDvDKkc28kACOaBb4tQ==:kNueQDU6RCOlff9SahNv4uLgN3m7yMqyE2MGWFMAPMU='
  UNION ALL SELECT 'kavitha.patient', 'OoPKDGXf+36e0COqBTsFqg==:7H1KvCp+jnzG+/9N/yDTSSgagcKQmTmUbcW41M4t6D4='
  UNION ALL SELECT 'faridah.patient', 'YyELFx4kJWoAXlcKIm/SqQ==:GKOMAftOhYMxxg+w7bfCes7zmHeSjRmo3Eps24Ml91o='
  UNION ALL SELECT 'marcus.patient', '2C9RBrBAJtMAKjUfjmeg4w==:q46qfbTjrpHRgNM9CDh3xvK0yXixf0qSWKSweB19mbk='
  UNION ALL SELECT 'nurul.patient', 'ZEg2/eY8AJNdA0CIgPe5jw==:HGXiNdcJZjW2UnoHhjkkGgZwt9Y5yU4UdV4A9rkLRmc='
) sample_credential
JOIN user_accounts USING (username);

INSERT OR IGNORE INTO user_roles (user_id, role_id)
SELECT ua.user_id, r.role_id
FROM user_accounts ua JOIN roles r
WHERE (ua.username = 'pharmacist.demo' AND r.role_name = 'Pharmacist')
   OR (ua.username IN ('aina.patient', 'daniel.patient', 'kavitha.patient', 'faridah.patient', 'marcus.patient', 'nurul.patient') AND r.role_name = 'Patient');

INSERT OR IGNORE INTO user_profiles (profile_id, user_id, profile_type, full_name, phone_number, contact_email, address, preferences_json, patient_identifier, date_of_birth, emergency_contact, professional_registration, specialization, department, branch, created_at, updated_at, version) VALUES
  (1, 1, 'Administrator', 'System Administrator', '+60 12-555 0100', 'admin@pharmacare.local', 'PharmaCare HQ, Kuching', '{"notificationChannel":"email"}', NULL, NULL, NULL, 'ADM-001', NULL, 'Operations', NULL, '2026-08-20T13:25:17', '2026-08-20T13:25:17', 1),
  (2, 2, 'Doctor', 'Dr Susie Lim', '+60 12-555 0101', 'saikeat.lim@mycareconcierge.com', 'Kuching Specialist Clinic', '{"notificationChannel":"email"}', NULL, NULL, NULL, 'MMC-123456', 'General Practice', 'Clinical', NULL, '2026-08-23T20:59:20', '2026-08-23T20:59:20', 1),
  (101, NULL, 'Patient', 'Aina Rahman', '+60 12-555 0201', 'aina.rahman@example.test', 'Tabuan Jaya, Kuching', '{"notificationChannel":"sms"}', 'PAT-2026-001', '1988-04-12', 'Rahman Ismail +60 12-555 0291', NULL, NULL, NULL, NULL, '2026-08-21T09:00:00', '2026-08-21T09:00:00', 1),
  (102, NULL, 'Patient', 'Daniel Wong', '+60 12-555 0202', 'daniel.wong@example.test', 'Batu Kawa, Kuching', '{"notificationChannel":"email"}', 'PAT-2026-002', '1975-09-28', 'Mei Wong +60 12-555 0292', NULL, NULL, NULL, NULL, '2026-08-21T09:15:00', '2026-08-21T09:15:00', 1),
  (103, NULL, 'Patient', 'Kavitha Devi', '+60 12-555 0203', 'kavitha.devi@example.test', 'Pending Road, Kuching', '{"notificationChannel":"email"}', 'PAT-2026-003', '1994-01-19', 'Suresh Kumar +60 12-555 0293', NULL, NULL, NULL, NULL, '2026-08-21T09:30:00', '2026-08-21T09:30:00', 1);

INSERT OR IGNORE INTO medicines (medicine_id, medicine_code, medicine_name, generic_name, dosage_form, strength, unit, description, active, created_at, updated_at, version) VALUES
  (101, 'MED-PCT-500', 'Panadol', 'Paracetamol', 'Tablet', '500 mg', 'tablets', 'Analgesic and antipyretic', 1, '2026-08-20T09:00:00', '2026-08-20T09:00:00', 1),
  (102, 'MED-AML-5', 'Amlodipine', 'Amlodipine besylate', 'Tablet', '5 mg', 'tablets', 'Calcium-channel blocker for hypertension', 1, '2026-08-20T09:05:00', '2026-08-20T09:05:00', 1),
  (103, 'MED-CTZ-10', 'Cetirizine', 'Cetirizine hydrochloride', 'Tablet', '10 mg', 'tablets', 'Non-sedating antihistamine', 1, '2026-08-20T09:10:00', '2026-08-20T09:10:00', 1),
  (104, 'MED-OMZ-20', 'Omeprazole', 'Omeprazole', 'Capsule', '20 mg', 'capsules', 'Proton-pump inhibitor', 1, '2026-08-20T09:15:00', '2026-08-20T09:15:00', 1);

INSERT OR IGNORE INTO inventory_items (inventory_id, medicine_id, batch_number, expiry_date, quantity_on_hand, reorder_level, version) VALUES
  (101, 101, 'PCT-260501', '2027-05-31', 460, 100, 1),
  (102, 102, 'AML-260301', '2027-03-31', 84, 100, 1),
  (103, 103, 'CTZ-260701', '2027-07-31', 210, 75, 1),
  (104, 104, 'OMZ-260401', '2027-04-30', 150, 60, 1);

INSERT OR IGNORE INTO prescriptions (prescription_id, patient_id, doctor_id, clinical_notes, status, issued_at, status_changed_at, status_changed_by, status_change_reason, created_at, updated_at, cancelled_at, cancellation_reason, version) VALUES
  (101, 101, 2, 'Take with meals where indicated; review if symptoms persist.', 'ISSUED', '2026-08-21T10:00:00', '2026-08-21T10:00:00', 2, 'Prescription issued', '2026-08-21T09:55:00', '2026-08-21T10:00:00', NULL, NULL, 2),
  (102, 102, 2, 'Monitor blood pressure and arrange follow-up in four weeks.', 'ISSUED', '2026-08-22T14:30:00', '2026-08-22T14:30:00', 2, 'Prescription issued', '2026-08-22T14:20:00', '2026-08-22T14:30:00', NULL, NULL, 2),
  (103, 103, 2, 'Seasonal allergy management.', 'ON_HOLD', '2026-08-22T16:00:00', '2026-08-23T09:10:00', 2, 'Awaiting allergy review', '2026-08-22T15:50:00', '2026-08-23T09:10:00', NULL, NULL, 3);

INSERT OR IGNORE INTO prescription_items (prescription_item_id, prescription_id, medicine_id, medicine_name, dosage, dosage_unit, frequency, route, duration_days, instructions, quantity) VALUES
  (101, 101, 101, 'Panadol', '500', 'mg', 'three times daily when required', 'oral', 5, 'Do not exceed 4 g per day.', 15),
  (102, 101, 104, 'Omeprazole', '20', 'mg', 'once daily', 'oral', 14, 'Take 30 minutes before breakfast.', 14),
  (103, 102, 102, 'Amlodipine', '5', 'mg', 'once daily', 'oral', 28, 'Take at the same time each day.', 28),
  (104, 103, 103, 'Cetirizine', '10', 'mg', 'once daily when required', 'oral', 14, 'May cause drowsiness.', 14);

INSERT OR IGNORE INTO dispense_records (dispense_id, prescription_id, patient_id, pharmacist_id, required_quantities_json, dispensed_quantities_json, status, verified_at, dispensed_at, created_at, updated_at, failure_reason, version) VALUES
  (101, 101, 101, 1, '{"101":15,"104":14}', '{"101":15,"104":14}', 'DISPENSED', '2026-08-21T10:20:00', '2026-08-21T10:25:00', '2026-08-21T10:15:00', '2026-08-21T10:25:00', NULL, 3),
  (102, 102, 102, NULL, '{"102":28}', '{}', 'PENDING', NULL, NULL, '2026-08-22T14:35:00', '2026-08-22T14:35:00', NULL, 1);

INSERT OR IGNORE INTO stock_movements (movement_id, inventory_id, medicine_id, movement_type, quantity_delta, balance_after, reason, performed_by, created_at) VALUES
  (101, 101, 101, 'DISPENSE', -15, 460, 'Dispensed against prescription 101', 1, '2026-08-21T10:25:00'),
  (102, 104, 104, 'DISPENSE', -14, 150, 'Dispensed against prescription 101', 1, '2026-08-21T10:25:00'),
  (103, 102, 102, 'ADJUSTMENT', -16, 84, 'Cycle-count reconciliation', 1, '2026-08-23T08:30:00');

INSERT OR IGNORE INTO notifications (notification_id, patient_id, prescription_id, event_type, notification_type, title, message, recipient, delivery_status, deduplication_key, created_at, delivered_at, failed_at, read_at, failure_reason) VALUES
  (101, 101, 101, 'PRESCRIPTION_ISSUED', 'INFO', 'Prescription issued', 'Your prescription is ready for pharmacy processing.', 'aina.rahman@example.test', 'DELIVERED', '101:101:PRESCRIPTION_ISSUED', '2026-08-21T10:00:00', '2026-08-21T10:01:00', NULL, '2026-08-21T11:00:00', NULL),
  (102, 101, 101, 'MEDICATION_DISPENSED', 'SUCCESS', 'Medication dispensed', 'Your medicines have been dispensed.', 'aina.rahman@example.test', 'DELIVERED', '101:101:MEDICATION_DISPENSED', '2026-08-21T10:25:00', '2026-08-21T10:26:00', NULL, NULL, NULL),
  (103, 102, 102, 'PRESCRIPTION_ISSUED', 'INFO', 'Prescription issued', 'Your prescription is ready for collection.', 'daniel.wong@example.test', 'DELIVERED', '102:102:PRESCRIPTION_ISSUED', '2026-08-22T14:30:00', '2026-08-22T14:31:00', NULL, NULL, NULL);

INSERT OR IGNORE INTO reports (report_id, report_type, title, criteria_json, generated_by, generated_at, row_count, data_json, version) VALUES
  (101, 'Inventory', 'Inventory status — August 2026', '{"reportType":"Inventory","startDate":"2026-08-01","endDate":"2026-08-23","filters":{"lowStock":"true"}}', 1, '2026-08-23T09:00:00', 1, '[{"medicineCode":"MED-AML-5","medicineName":"Amlodipine","quantityOnHand":84,"reorderLevel":100}]', 1),
  (102, 'Dispensing', 'Dispensing activity — August 2026', '{"reportType":"Dispensing","startDate":"2026-08-01","endDate":"2026-08-23","filters":{}}', 1, '2026-08-23T09:05:00', 2, '[{"prescriptionId":101,"status":"DISPENSED"},{"prescriptionId":102,"status":"PENDING"}]', 1);

-- Extended operational records: additional cases for stock, prescription, dispensing,
-- notification, and report filters.  All people and contact details are fictional.
INSERT OR IGNORE INTO user_profiles (profile_id, user_id, profile_type, full_name, phone_number, contact_email, address, preferences_json, patient_identifier, date_of_birth, emergency_contact, professional_registration, specialization, department, branch, created_at, updated_at, version) VALUES
  (104, NULL, 'Patient', 'Faridah Osman', '+60 12-555 0204', 'faridah.osman@example.test', 'Petra Jaya, Kuching', '{"notificationChannel":"sms"}', 'PAT-2026-004', '1968-06-03', 'Hafiz Osman +60 12-555 0294', NULL, NULL, NULL, NULL, '2026-08-22T09:00:00', '2026-08-22T09:00:00', 1),
  (105, NULL, 'Patient', 'Marcus Lee', '+60 12-555 0205', 'marcus.lee@example.test', 'Samarahan, Sarawak', '{"notificationChannel":"email"}', 'PAT-2026-005', '1981-11-16', 'Grace Lee +60 12-555 0295', NULL, NULL, NULL, NULL, '2026-08-22T09:15:00', '2026-08-22T09:15:00', 1),
  (106, NULL, 'Patient', 'Nurul Huda', '+60 12-555 0206', 'nurul.huda@example.test', 'Matang Jaya, Kuching', '{"notificationChannel":"email"}', 'PAT-2026-006', '2000-02-27', 'Huda Karim +60 12-555 0296', NULL, NULL, NULL, NULL, '2026-08-22T09:30:00', '2026-08-22T09:30:00', 1);

INSERT OR IGNORE INTO medicines (medicine_id, medicine_code, medicine_name, generic_name, dosage_form, strength, unit, description, active, created_at, updated_at, version) VALUES
  (105, 'MED-MTF-500', 'Metformin', 'Metformin hydrochloride', 'Tablet', '500 mg', 'tablets', 'Biguanide for type 2 diabetes', 1, '2026-08-20T09:20:00', '2026-08-20T09:20:00', 1),
  (106, 'MED-ATV-20', 'Atorvastatin', 'Atorvastatin calcium', 'Tablet', '20 mg', 'tablets', 'HMG-CoA reductase inhibitor', 1, '2026-08-20T09:25:00', '2026-08-20T09:25:00', 1),
  (107, 'MED-SAL-100', 'Salbutamol inhaler', 'Salbutamol sulfate', 'Inhaler', '100 mcg/dose', 'inhalers', 'Short-acting bronchodilator', 1, '2026-08-20T09:30:00', '2026-08-20T09:30:00', 1);

INSERT OR IGNORE INTO inventory_items (inventory_id, medicine_id, batch_number, expiry_date, quantity_on_hand, reorder_level, version) VALUES
  (105, 105, 'MTF-260601', '2027-06-30', 320, 120, 1),
  (106, 106, 'ATV-260402', '2027-04-30', 52, 80, 1),
  (107, 107, 'SAL-260801', '2027-08-31', 24, 30, 1),
  (108, 101, 'PCT-260801', '2027-08-31', 300, 100, 1);

INSERT OR IGNORE INTO prescriptions (prescription_id, patient_id, doctor_id, clinical_notes, status, issued_at, status_changed_at, status_changed_by, status_change_reason, created_at, updated_at, cancelled_at, cancellation_reason, version) VALUES
  (104, 104, 3, 'Continue long-term cardiovascular medicines; medication counselling required.', 'ISSUED', '2026-08-23T10:30:00', '2026-08-23T10:30:00', 3, 'Prescription issued', '2026-08-23T10:20:00', '2026-08-23T10:30:00', NULL, NULL, 2),
  (105, 105, 2, 'Draft treatment plan pending laboratory results.', 'DRAFT', NULL, NULL, NULL, NULL, '2026-08-23T11:10:00', '2026-08-23T11:10:00', NULL, NULL, 1),
  (106, 106, 3, 'Prescription superseded after a medication-history review.', 'CANCELLED', '2026-08-22T12:00:00', '2026-08-23T08:45:00', 3, 'Medication history changed', '2026-08-22T11:50:00', '2026-08-23T08:45:00', '2026-08-23T08:45:00', 'Medication history changed', 3);

INSERT OR IGNORE INTO prescription_items (prescription_item_id, prescription_id, medicine_id, medicine_name, dosage, dosage_unit, frequency, route, duration_days, instructions, quantity) VALUES
  (105, 104, 105, 'Metformin', '500', 'mg', 'twice daily', 'oral', 28, 'Take with food.', 56),
  (106, 104, 106, 'Atorvastatin', '20', 'mg', 'once nightly', 'oral', 28, 'Take in the evening.', 28),
  (107, 105, 107, 'Salbutamol inhaler', '100', 'mcg/dose', 'one to two puffs when required', 'inhaled', 30, 'Use with a spacer if available.', 1),
  (108, 106, 103, 'Cetirizine', '10', 'mg', 'once daily when required', 'oral', 7, 'Cancelled before dispensing.', 7);

INSERT OR IGNORE INTO dispense_records (dispense_id, prescription_id, patient_id, pharmacist_id, required_quantities_json, dispensed_quantities_json, status, verified_at, dispensed_at, created_at, updated_at, failure_reason, version) VALUES
  (103, 104, 104, 1, '{"105":56,"106":28}', '{}', 'VERIFIED', '2026-08-23T10:45:00', NULL, '2026-08-23T10:40:00', '2026-08-23T10:45:00', NULL, 2);

INSERT OR IGNORE INTO stock_movements (movement_id, inventory_id, medicine_id, movement_type, quantity_delta, balance_after, reason, performed_by, created_at) VALUES
  (104, 105, 105, 'RECEIVE', 320, 320, 'Initial stock receipt', 1, '2026-08-20T11:00:00'),
  (105, 106, 106, 'RECEIVE', 80, 80, 'Initial stock receipt', 1, '2026-08-20T11:05:00'),
  (106, 106, 106, 'ADJUSTMENT', -28, 52, 'Damaged stock removed after inspection', 1, '2026-08-23T08:50:00'),
  (107, 107, 107, 'RECEIVE', 24, 24, 'Initial stock receipt', 1, '2026-08-20T11:10:00');

INSERT OR IGNORE INTO notifications (notification_id, patient_id, prescription_id, event_type, notification_type, title, message, recipient, delivery_status, deduplication_key, created_at, delivered_at, failed_at, read_at, failure_reason) VALUES
  (104, 104, 104, 'PRESCRIPTION_ISSUED', 'INFO', 'Prescription issued', 'Your prescription is being prepared by the pharmacy.', 'faridah.osman@example.test', 'DELIVERED', '104:104:PRESCRIPTION_ISSUED', '2026-08-23T10:30:00', '2026-08-23T10:31:00', NULL, NULL, NULL),
  (105, 104, 104, 'MEDICATION_PREPARING', 'INFO', 'Medication being prepared', 'Your prescription has passed identity verification.', 'faridah.osman@example.test', 'DELIVERED', '104:104:MEDICATION_PREPARING', '2026-08-23T10:45:00', '2026-08-23T10:46:00', NULL, NULL, NULL),
  (106, 106, 106, 'PRESCRIPTION_CANCELLED', 'WARNING', 'Prescription cancelled', 'Please contact the clinic if you have any questions.', 'nurul.huda@example.test', 'DELIVERED', '106:106:PRESCRIPTION_CANCELLED', '2026-08-23T08:45:00', '2026-08-23T08:46:00', NULL, '2026-08-23T09:05:00', NULL),
  (107, 102, 102, 'MEDICATION_READY_FOR_COLLECTION', 'SUCCESS', 'Medication ready for collection', 'Please bring a valid form of identification to the pharmacy.', 'daniel.wong@example.test', 'PENDING', '102:102:MEDICATION_READY_FOR_COLLECTION', '2026-08-23T11:00:00', NULL, NULL, NULL, NULL);

INSERT OR IGNORE INTO reports (report_id, report_type, title, criteria_json, generated_by, generated_at, row_count, data_json, version) VALUES
  (103, 'Prescription', 'Prescription status — August 2026', '{"reportType":"Prescription","startDate":"2026-08-01","endDate":"2026-08-23","filters":{"status":"ISSUED"}}', 1, '2026-08-23T11:15:00', 3, '[{"prescriptionId":101,"status":"ISSUED"},{"prescriptionId":102,"status":"ISSUED"},{"prescriptionId":104,"status":"ISSUED"}]', 1),
  (104, 'UserAccess', 'Patient records without linked accounts', '{"reportType":"UserAccess","startDate":"2026-08-01","endDate":"2026-08-23","filters":{"linkedAccount":"false"}}', 1, '2026-08-23T11:20:00', 6, '[{"patientIdentifier":"PAT-2026-001"},{"patientIdentifier":"PAT-2026-002"},{"patientIdentifier":"PAT-2026-003"},{"patientIdentifier":"PAT-2026-004"},{"patientIdentifier":"PAT-2026-005"},{"patientIdentifier":"PAT-2026-006"}]', 1);

-- Link each patient business record to its sample sign-in account.
UPDATE user_profiles
SET user_id = (SELECT user_id FROM user_accounts WHERE username = 'aina.patient'), updated_at = CURRENT_TIMESTAMP
WHERE profile_id = 101;
UPDATE user_profiles
SET user_id = (SELECT user_id FROM user_accounts WHERE username = 'daniel.patient'), updated_at = CURRENT_TIMESTAMP
WHERE profile_id = 102;
UPDATE user_profiles
SET user_id = (SELECT user_id FROM user_accounts WHERE username = 'kavitha.patient'), updated_at = CURRENT_TIMESTAMP
WHERE profile_id = 103;
UPDATE user_profiles
SET user_id = (SELECT user_id FROM user_accounts WHERE username = 'faridah.patient'), updated_at = CURRENT_TIMESTAMP
WHERE profile_id = 104;
UPDATE user_profiles
SET user_id = (SELECT user_id FROM user_accounts WHERE username = 'marcus.patient'), updated_at = CURRENT_TIMESTAMP
WHERE profile_id = 105;
UPDATE user_profiles
SET user_id = (SELECT user_id FROM user_accounts WHERE username = 'nurul.patient'), updated_at = CURRENT_TIMESTAMP
WHERE profile_id = 106;

-- Volume fixtures for non-profile operational tables.  These bring every operational
-- table to at least 21 rows while retaining valid cross-table references.
WITH RECURSIVE sample(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM sample WHERE n < 14
)
INSERT OR IGNORE INTO medicines (medicine_id, medicine_code, medicine_name, generic_name, dosage_form, strength, unit, description, active, created_at, updated_at, version)
SELECT 107 + n, printf('MED-DEMO-%03d', n), printf('Demo medicine %02d', n),
       printf('Demo active ingredient %02d', n), 'Tablet', '10 mg', 'tablets',
       'Fictional development fixture medicine.', 1,
       printf('2026-08-%02dT08:00:00', n), printf('2026-08-%02dT08:00:00', n), 1
FROM sample;

WITH RECURSIVE sample(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM sample WHERE n < 14
)
INSERT OR IGNORE INTO inventory_items (inventory_id, medicine_id, batch_number, expiry_date, quantity_on_hand, reorder_level, version)
SELECT 108 + n, 107 + n, printf('DEMO-26-%03d', n),
       printf('2027-%02d-28', ((n - 1) % 12) + 1), 180 + n, 60, 1
FROM sample;

WITH RECURSIVE sample(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM sample WHERE n < 14
)
INSERT OR IGNORE INTO stock_movements (movement_id, inventory_id, medicine_id, movement_type, quantity_delta, balance_after, reason, performed_by, created_at)
SELECT 107 + n, 108 + n, 107 + n, 'RECEIVE', 180 + n, 180 + n,
       'Development fixture stock receipt', 4, printf('2026-08-%02dT08:15:00', n)
FROM sample;

WITH RECURSIVE sample(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM sample WHERE n < 18
)
INSERT OR IGNORE INTO prescriptions (prescription_id, patient_id, doctor_id, clinical_notes, status, issued_at, status_changed_at, status_changed_by, status_change_reason, created_at, updated_at, cancelled_at, cancellation_reason, version)
SELECT 106 + n, 101 + (n % 6), CASE WHEN n % 2 = 0 THEN 2 ELSE 3 END,
       printf('Development fixture prescription %02d.', n), 'ISSUED',
       printf('2026-08-%02dT09:00:00', n), printf('2026-08-%02dT09:00:00', n),
       CASE WHEN n % 2 = 0 THEN 2 ELSE 3 END, 'Prescription issued',
       printf('2026-08-%02dT08:55:00', n), printf('2026-08-%02dT09:00:00', n), NULL, NULL, 2
FROM sample;

WITH RECURSIVE sample(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM sample WHERE n < 18
)
INSERT OR IGNORE INTO prescription_items (prescription_item_id, prescription_id, medicine_id, medicine_name, dosage, dosage_unit, frequency, route, duration_days, instructions, quantity)
SELECT 108 + n, 106 + n, 108 + ((n - 1) % 14),
       printf('Demo medicine %02d', ((n - 1) % 14) + 1), '10', 'mg',
       'once daily', 'oral', 14, 'Development fixture: take as directed.', 14
FROM sample;

WITH RECURSIVE sample(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM sample WHERE n < 18
)
INSERT OR IGNORE INTO dispense_records (dispense_id, prescription_id, patient_id, pharmacist_id, required_quantities_json, dispensed_quantities_json, status, verified_at, dispensed_at, created_at, updated_at, failure_reason, version)
SELECT 103 + n, 106 + n, 101 + (n % 6), 4,
       printf('{"%d":14}', 108 + ((n - 1) % 14)),
       CASE WHEN n % 3 = 0 THEN printf('{"%d":14}', 108 + ((n - 1) % 14)) ELSE '{}' END,
       CASE WHEN n % 3 = 0 THEN 'DISPENSED' WHEN n % 3 = 1 THEN 'PENDING' ELSE 'VERIFIED' END,
       CASE WHEN n % 3 = 1 THEN NULL ELSE printf('2026-08-%02dT09:10:00', n) END,
       CASE WHEN n % 3 = 0 THEN printf('2026-08-%02dT09:20:00', n) ELSE NULL END,
       printf('2026-08-%02dT09:05:00', n), printf('2026-08-%02dT09:20:00', n), NULL,
       CASE WHEN n % 3 = 1 THEN 1 WHEN n % 3 = 2 THEN 2 ELSE 3 END
FROM sample;

WITH RECURSIVE sample(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM sample WHERE n < 18
)
INSERT OR IGNORE INTO notifications (notification_id, patient_id, prescription_id, event_type, notification_type, title, message, recipient, delivery_status, deduplication_key, created_at, delivered_at, failed_at, read_at, failure_reason)
SELECT 107 + n, 101 + (n % 6), 106 + n, 'PRESCRIPTION_ISSUED', 'INFO',
       'Prescription issued', 'A development fixture prescription has been issued.',
       printf('patient%02d@example.test', 101 + (n % 6)), 'DELIVERED',
       printf('%d:%d:PRESCRIPTION_ISSUED', 101 + (n % 6), 106 + n),
       printf('2026-08-%02dT09:00:00', n), printf('2026-08-%02dT09:01:00', n), NULL, NULL, NULL
FROM sample;

WITH RECURSIVE sample(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM sample WHERE n < 17
)
INSERT OR IGNORE INTO reports (report_id, report_type, title, criteria_json, generated_by, generated_at, row_count, data_json, version)
SELECT 104 + n,
       CASE n % 4 WHEN 0 THEN 'Prescription' WHEN 1 THEN 'Dispensing' WHEN 2 THEN 'Inventory' ELSE 'UserAccess' END,
       printf('Development fixture report %02d', n),
       printf('{"reportType":"Development","filters":{"fixture":"%02d"}}', n), 1,
       printf('2026-08-%02dT12:00:00', n), n,
       printf('[{"fixtureRow":%d,"status":"SAMPLE"}]', n), 1
FROM sample;
