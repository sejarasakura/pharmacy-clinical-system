package pharmacy_system.model.security_user.profile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Patient-specific profile extending UserProfile.
 * Patient profile doubles as the Patient business record used for clinical work.
 * May exist without a linked UserAccount (FR-010).
 * 
 * Contains mandatory patient identifier and full name, plus optional clinical/emergency fields.
 * 
 * [UCD-05]
 */
public class PatientProfile extends UserProfile {
    private String patientIdentifier;  // mandatory, business record identifier
    private LocalDate dateOfBirth;
    private String emergencyContact;

    // Constructor
    public PatientProfile(long profileId, long userId, String fullName, String patientIdentifier) {
        super(profileId, userId, fullName);
        this.patientIdentifier = patientIdentifier;
        this.dateOfBirth = null;
        this.emergencyContact = null;
    }

    public PatientProfile(Long userId, String patientIdentifier, String fullName,
                          String phoneNumber, String contactEmail, LocalDate dateOfBirth,
                          String address, String emergencyContact) {
        super(userId, fullName);
        this.patientIdentifier = patientIdentifier;
        this.phoneNumber = phoneNumber;
        this.contactEmail = contactEmail;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.emergencyContact = emergencyContact;
    }

    /** Compatibility constructor for the original profile-storage contract. */
    public PatientProfile(long profileId, long userId, String fullName,
                          String phoneNumber, String contactEmail, String address,
                          LocalDate dateOfBirth, String emergencyContact,
                          Map<String, String> preferences, long version) {
        this(profileId, userId, "PAT-" + (profileId > 0 ? profileId : userId), fullName,
                phoneNumber, contactEmail, address, preferences, version,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                dateOfBirth, emergencyContact);
    }

    public PatientProfile(long profileId, long userId, String patientIdentifier, String fullName,
                          String phoneNumber, String contactEmail, String address,
                          Map<String, String> preferences, long version,
                          java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt,
                          LocalDate dateOfBirth, String emergencyContact) {
        super(profileId, Long.valueOf(userId), fullName, phoneNumber, contactEmail, address,
                preferences, version, createdAt, updatedAt);
        this.patientIdentifier = patientIdentifier;
        this.dateOfBirth = dateOfBirth;
        this.emergencyContact = emergencyContact;
    }

    // Getters
    public String getPatientIdentifier() {
        return patientIdentifier;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    /** A patient business record is unlinked when it has no user-account identifier. */
    public boolean hasLinkedAccount() {
        return super.hasLinkedAccount();
    }

    // Setters
    public void setDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth != null && dateOfBirth.isBefore(LocalDate.now())) {
            this.dateOfBirth = dateOfBirth;
            this.updatedAt = java.time.LocalDateTime.now();
        }
    }

    public void setEmergencyContact(String emergencyContact) {
        if (emergencyContact != null && !emergencyContact.isBlank() && emergencyContact.length() <= 255) {
            this.emergencyContact = emergencyContact;
            this.updatedAt = java.time.LocalDateTime.now();
        }
    }

    @Override
    public void applyChanges(Map<String, Object> changes) {
        super.applyChanges(changes);

        if (changes == null) return;

        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            switch (field) {
                case "dateOfBirth":
                    if (value instanceof LocalDate) {
                        setDateOfBirth((LocalDate) value);
                    }
                    break;
                case "emergencyContact":
                    if (value instanceof String) {
                        setEmergencyContact((String) value);
                    }
                    break;
            }
        }
    }

    @Override
    public List<String> validateProfileData() {
        List<String> errors = super.validateProfileData();

        if (patientIdentifier == null || patientIdentifier.isBlank()) {
            errors.add("patientIdentifier is required");
        } else if (patientIdentifier.length() > MAX_TEXT_LENGTH) {
            errors.add("patientIdentifier must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
            errors.add("dateOfBirth must not be later than the current date");
        }

        if (emergencyContact != null && emergencyContact.length() > MAX_TEXT_LENGTH) {
            errors.add("emergencyContact must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        return errors;
    }

    @Override
    public String getProfileType() {
        return "Patient";
    }

    public int calculateAge() {
        return dateOfBirth == null ? -1 : java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    @Override
    public String toString() {
        return "PatientProfile{" +
                "profileId=" + profileId +
                ", userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", patientIdentifier='" + patientIdentifier + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", version=" + version +
                '}';
    }
}
