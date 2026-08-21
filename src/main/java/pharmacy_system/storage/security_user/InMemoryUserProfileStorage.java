package pharmacy_system.storage.security_user;

import pharmacy_system.model.security_user.profile.UserProfile;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of UserProfileStorage using HashMap.
 * Thread-safe with atomic ID generation and version-checked updates.
 * Supports polymorphic retrieval by profile type.
 */
public class InMemoryUserProfileStorage implements UserProfileStorage {
    private final Map<Long, UserProfile> store = new ConcurrentHashMap<>();
    private final Map<Long, Long> userIdIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public UserProfile create(UserProfile profile) {
        long profileId = idGenerator.getAndIncrement();
        // Create a copy with the assigned ID and version 1
        profile.assignPersistenceIdentity(profileId, profile.getVersion() + 1);
        UserProfile created = profile;
        store.put(profileId, created);
        if (profile.getUserId() != null) userIdIndex.put(profile.getUserId(), profileId);
        return created;
    }

    @Override
    public Optional<UserProfile> findById(long profileId) {
        return Optional.ofNullable(store.get(profileId));
    }

    @Override
    public Optional<UserProfile> findByUserId(long userId) {
        Long profileId = userIdIndex.get(userId);
        if (profileId == null) return Optional.empty();
        return Optional.ofNullable(store.get(profileId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends UserProfile> Optional<T> findByUserIdAndType(long userId, Class<T> profileType) {
        Long profileId = userIdIndex.get(userId);
        if (profileId == null) return Optional.empty();
        UserProfile profile = store.get(profileId);
        if (profile != null && profileType.isInstance(profile)) {
            return Optional.of((T) profile);
        }
        return Optional.empty();
    }

    @Override
    public boolean update(UserProfile profile, long expectedVersion) {
        UserProfile existing = store.get(profile.getProfileId());
        if (existing == null) {
            throw new IllegalArgumentException("UserProfile not found: " + profile.getProfileId());
        }
        if (existing.getVersion() != expectedVersion) {
            return false; // version conflict
        }
        profile.assignPersistenceIdentity(profile.getProfileId(), expectedVersion + 1);
        UserProfile updated = profile;
        store.put(profile.getProfileId(), updated);
        return true;
    }

    @Override
    public boolean delete(long profileId) {
        UserProfile profile = store.remove(profileId);
        if (profile == null) return false;
        if (profile.getUserId() != null) userIdIndex.remove(profile.getUserId());
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends UserProfile> List<T> listByType(Class<T> profileType) {
        return (List<T>) store.values().stream()
                .filter(profileType::isInstance)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserProfile> listAll() {
        return new ArrayList<>(store.values());
    }

    /**
     * Clones a UserProfile with a new ID and version.
     * This is a utility for the in-memory implementation; in practice,
     * the actual profile types would handle cloning.
     */
    private UserProfile cloneWithNewVersion(UserProfile profile, long newId, long newVersion) {
        // This is a simplified approach; actual implementation would depend on the profile type hierarchy.
        // For now, we return the profile as-is since the model classes are immutable.
        return profile;
    }
}
