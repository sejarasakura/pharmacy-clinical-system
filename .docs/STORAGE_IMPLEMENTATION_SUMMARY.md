# In-Memory Storage Implementation Summary

Task 8.2: Implement in-memory Storage implementations for all 9 Storage interfaces with version-checked updates and atomic inventory operations.

## Implementation Details

### Overview
Implemented 9 in-memory (HashMap-based) Storage implementations with thread-safe concurrent access, atomic ID generation, and optimistic concurrency control via version fields.

### Storage Implementations Created

#### 1. **InMemoryUserAccountStorage** (`security_user` domain)
- **Features:**
  - CRUD operations for UserAccount entities
  - Indices: username → userId, email → userId
  - Version-checked updates (returns false on stale version)
  - queryForReport() for administrative reporting

#### 2. **InMemoryCredentialStorage** (`security_user` domain)
- **Features:**
  - Stores password hashes and authentication state
  - Requires PasswordHasher seam (constructor parameter)
  - Index: userId → credentialId
  - Version-checked updates with password verification support
  - Tracks failed attempts and lockout state

#### 3. **InMemoryRolePermissionStorage** (`security_user` domain)
- **Features:**
  - CRUD for role definitions and permissions
  - User-role join table via `userRoleAssignments` index
  - Methods: assignRoleToUser(), removeRoleFromUser(), findRolesByUserId()
  - Name-based lookup index for efficiency

#### 4. **InMemoryUserProfileStorage** (`security_user` domain)
- **Features:**
  - Polymorphic profile storage (DoctorProfile, PatientProfile, etc.)
  - Type-based queries: findByUserIdAndType()
  - listByType() for profile filtering
  - userId → profileId index

#### 5. **InMemoryPrescriptionStorage** (`clinical_prescription` domain)
- **Features:**
  - Prescription lifecycle management
  - Indices: patientId → prescriptionIds, doctorId → prescriptionIds
  - Version-checked updates (Requirement 5.1 - valid transitions)
  - queryForReport() with date range filtering

