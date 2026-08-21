package pharmacy_system.model.security_user.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Administrator-specific profile extending UserProfile.
 * Represents personal information for administrative staff.
 * 
 * [UCD-05]
 */
public class AdminProfile extends UserProfile {
    private String employeeId;
    private String department;

    // Constructor
    public AdminProfile(long profileId, long userId, String fullName) {
        super(profileId, userId, fullName);
        this.employeeId = null;
        this.department = null;
    }

    public AdminProfile(Long userId, String fullName, String staffId, String department) {
        super(userId, fullName);
        this.employeeId = staffId;
        this.department = department;
    }

    public AdminProfile(long profileId, Long userId, String fullName, String phoneNumber,
                        String contactEmail, String address, Map<String, String> preferences,
                        long version, java.time.LocalDateTime createdAt,
                        java.time.LocalDateTime updatedAt, String staffId, String department) {
        super(profileId, userId, fullName, phoneNumber, contactEmail, address,
                preferences, version, createdAt, updatedAt);
        this.employeeId = staffId;
        this.department = department;
    }

    // Getters
    public String getEmployeeId() {
        return employeeId;
    }

    public String getDepartment() {
        return department;
    }
    public String getStaffId() { return employeeId; }

    // Setters
    public void setEmployeeId(String employeeId) {
        if (employeeId != null && !employeeId.isBlank() && employeeId.length() <= 100) {
            this.employeeId = employeeId;
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
        if (changes != null && changes.get("department") != null
                && !(changes.get("department") instanceof String)) {
            throw new IllegalArgumentException("department must be a String");
        }
        super.applyChanges(changes);

        if (changes == null) return;

        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            switch (field) {
                case "employeeId":
                    if (value instanceof String) {
                        setEmployeeId((String) value);
                    }
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

        if (employeeId != null && employeeId.length() > MAX_TEXT_LENGTH) {
            errors.add("staffId must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        if (department != null && department.length() > MAX_TEXT_LENGTH) {
            errors.add("department must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        return errors;
    }

    @Override
    public String getProfileType() {
        return "Administrator";
    }

    @Override
    public String toString() {
        return "AdminProfile{" +
                "profileId=" + profileId +
                ", userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", department='" + department + '\'' +
                ", version=" + version +
                '}';
    }
}
