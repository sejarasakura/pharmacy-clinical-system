package pharmacy_system.storage.security_user;

import pharmacy_system.model.security_user.Credential;
import pharmacy_system.model.security_user.PasswordHasher;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of CredentialStorage using HashMap.
 * Thread-safe with atomic ID generation and version-checked updates.
 */
public class InMemoryCredentialStorage implements CredentialStorage {
    private final Map<Long, Credential> store = new ConcurrentHashMap<>();
    private final Map<Long, Long> userIdIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final PasswordHasher passwordHasher;

    /**
     * Creates an InMemoryCredentialStorage with a provided PasswordHasher.
     * @param passwordHasher the hashing algorithm to use for password operations
     */
    public InMemoryCredentialStorage(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Credential create(Credential credential) {
        long credentialId = idGenerator.getAndIncrement();
        // Create a new Credential by calling the persistence constructor
        Credential created = new Credential(
                credentialId,
                credential.getUserId(),
                credential.getPasswordHash(),
                credential.getFailedAttempts(),
                credential.getLockedUntil(),
                credential.getPasswordUpdatedAt(),
                passwordHasher,
                1L
        );
        store.put(credentialId, created);
        userIdIndex.put(credential.getUserId(), credentialId);
        return created;
    }

    @Override
    public Optional<Credential> findById(long credentialId) {
        return Optional.ofNullable(store.get(credentialId));
    }

    @Override
    public Optional<Credential> findByUserId(long userId) {
        Long credentialId = userIdIndex.get(userId);
        if (credentialId == null) return Optional.empty();
        return Optional.ofNullable(store.get(credentialId));
    }

    @Override
    public boolean update(Credential credential, long expectedVersion) {
        Credential existing = store.get(credential.getCredentialId());
        if (existing == null) {
            throw new IllegalArgumentException("Credential not found: " + credential.getCredentialId());
        }
        if (existing.getVersion() != expectedVersion) {
            return false; // version conflict
        }
        // Create updated credential with incremented version
        Credential updated = new Credential(
                credential.getCredentialId(),
                credential.getUserId(),
                credential.getPasswordHash(),
                credential.getFailedAttempts(),
                credential.getLockedUntil(),
                credential.getPasswordUpdatedAt(),
                passwordHasher,
                expectedVersion + 1
        );
        store.put(credential.getCredentialId(), updated);
        return true;
    }

    @Override
    public boolean delete(long credentialId) {
        Credential credential = store.remove(credentialId);
        if (credential == null) return false;
        userIdIndex.remove(credential.getUserId());
        return true;
    }
}