#### 6. **InMemoryNotificationStorage** (`patient_information` domain)
- **Features:**
  - Persisted patient notifications
  - Deduplication index: (patientId, prescriptionId, eventType) → notificationId
  - existsByDeduplicationKey() for duplicate prevention (Requirement 7.3)
  - findByDeduplicationKey() for dedup queries
  - No version field (Notification model doesn't use versions)

#### 7. **InMemoryDispenseRecordStorage** (`pharmacy_operations` domain)
- **Features:**
  - Dispensing transaction records
  - Indices: prescriptionId → dispenseIds, patientId → dispenseIds
  - Completed dispense tracking: completedDispensesByPrescription map
  - existsCompletedDispense() for idempotent-fulfilment property (Requirement 8.4)
  - Version-checked updates
  - queryForReport() with date range filtering

#### 8. **InMemoryMedicineStorage** (`pharmacy_operations` domain)
- **Features:**
  - Medicine master data (no quantity here)
  - Code-based lookup for unique medicine identification
  - Active/inactive filtering: listActive()
  - Version-checked updates
  - queryForReport() for inventory reports

#### 9. **InMemoryInventoryItemStorage** (`pharmacy_operations` domain) - **ATOMIC OPERATIONS**
- **Features:**
  - Batch position tracking (quantity, expiry, reorder level)
  - Index: medicineId → inventoryIds
  - **ATOMIC deductInventory(Map<inventoryId, qty>, StockMovement):**
    - Phase 1: Validates all items exist and have sufficient quantity
    - Phase 2: Atomically deducts from all batches
    - Phase 3: Records single combined movement
    - Implements FEFO (First-Expiry-First-Out) ordering
    - Validates no partial dispenses (Requirement 8.1)
    - Returns false if insufficient eligible stock (no partial deduction)
  - **ATOMIC adjustStock(item, movement, expectedVersion):**
    - Atomically adjusts single batch with reason validation
    - Returns false if would result in negative balance (Requirement 9.5)
    - Records movement for audit trail
  - queryForReport() for inventory reports

### Key Architectural Patterns

#### Thread Safety
- **ConcurrentHashMap** for main stores
- **AtomicLong** for ID generation
- No explicit locking needed for read operations

#### Version-Checked Updates (Optimistic Concurrency)
```java
public boolean update(Entity entity, long expectedVersion) {
    Entity existing = store.get(entity.getId());
    if (existing == null) throw new IllegalArgumentException(...);
    if (existing.getVersion() != expectedVersion) {
        return false; // version conflict detected
    }
    // Update with version incremented
    Entity updated = new Entity(..., expectedVersion + 1);
    store.put(entity.getId(), updated);
    return true;
}
```

#### Atomic Inventory Operations
- **deductInventory** implements Requirements 8.1 (no partial), 8.2 (sufficient check), 8.3 (FEFO ordering)
- **adjustStock** implements Requirements 9.3 (reason required), 9.5 (no negative balance)
- Both operations record a single StockMovement atomically for audit trail

### Requirements Compliance

**Requirement 8.1 - No Over-Dispensing (FR-037)**
- `deductInventory` validates total eligible stock ≥ required before any deduction
- Returns false if insufficient, no stock is mutated

**Requirement 8.2 - FEFO Ordering (FR-040)**  
- `deductInventory` allocates from earliest-expiring batches first
- Skips expired batches (FR-046)

**Requirement 8.3 - Allocation Conservation (FR-037)**
- Sum of allocated quantities equals required quantity exactly
- No partial dispensing, no surplus

**Requirement 8.4 - Idempotent Fulfilment (FR-042)**
- `existsCompletedDispense()` returns true if DISPENSED record exists
- Prevents duplicate dispensing transactions

**Requirement 9.2 - Stock Receipt Recording (FR-035)**
- `deductInventory` records combined StockMovement of type DISPENSE
- Both deduction and movement are persisted atomically

**Requirement 9.3 - Adjustment Reason Required (FR-047)**
- `adjustStock` validates reason is non-blank
- Rejects adjustment without reason

**Requirement 9.5 - Non-Negative Stock (FR-048)**
- `adjustStock` rejects any adjustment that would result in negative balance
- Returns false before any commit

### Implementation Notes

1. **No DTO conversions needed** - Storage returns model entities directly
2. **Deferred PasswordHasher** - CredentialStorage requires injected PasswordHasher seam
3. **Notification has no version** - Storage implements update() but doesn't check version
4. **Immutable snapshots** - Report records are created once, never updated
5. **Cross-domain reads allowed** - Storage is called from controllers for validation/reporting
6. **Atomic movement recording** - Stock movements are persisted together with inventory updates

### Files Created
1. `src/main/java/pharmacy_system/storage/security_user/InMemoryUserAccountStorage.java`
2. `src/main/java/pharmacy_system/storage/security_user/InMemoryCredentialStorage.java`
3. `src/main/java/pharmacy_system/storage/security_user/InMemoryRolePermissionStorage.java`
4. `src/main/java/pharmacy_system/storage/security_user/InMemoryUserProfileStorage.java`
5. `src/main/java/pharmacy_system/storage/clinical_prescription/InMemoryPrescriptionStorage.java`
6. `src/main/java/pharmacy_system/storage/patient_information/InMemoryNotificationStorage.java`
7. `src/main/java/pharmacy_system/storage/pharmacy_operations/InMemoryDispenseRecordStorage.java`
8. `src/main/java/pharmacy_system/storage/pharmacy_operations/InMemoryMedicineStorage.java`
9. `src/main/java/pharmacy_system/storage/pharmacy_operations/InMemoryInventoryItemStorage.java`
10. `src/main/java/pharmacy_system/storage/management_dss/InMemoryReportStorage.java`

All implementations follow the Storage interface contracts and implement the requirements and properties specified in the design document.
