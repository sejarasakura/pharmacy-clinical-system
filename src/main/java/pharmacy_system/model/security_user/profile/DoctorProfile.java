package pharmacy_system.model.security_user.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Doctor-specific profile extending UserProfile.
 * Doctor role is permitted to view/edit personal information and system defaults
 * but never role/status information.
 * 
 * [UCD-05]
 */
public class DoctorProfile extends UserProfile {
    private String licenseNumber;
    private String specialization;
    private String department;

    // Constructor
    public DoctorProfile(long profileId, long userId, String fullName) {
        super(profileId, userId, fullName);
        this.licenseNumber = null;
        this.specialization = null;
        this.department = null;
    }

    public DoctorProfile(Long userId, String fullName, String medicalRegistrationNo,
                         String speciality) {
        super(userId, fullName);
        this.licenseNumber = medicalRegistrationNo;
        this.specialization = speciality;
    }

    public DoctorProfile(long profileId, Long userId, String fullName, String phoneNumber,
                         String contactEmail, String address, Map<String, String> preferences,
                         long version, java.time.LocalDateTime createdAt,
                         java.time.LocalDateTime updatedAt, String medicalRegistrationNo,
                         String speciality) {
        super(profileId, userId, fullName, phoneNumber, contactEmail, address,
                preferences, version, createdAt, updatedAt);
        this.licenseNumber = medicalRegistrationNo;
        this.specialization = speciality;
    }

    /** Compatibility constructor for the original profile-storage contract. */
    public DoctorProfile(long profileId, long userId, String fullName,
                         String phoneNumber, String contactEmail, String address,
                         String licenseNumber, Map<String, String> preferences,
                         long version) {
        super(profileId, userId, fullName);
        this.phoneNumber = phoneNumber;
        this.contactEmail = contactEmail;
        this.address = address;
        this.licenseNumber = licenseNumber;
        this.preferences = preferences == null ? new java.util.HashMap<>() : new java.util.HashMap<>(preferences);
        this.version = version;
        this.specialization = null;
        this.department = null;
    }

    // Getters
    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getDepartment() {
        return department;
    }
    public String getMedicalRegistrationNo() { return licenseNumber; }
    public String getSpeciality() { return specialization; }
    public boolean validateProfessionalDetails() {
        return licenseNumber != null && !licenseNumber.isBlank()
                && licenseNumber.length() <= MAX_TEXT_LENGTH;
    }

    // Setters
    public void setLicenseNumber(String licenseNumber) {
        if (licenseNumber != null && !licenseNumber.isBlank() && licenseNumber.length() <= 100) {
            this.licenseNumber = licenseNumber;
            this.updatedAt = java.time.LocalDateTime.now();
        }
    }

    public void setSpecialization(String specialization) {
        if (specialization != null && !specialization.isBlank() && specialization.length() <= 100) {
            this.specialization = specialization;
            this.updatedAt = java.time.LocalDateTime.now();
        }
    }

    public void setDepartment(String department) {
        if (department != null && !department.isBlank() && department.length() <= 100) {
            this.department = department;
            this.updatedAt = java.time.LocalDateTime.now();
        }
    }

    @Override
    public void applyChanges(Map<String, Object> changes) {
        if (changes != null && changes.get("speciality") != null
                && !(changes.get("speciality") instanceof String)) {
            throw new IllegalArgumentException("speciality must be a String");
        }
        super.applyChanges(changes);

        if (changes == null) return;

        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            switch (field) {
                case "licenseNumber":
                    if (value instanceof String) {
                        setLicenseNumber((String) value);
                    }
                    break;
                case "specialization":
                    if (value instanceof String) {
                        setSpecialization((String) value);
                    }
                    break;
                case "speciality":
                    this.specialization = (String) value;
                    break;
                case "department":
                    if (value instanceof String) {
                        setDepartment((String) value);
                    }
                    break;
            }
        }
    }

    @Override
    public List<String> validateProfileData() {
        List<String> errors = super.validateProfileData();

        if (licenseNumber == null || licenseNumber.isBlank()) {
            errors.add("medicalRegistrationNo is required");
        } else if (licenseNumber.length() > MAX_TEXT_LENGTH) {
            errors.add("medicalRegistrationNo must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        if (specialization != null && specialization.length() > MAX_TEXT_LENGTH) {
            errors.add("speciality must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        if (department != null && department.length() > 100) {
            errors.add("Department must not exceed 100 characters");
        }

        return errors;
    }

    @Override
    public String getProfileType() {
        return "Doctor";
    }

    @Override
    public String toString() {
        return "DoctorProfile{" +
                "profileId=" + profileId +
                ", userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", licenseNumber='" + licenseNumber + '\'' +
                ", specialization='" + specialization + '\'' +
                ", department='" + department + '\'' +
                ", version=" + version +
                '}';
    }
}
