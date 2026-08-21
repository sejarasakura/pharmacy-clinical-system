package pharmacy_system.model.security_user.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pharmacist-specific profile extending UserProfile.
 * Represents personal information for pharmacy staff.
 * 
 * [UCD-05]
 */
public class PharmacyProfile extends UserProfile {
    private String pharmacyRegistration;
    private String branch;

    // Constructor
    public PharmacyProfile(long profileId, long userId, String fullName) {
        super(profileId, userId, fullName);
        this.pharmacyRegistration = null;
        this.branch = null;
    }

    public PharmacyProfile(Long userId, String fullName, String pharmacistRegistrationNo,
                           String pharmacyUnit) {
        super(userId, fullName);
        this.pharmacyRegistration = pharmacistRegistrationNo;
        this.branch = pharmacyUnit;
    }

    public PharmacyProfile(long profileId, Long userId, String fullName, String phoneNumber,
                           String contactEmail, String address, Map<String, String> preferences,
                           long version, java.time.LocalDateTime createdAt,
                           java.time.LocalDateTime updatedAt, String pharmacistRegistrationNo,
                           String pharmacyUnit) {
        super(profileId, userId, fullName, phoneNumber, contactEmail, address,
                preferences, version, createdAt, updatedAt);
        this.pharmacyRegistration = pharmacistRegistrationNo;
        this.branch = pharmacyUnit;
    }

    // Getters
    public String getPharmacyRegistration() {
        return pharmacyRegistration;
    }

    public String getBranch() {
        return branch;
    }
    public String getPharmacistRegistrationNo() { return pharmacyRegistration; }
    public String getPharmacyUnit() { return branch; }
    public boolean validateProfessionalDetails() {
        return pharmacyRegistration != null && !pharmacyRegistration.isBlank()
                && pharmacyRegistration.length() <= MAX_TEXT_LENGTH;
    }

    // Setters
    public void setPharmacyRegistration(String pharmacyRegistration) {
        if (pharmacyRegistration != null && !pharmacyRegistration.isBlank() && pharmacyRegistration.length() <= 100) {
            this.pharmacyRegistration = pharmacyRegistration;
            this.updatedAt = java.time.LocalDateTime.now();
        }
    }

    public void setBranch(String branch) {
        if (branch != null && !branch.isBlank() && branch.length() <= 100) {
            this.branch = branch;
            this.updatedAt = java.time.LocalDateTime.now();
        }
    }

    @Override
    public void applyChanges(Map<String, Object> changes) {
        if (changes != null && changes.get("pharmacyUnit") != null
                && !(changes.get("pharmacyUnit") instanceof String)) {
            throw new IllegalArgumentException("pharmacyUnit must be a String");
        }
        super.applyChanges(changes);

        if (changes == null) return;

        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            switch (field) {
                case "pharmacyRegistration":
                    if (value instanceof String) {
                        setPharmacyRegistration((String) value);
                    }
                    break;
                case "branch":
                    if (value instanceof String) {
                        setBranch((String) value);
                    }
                    break;
                case "pharmacyUnit":
                    this.branch = (String) value;
                    break;
            }
        }
    }

    @Override
    public List<String> validateProfileData() {
        List<String> errors = super.validateProfileData();

        if (pharmacyRegistration == null || pharmacyRegistration.isBlank()) {
            errors.add("pharmacistRegistrationNo is required");
        } else if (pharmacyRegistration.length() > MAX_TEXT_LENGTH) {
            errors.add("pharmacistRegistrationNo must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        if (branch != null && branch.length() > MAX_TEXT_LENGTH) {
            errors.add("pharmacyUnit must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        return errors;
    }

    @Override
    public String getProfileType() {
        return "Pharmacist";
    }

    @Override
    public String toString() {
        return "PharmacyProfile{" +
                "profileId=" + profileId +
                ", userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", pharmacyRegistration='" + pharmacyRegistration + '\'' +
                ", branch='" + branch + '\'' +
                ", version=" + version +
                '}';
    }
}
