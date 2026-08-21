package pharmacy_system.storage.security_user;

import pharmacy_system.model.security_user.UserAccount;
import java.util.List;
import java.util.Optional;

/**
 * Storage contract for UserAccount entities.
 * Supports CRUD operations and queries needed by the authentication and account management workflows.
 * Implements optimistic concurrency control via version fields.
 */
public interface UserAccountStorage {

    /**
     * Creates and persists a new UserAccount.
     * @param account the UserAccount to create (must have all mandatory fields populated)
     * @return the created UserAccount with assigned userId and version set to 1
     */
    UserAccount create(UserAccount account);

    /**
     * Retrieves a UserAccount by its unique ID.
     * @param userId the user ID
     * @return Optional containing the UserAccount if found, empty otherwise
     */
    Optional<UserAccount> findById(long userId);

    /**
     * Retrieves a UserAccount by username (case-sensitive).
     * @param username the username to search for
     * @return Optional containing the UserAccount if found, empty otherwise
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * Retrieves a UserAccount by email address.
     * @param email the email to search for
     * @return Optional containing the UserAccount if found, empty otherwise
     */
    Optional<UserAccount> findByEmail(String email);

    /**
     * Updates an existing UserAccount with optimistic concurrency control.
     * @param account the updated UserAccount (must have version set to the current value)
     * @param expectedVersion the version at the time of the last read (must match current to succeed)
     * @return true if update succeeds, false if version conflict detected
     * @throws IllegalArgumentException if the account does not exist
     */
    boolean update(UserAccount account, long expectedVersion);

    /**
     * Deletes a UserAccount by ID.
     * @param userId the user ID to delete
     * @return true if deletion succeeds, false if the account does not exist
     */
    boolean delete(long userId);

    /**
     * Retrieves all UserAccounts.
     * @return a list of all UserAccounts in the system
     */
    List<UserAccount> listAll();

    /**
     * Queries UserAccounts for reporting purposes.
     * Used by the GenerateReportsController to retrieve user/access reports.
     * @param filters optional filter criteria (implementation-dependent)
     * @return a list of Maps representing UserAccount rows matching the filters
     */
    List<java.util.Map<String, Object>> queryForReport(java.util.Map<String, String> filters);
}
