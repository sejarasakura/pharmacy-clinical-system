package pharmacy_system.model.security_user;

/**
 * Hashing seam consulted by {@link Credential}. The concrete password-hashing
 * algorithm is deliberately deferred (see {@code storage_notdone.md}):
 * {@link Credential} depends only on this interface, so the algorithm behind
 * it can be swapped without changing the model or any of its callers.
 */
public interface PasswordHasher {

    /**
     * Produces a salted hash of {@code password} suitable for storage in
     * {@code Credential.passwordHash}. Implementations must never return or
     * retain the raw password.
     *
     * @param password the raw password characters; the caller remains
     *                  responsible for clearing this array afterwards
     * @return an opaque, storable representation of the hashed password
     */
    String hash(char[] password);

    /** Convenience overload for legacy callers; security-sensitive code should prefer {@code char[]}. */
    default String hash(String password) {
        return hash(password == null ? null : password.toCharArray());
    }

    /**
     * Returns whether {@code password} matches a hash previously produced by
     * {@link #hash(char[])}.
     *
     * @param password     the raw password characters to check
     * @param passwordHash the previously stored hash to check against
     * @return {@code true} only when the password matches the stored hash
     */
    boolean matches(char[] password, String passwordHash);

    /** Convenience overload for legacy callers; security-sensitive code should prefer {@code char[]}. */
    default boolean matches(String password, String passwordHash) {
        return matches(password == null ? null : password.toCharArray(), passwordHash);
    }
}
