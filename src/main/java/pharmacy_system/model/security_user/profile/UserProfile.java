package pharmacy_system.model.security_user.profile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for self-service user profiles across all operational roles.
 * Represents personal/profile information that users can view and modify within permitted scope.
 * 
 * Restricted fields (role, account status, system IDs, permissions) are never modifiable
 * through self-service profile updates; these are controlled by administrators.
 * 
 * [UCD-05]
 */
public abstract class UserProfile {
    public static final int MAX_TEXT_LENGTH = 255;
    protected long profileId;
    protected Long userId;
    protected String fullName;
    protected String phoneNumber;
    protected String contactEmail;
    protected String address;
    protected Map<String, String> preferences;  // e.g. notification frequency
    protected long version;  // optimistic locking
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    // Restricted field names that cannot be modified through self-service
    private static final Set<String> RESTRICTED_FIELDS = Set.of(
        "profileId", "userId", "version",
        "role", "status", "permissions", "accountStatus",
        "createdAt", "updatedAt", "patientIdentifier", "medicalRegistrationNo",
        "pharmacistRegistrationNo", "staffId"
    );

    private static final Set<String> BASE_EDITABLE_FIELDS = Set.of(
            "fullName", "phoneNumber", "contactEmail", "address", "preferences");
    private static final Set<String> ROLE_EDITABLE_FIELDS = Set.of(
            "dateOfBirth", "emergencyContact", "speciality", "pharmacyUnit", "department");

    // Constructor
    public UserProfile(long profileId, long userId, String fullName) {
        this.profileId = profileId;
        this.userId = userId;
        this.fullName = fullName;
        this.phoneNumber = null;
        this.contactEmail = null;
        this.address = null;
        this.preferences = new HashMap<>();
        this.version = 0L;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UserProfile(Long userId, String fullName) {
        this(0L, userId, fullName, null, null, null, null, 0L,
                LocalDateTime.now(), LocalDateTime.now());
    }

    public UserProfile(long profileId, Long userId, String fullName,
                       String phoneNumber, String contactEmail, String address,
                       Map<String, String> preferences, long version,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.profileId = profileId;
        this.userId = userId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.contactEmail = contactEmail;
        this.address = address;
        this.preferences = preferences == null ? new HashMap<>() : new HashMap<>(preferences);
        this.version = version;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    // Getters
    public long getProfileId() {
        return profileId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getAddress() {
        return address;
    }

    public Map<String, String> getPreferences() {
        return Collections.unmodifiableMap(preferences);
    }

    public long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters for permitted fields
    public void setFullName(String fullName) {
        if (fullName != null && !fullName.isBlank() && fullName.length() <= 255) {
            this.fullName = fullName;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            this.phoneNumber = phoneNumber;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void setContactEmail(String contactEmail) {
        if (contactEmail != null && !contactEmail.isBlank()) {
            this.contactEmail = contactEmail;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void setAddress(String address) {
        if (address != null && !address.isBlank() && address.length() <= 255) {
            this.address = address;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void setPreference(String key, String value) {
        if (key != null && !key.isBlank()) {
            preferences.put(key, value);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void incrementVersion() {
        this.version++;
        this.updatedAt = LocalDateTime.now();
    }

    /** Storage-only identity assignment for the in-memory persistence adapter. */
    public void assignPersistenceIdentity(long persistedProfileId, long persistedVersion) {
        this.profileId = persistedProfileId;
        this.version = persistedVersion;
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic
    /**
     * Apply changes from a map, rejecting any restricted field modifications.
     * Called by ManageProfileController to ensure only permitted fields are modified.
     * 
     * @param changes map of field name -> new value
     * @throws IllegalArgumentException if attempting to modify restricted field
     */
    public void applyChanges(Map<String, Object> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        List<String> restricted = changes.keySet().stream().filter(RESTRICTED_FIELDS::contains).toList();
        if (!restricted.isEmpty()) throw new RestrictedFieldException(restricted);
        for (String fieldName : changes.keySet()) {
            if (!BASE_EDITABLE_FIELDS.contains(fieldName) && !ROLE_EDITABLE_FIELDS.contains(fieldName)) {
                throw new IllegalArgumentException("Unknown profile field: " + fieldName);
            }
        }
        validateChangeTypes(changes);

        // Apply non-restricted changes
        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            switch (field) {
                case "fullName":
                    if (value instanceof String) {
                        setFullName((String) value);
                    }
                    break;
                case "phoneNumber":
                    if (value instanceof String) {
                        setPhoneNumber((String) value);
                    }
                    break;
                case "contactEmail":
                    if (value instanceof String) {
                        setContactEmail((String) value);
                    }
                    break;
                case "address":
                    if (value instanceof String) {
                        setAddress((String) value);
                    }
                    break;
                case "preferences":
                    if (value instanceof Map<?, ?> values) {
                        values.forEach((key, preference) -> {
                            if (key instanceof String && preference instanceof String) {
                                setPreference((String) key, (String) preference);
                            }
                        });
                    }
                    break;
                // Role-specific fields are applied by subclasses after this atomic validation pass.
            }
        }
        incrementVersion();
    }

    private void validateChangeTypes(Map<String, Object> changes) {
        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            Object value = entry.getValue();
            String field = entry.getKey();
            if ("preferences".equals(field)) {
                if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("preferences must be a Map");
                if (map.keySet().stream().anyMatch(key -> !(key instanceof String))) {
                    throw new IllegalArgumentException("preferences keys must be Strings");
                }
                if (map.values().stream().anyMatch(preference -> !(preference instanceof String))) {
                    throw new IllegalArgumentException("preferences values must be Strings");
                }
            } else if ("dateOfBirth".equals(field)) {
                if (value != null && !(value instanceof java.time.LocalDate)) {
                    throw new IllegalArgumentException("dateOfBirth must be a LocalDate");
                }
            } else if (value != null && !(value instanceof String)) {
                throw new IllegalArgumentException(field + " must be a String");
            }
        }
    }

    /**
     * Validate profile data for consistency.
     * @return list of validation error messages; empty list if valid
     */
    public List<String> validateProfileData() {
        List<String> errors = new ArrayList<>();

        if (fullName == null || fullName.isBlank()) {
            errors.add("fullName is required");
        } else if (fullName.length() > MAX_TEXT_LENGTH) {
            errors.add("fullName must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
            if (digitsOnly.length() < 7 || digitsOnly.length() > 15) {
                errors.add("phoneNumber must contain 7 to 15 digits");
            }
        }

        if (contactEmail != null && !contactEmail.isBlank()) {
            if (!contactEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                errors.add("contactEmail must be of the form local@domain");
            }
        }

        if (address != null && address.length() > MAX_TEXT_LENGTH) {
            errors.add("address must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        return errors;
    }

    /**
     * Get the profile type/role. Implemented by subclasses.
     * @return string name of profile type (e.g. "Patient", "Doctor")
     */
    public String getProfileType() { return "User"; }

    public boolean hasLinkedAccount() { return userId != null && userId > 0; }
    public boolean hasValidProfileData() { return validateProfileData().isEmpty(); }
    public void updateContact(String phone, String email) {
        this.phoneNumber = phone; this.contactEmail = email; incrementVersion();
    }
    public void updateAddress(String value) { this.address = value; incrementVersion(); }
    public void updatePreference(String key, String value) { setPreference(key, value); incrementVersion(); }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "profileId=" + profileId +
                ", userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", version=" + version +
                '}';
    }
}
