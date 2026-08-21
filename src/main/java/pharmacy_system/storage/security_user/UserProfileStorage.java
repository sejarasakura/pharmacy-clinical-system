package pharmacy_system.storage.security_user;

import pharmacy_system.model.security_user.profile.UserProfile;
import java.util.List;
import java.util.Optional;

/**
 * Storage contract for UserProfile entities (abstract base and all subclasses).
 * Supports CRUD operations with polymorphic retrieval by type.
 * UserProfile is an abstract base; concrete types are DoctorProfile, PatientProfile, PharmacyProfile, AdminProfile.
 * Implements optimistic concurrency control via version fields.
 */
public interface UserProfileStorage {

    /**
     * Creates and persists a new UserProfile.
     * @param profile the UserProfile to create (must have userId set; actual type is a concrete subclass)
     * @return the created UserProfile with assigned profileId and version set to 1
     */
    UserProfile create(UserProfile profile);

    /**
     * Retrieves a UserProfile by its unique ID.
     * @param profileId the profile ID
     * @return Optional containing the UserProfile if found, empty otherwise
     */
    Optional<UserProfile> findById(long profileId);

    /**
     * Retrieves a UserProfile by its associated user ID.
     * @param userId the user ID
     * @return Optional containing the UserProfile if found, empty otherwise
     */
    Optional<UserProfile> findByUserId(long userId);

    /**
     * Retrieves a UserProfile by user ID and concrete type.
     * @param userId the user ID
     * @param profileType the expected profile class (e.g., DoctorProfile.class, PatientProfile.class)
     * @return Optional containing the UserProfile cast to the expected type if found and type matches, empty otherwise
     */
    <T extends UserProfile> Optional<T> findByUserIdAndType(long userId, Class<T> profileType);

    /**
     * Updates an existing UserProfile with optimistic concurrency control.
     * @param profile the updated UserProfile (must have version set to the current value)
     * @param expectedVersion the version at the time of the last read (must match current to succeed)
     * @return true if update succeeds, false if version conflict detected
     * @throws IllegalArgumentException if the profile does not exist
     */
    boolean update(UserProfile profile, long expectedVersion);

    /**
     * Deletes a UserProfile by ID.
     * @param profileId the profile ID to delete
     * @return true if deletion succeeds, false if the profile does not exist
     */
    boolean delete(long profileId);

    /**
     * Retrieves all UserProfiles of a specific type.
     * @param profileType the profile class to filter by
     * @return a list of UserProfiles of the given type
     */
    <T extends UserProfile> List<T> listByType(Class<T> profileType);

    /**
     * Retrieves all UserProfiles.
     * @return a list of all UserProfiles in the system
     */
    List<UserProfile> listAll();
}
