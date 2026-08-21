package pharmacy_system.model.security_user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Placeholder {@link PasswordHasher} used until the adaptive password-hashing
 * algorithm decision is made (see {@code storage_notdone.md}, which lists the
 * password hashing algorithm as not yet defined). This salts each password
 * with a random value and hashes it with SHA-256.
 *
 * <p><strong>This is not an adaptive KDF</strong> (e.g. bcrypt/scrypt/Argon2)
 * and must be replaced with one before this system is used in production;
 * it exists solely so {@link Credential} has a working default implementation
 * of the hashing seam.</p>
 */
public final class Sha256PasswordHasher implements PasswordHasher {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH_BYTES = 16;
    private static final String SEPARATOR = ":";

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String hash(char[] password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        byte[] digest = digest(password, salt);
        return encode(salt) + SEPARATOR + encode(digest);
    }

    @Override
    public boolean matches(char[] password, String passwordHash) {
        if (password == null || passwordHash == null) {
            return false;
        }
        String[] parts = passwordHash.split(SEPARATOR, 2);
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = decode(parts[0]);
        byte[] expectedDigest = decode(parts[1]);
        byte[] actualDigest = digest(password, salt);
        return MessageDigest.isEqual(expectedDigest, actualDigest);
    }

    private static byte[] digest(char[] password, byte[] salt) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);
            messageDigest.update(salt);
            messageDigest.update(toBytes(password));
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITHM + " is not available", e);
        }
    }

    private static byte[] toBytes(char[] password) {
        byte[] bytes = new byte[password == null ? 0 : password.length];
        if (password != null) {
            for (int i = 0; i < password.length; i++) {
                bytes[i] = (byte) password[i];
            }
        }
        return bytes;
    }

    private static String encode(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

    private static byte[] decode(String value) {
        return Base64.getDecoder().decode(value);
    }
}
