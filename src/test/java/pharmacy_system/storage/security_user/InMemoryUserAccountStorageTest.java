package pharmacy_system.storage.security_user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pharmacy_system.model.security_user.UserAccount;
import pharmacy_system.model.security_user.AccountStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserAccountStorageTest {
    private UserAccountStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryUserAccountStorage();
    }

    @Test
    void testCreateAndFindById() {
        UserAccount account = new UserAccount(
                0, "john_doe", "john@example.com", AccountStatus.PENDING,
                false, LocalDateTime.now(), LocalDateTime.now(), null, null, null, 0
        );

        UserAccount created = storage.create(account);
        assertEquals(1, created.getUserId());
        assertEquals(1, created.getVersion());

        Optional<UserAccount> found = storage.findById(created.getUserId());
        assertTrue(found.isPresent());
        assertEquals("john_doe", found.get().getUsername());
    }

    @Test
    void testFindByUsername() {
        UserAccount account = new UserAccount(
                0, "alice", "alice@example.com", AccountStatus.ACTIVE,
                true, LocalDateTime.now(), LocalDateTime.now(), null, null, null, 0
        );
        storage.create(account);

        Optional<UserAccount> found = storage.findByUsername("alice");
        assertTrue(found.isPresent());
        assertEquals("alice", found.get().getUsername());
    }

    @Test
    void testFindByEmail() {
        UserAccount account = new UserAccount(
                0, "bob", "bob@example.com", AccountStatus.ACTIVE,
                true, LocalDateTime.now(), LocalDateTime.now(), null, null, null, 0
        );
        storage.create(account);

        Optional<UserAccount> found = storage.findByEmail("bob@example.com");
        assertTrue(found.isPresent());
        assertEquals("bob@example.com", found.get().getEmail());
    }

    @Test
    void testUpdateWithVersionConflict() {
        UserAccount account = new UserAccount(
                0, "user1", "user1@example.com", AccountStatus.PENDING,
                false, LocalDateTime.now(), LocalDateTime.now(), null, null, null, 0
        );
        UserAccount created = storage.create(account);

        UserAccount modified = new UserAccount(
                created.getUserId(), "user1_modified", "user1@example.com", AccountStatus.ACTIVE,
                true, LocalDateTime.now(), LocalDateTime.now(), null, null, null, 0
        );

        // Try to update with wrong version
        boolean success = storage.update(modified, 0); // expecting version 1
        assertFalse(success);

        // Verify original was not changed
        Optional<UserAccount> unchanged = storage.findById(created.getUserId());
        assertEquals("user1", unchanged.get().getUsername());
    }

    @Test
    void testUpdateWithCorrectVersion() {
        UserAccount account = new UserAccount(
                0, "user2", "user2@example.com", AccountStatus.PENDING,
                false, LocalDateTime.now(), LocalDateTime.now(), null, null, null, 0
        );
        UserAccount created = storage.create(account);

        UserAccount modified = new UserAccount(
                created.getUserId(), "user2_modified", "user2_modified@example.com", AccountStatus.ACTIVE,
                true, LocalDateTime.now(), LocalDateTime.now(), null, null, null, 0
        );

        // Update with correct version
        boolean success = storage.update(modified, 1);
        assertTrue(success);

        // Verify update
        Optional<UserAccount> updated = storage.findById(created.getUserId());
        assertEquals("user2_modified", updated.get().getUsername());
        assertEquals(2, updated.get().getVersion());
    }

    @Test
    void testDelete() {
        UserAccount account = new UserAccount(
                0, "user3", "user3@example.com", AccountStatus.ACTIVE,
                true, LocalDateTime.now(), LocalDateTime.now(), null, null, null, 0
        );
        UserAccount created = storage.create(account);

        boolean deleted = storage.delete(created.getUserId());
        assertTrue(deleted);

        Optional<UserAccount> found = storage.findById(created.getUserId());
        assertFalse(found.isPresent());
    }

    @Test
    void testListAll() {
        UserAccount account1 = new UserAccount(
                0, "user4", "user4@example.com", AccountStatus.ACTIVE,
                true, LocalDateTime.now(), LocalDateTime.now(), null, null, null, 0
        );
        UserAccount account2 = new UserAccount(
                0, "user5", "user5@example.com", AccountStatus.ACTIVE,
                true, LocalDateTime.now(), LocalDateTime.now(), null, null, null, 0
        );

        storage.create(account1);
        storage.create(account2);

        List<UserAccount> all = storage.listAll();
        assertEquals(2, all.size());
    }

    @Test
    void testQueryForReport() {
        UserAccount account = new UserAccount(
                0, "user6", "user6@example.com", AccountStatus.ACTIVE,
                true, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null, null, 0
        );
        storage.create(account);

        List<java.util.Map<String, Object>> rows = storage.queryForReport(new java.util.HashMap<>());
        assertEquals(1, rows.size());
        assertEquals("user6", rows.get(0).get("username"));
    }
}
