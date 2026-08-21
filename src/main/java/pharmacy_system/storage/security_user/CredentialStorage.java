package pharmacy_system.storage.security_user;

import pharmacy_system.model.security_user.Credential;
import java.util.Optional;

/**
 * Storage contract for Credential entities.
 * Credentials are tightly coupled to UserAccounts but stored separately to enforce the separation
 * between user identity (UserAccount) and authentication materials (Credential).
 * Implements optimistic concurrency control via version fields.
 */
public interface CredentialStorage {

    /**
     * Creates and persists a new Credential.
     * @param credential the Credential to create (must have userId set)
     * @return the created Credential with assigned credentialId and version set to 1
     */
    Credential create(Credential credential);

    /**
     * Retrieves a Credential by its unique ID.
     * @param credentialId the credential ID
     * @return Optional containing the Credential if found, empty otherwise
     */
    Optional<Credential> findById(long credentialId);

    /**
     * Retrieves a Credential by the associated user ID.
     * @param userId the user ID to search for
     * @return Optional containing the Credential if found, empty otherwise
     */
    Optional<Credential> findByUserId(long userId);

    /**
     * Updates an existing Credential with optimistic concurrency control.
     * @param credential the updated Credential (must have version set to the current value)
     * @param expectedVersion the version at the time of the last read (must match current to succeed)
     * @return true if update succeeds, false if version conflict detected
     * @throws IllegalArgumentException if the credential does not exist
     */
    boolean update(Credential credential, long expectedVersion);

    /**
     * Deletes a Credential by ID.
     * @param credentialId the credential ID to delete
     * @return true if deletion succeeds, false if the credential does not exist
     */
    boolean delete(long credentialId);
}
