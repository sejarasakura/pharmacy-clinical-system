package pharmacy_system.storage.security_user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pharmacy_system.model.security_user.Credential;
import pharmacy_system.model.security_user.Sha256PasswordHasher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCredentialStorageTest {
    private CredentialStorage storage;
    private Sha256PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new Sha256PasswordHasher();
        storage = new InMemoryCredentialStorage(hasher);
    }

    @Test
    void testCreateAndFindById() {
        String passwordHash = hasher.hash("password123");
        Credential credential = new Credential(
                0, 1, passwordHash, 0, null, LocalDateTime.now(), hasher
        );

        Credential created = storage.create(credential);
        assertEquals(1, created.getCredentialId());
        assertEquals(1, created.getVersion());

        Optional<Credential> found = storage.findById(created.getCredentialId());
        assertTrue(found.isPresent());
        assertEquals(1, found.get().getUserId());
    }

    @Test
    void testFindByUserId() {
        String passwordHash = hasher.hash("password456");
        Credential credential = new Credential(
                0, 2, passwordHash, 0, null, LocalDateTime.now(), hasher
        );
        storage.create(credential);

        Optional<Credential> found = storage.findByUserId(2);
        assertTrue(found.isPresent());
        assertEquals(2, found.get().getUserId());
    }

    @Test
    void testUpdateWithVersionConflict() {
        String passwordHash = hasher.hash("oldpassword");
        Credential credential = new Credential(
                0, 3, passwordHash, 0, null, LocalDateTime.now(), hasher
        );
        Credential created = storage.create(credential);

        String newPasswordHash = hasher.hash("newpassword");
        Credential modified = new Credential(
                created.getCredentialId(), 3, newPasswordHash, 0, null, LocalDateTime.now(), hasher
        );

        // Try to update with wrong version
        boolean success = storage.update(modified, 0);
        assertFalse(success);
    }

    @Test
    void testUpdateWithCorrectVersion() {
        String passwordHash = hasher.hash("password789");
        Credential credential = new Credential(
                0, 4, passwordHash, 0, null, LocalDateTime.now(), hasher
        );
        Credential created = storage.create(credential);

        String newPasswordHash = hasher.hash("newpassword789");
        Credential modified = new Credential(
                created.getCredentialId(), 4, newPasswordHash, 0, null, LocalDateTime.now(), hasher
        );

        boolean success = storage.update(modified, 1);
        assertTrue(success);

        Optional<Credential> updated = storage.findById(created.getCredentialId());
        assertEquals(2, updated.get().getVersion());
    }

    @Test
    void testDelete() {
        String passwordHash = hasher.hash("password");
        Credential credential = new Credential(
                0, 5, passwordHash, 0, null, LocalDateTime.now(), hasher
        );
        Credential created = storage.create(credential);

        boolean deleted = storage.delete(created.getCredentialId());
        assertTrue(deleted);

        Optional<Credential> found = storage.findById(created.getCredentialId());
        assertFalse(found.isPresent());
    }
}
