package pharmacy_system.storage.security_user;

import pharmacy_system.model.security_user.UserAccount;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of UserAccountStorage using HashMap.
 * Thread-safe with atomic ID generation and version-checked updates.
 */
public class InMemoryUserAccountStorage implements UserAccountStorage {
    private final Map<Long, UserAccount> store = new ConcurrentHashMap<>();
    private final Map<String, Long> usernameIndex = new ConcurrentHashMap<>();
    private final Map<String, Long> emailIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public UserAccount create(UserAccount account) {
        long userId = idGenerator.getAndIncrement();
        UserAccount created = new UserAccount(
                userId,
                account.getUsername(),
                account.getEmail(),
                account.getStatus(),
                account.isRegistrationApproved(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                account.getLastLoginAt(),
                account.getDisabledAt(),
                account.getDisabledReason(),
                1 // version
        );
        store.put(userId, created);
        usernameIndex.put(account.getUsername(), userId);
        emailIndex.put(account.getEmail(), userId);
        return created;
    }

    @Override
    public Optional<UserAccount> findById(long userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        Long userId = usernameIndex.get(username);
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        Long userId = emailIndex.get(email);
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public boolean update(UserAccount account, long expectedVersion) {
        UserAccount existing = store.get(account.getUserId());
        if (existing == null) {
            throw new IllegalArgumentException("UserAccount not found: " + account.getUserId());
        }
        if (existing.getVersion() != expectedVersion) {
            return false; // version conflict
        }
        UserAccount updated = new UserAccount(
                account.getUserId(),
                account.getUsername(),
                account.getEmail(),
                account.getStatus(),
                account.isRegistrationApproved(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                account.getLastLoginAt(),
                account.getDisabledAt(),
                account.getDisabledReason(),
                expectedVersion + 1 // increment version
        );
        store.put(account.getUserId(), updated);
        // Update indices if username or email changed
        if (!existing.getUsername().equals(account.getUsername())) {
            usernameIndex.remove(existing.getUsername());
            usernameIndex.put(account.getUsername(), account.getUserId());
        }
        if (!existing.getEmail().equals(account.getEmail())) {
            emailIndex.remove(existing.getEmail());
            emailIndex.put(account.getEmail(), account.getUserId());
        }
        return true;
    }

    @Override
    public boolean delete(long userId) {
        UserAccount account = store.remove(userId);
        if (account == null) return false;
        usernameIndex.remove(account.getUsername());
        emailIndex.remove(account.getEmail());
        return true;
    }

    @Override
    public List<UserAccount> listAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Map<String, Object>> queryForReport(Map<String, String> filters) {
        return store.values().stream()
                .map(account -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("userId", account.getUserId());
                    row.put("username", account.getUsername());
                    row.put("email", account.getEmail());
                    row.put("status", account.getStatus());
                    row.put("createdAt", account.getCreatedAt());
                    row.put("lastLoginAt", account.getLastLoginAt());
                    return row;
                })
                .collect(Collectors.toList());
    }
}
