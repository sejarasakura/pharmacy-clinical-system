package pharmacy_system.controller.common;

import pharmacy_system.model.security_user.RolePermission;
import pharmacy_system.model.security_user.UserAccount;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Manages the authenticated session lifecycle for the current user, enforcing
 * time-based expiry (one hour) and idle timeout.
 *
 * <p>Implements Algorithm 4 (Session Expiry Enforcement with Idle Timeout) from
 * the design document. Session expiry is checked on every protected operation
 * invocation via {@link #requirePermission(String)}.
 *
 * <p>Requirements:
 * <ul>
 *   <li>FR-006: Session expires one hour after establishment.</li>
 *   <li>SC-016: Session expiry check on protected operations.</li>
 * </ul>
 *
 * <p>Web mutation handlers use Post/Redirect/Get: they add a presentation-only
 * {@code Flash} through {@code RedirectAttributes}, then return a
 * {@code redirect:} view name.
 */
public class SessionController {
    private long userId;
    private String username;
    private String roleName;
    private Set<String> permissionCodes;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastActivityAt;
    private boolean authenticated;
    private long idleTimeoutMinutes;

    /**
     * Constructs a session controller with default idle timeout of 30 minutes.
     */
    public SessionController() {
        this(30);
    }

    /**
     * Constructs a session controller with a custom idle timeout.
     *
     * @param idleTimeoutMinutes the idle timeout in minutes; must be positive
     */
    public SessionController(long idleTimeoutMinutes) {
        if (idleTimeoutMinutes <= 0) {
            throw new IllegalArgumentException("idleTimeoutMinutes must be positive");
        }
        this.userId = 0L;
        this.username = "";
        this.roleName = "";
        this.permissionCodes = Collections.emptySet();
        this.createdAt = null;
        this.expiresAt = null;
        this.lastActivityAt = null;
        this.authenticated = false;
        this.idleTimeoutMinutes = idleTimeoutMinutes;
    }

    /**
     * Establishes a new authenticated session for the given user account and
     * assigned roles.
     *
     * <p>Session expiry is set to one hour from establishment (FR-006).
     * Last activity is initialized to creation time.
     *
     * @param account the authenticated user account
     * @param roles the operational roles assigned to the user
     * @throws NullPointerException if account or roles is null
     */
    public void establishSession(UserAccount account, List<RolePermission> roles) {
        Objects.requireNonNull(account, "account must not be null");
        Objects.requireNonNull(roles, "roles must not be null");

        this.userId = account.getUserId();
        this.username = account.getUsername();
        this.roleName = roles.isEmpty() ? "" : roles.get(0).getRoleName();
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusHours(1); // FR-006: one hour session lifetime
        this.lastActivityAt = this.createdAt;
        this.authenticated = true;

        // Aggregate all permission codes from all assigned roles
        Set<String> combinedPermissions = new HashSet<>();
        for (RolePermission role : roles) {
            combinedPermissions.addAll(role.getPermissionCodes());
        }
        this.permissionCodes = Collections.unmodifiableSet(combinedPermissions);
    }

    /**
     * Returns the user ID of the currently authenticated session, or 0 if no
     * session is established.
     *
     * @return the user ID, or 0 if not authenticated
     */
    public long getCurrentUserId() {
        return userId;
    }

    public String getCurrentUsername() {
        return username;
    }

    public String getCurrentRole() {
        return roleName;
    }

    /**
     * Returns whether a session is currently established.
     *
     * @return {@code true} only when a session exists and is not expired
     */
    public boolean isAuthenticated() {
        return authenticated && !isExpired();
    }

    /**
     * Returns whether the current session has expired based on the one-hour
     * absolute limit from establishment.
     *
     * <p>Algorithm 4 implementation: absolute expiry = createdAt + 1 hour.
     *
     * @return {@code true} when {@code now > expiresAt}
     */
    public boolean isExpired() {
        if (!authenticated || expiresAt == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Returns whether the current session is idle (no activity for the configured
     * idle timeout period).
     *
     * @return {@code true} when {@code now > lastActivityAt + idleTimeoutMinutes}
     */
    public boolean isIdleTimedOut() {
        if (!authenticated || lastActivityAt == null) {
            return true;
        }
        LocalDateTime idleDeadline = lastActivityAt.plus(idleTimeoutMinutes, ChronoUnit.MINUTES);
        return LocalDateTime.now().isAfter(idleDeadline);
    }

    /**
     * Returns whether the current session holds the given permission code.
     *
     * <p>Permission check does not enforce session expiry; use
     * {@link #requirePermission(String)} for the full access-control check.
     *
     * @param permissionCode the permission code to check
     * @return {@code true} only when the session grants the code
     */
    public boolean hasPermission(String permissionCode) {
        return authenticated && permissionCode != null && permissionCodes.contains(permissionCode);
    }

    /**
     * Enforces both session validity and permission grant for a protected
     * operation.
     *
     * <p>Algorithm 4 implementation: verifies that the session is authenticated,
     * not expired, and grants the required permission. If any check fails, an
     * exception is raised and the operation is denied.
     *
     * <p>Requirement 1.3 (access-control denial) and 1.4 (session expiry denial).
     *
     * @param permissionCode the required permission code
     * @throws SessionExpiredException when the session has expired or no session is established
     * @throws InsufficientPermissionException when the session lacks the required permission
     */
    public void requirePermission(String permissionCode) {
        // Check session authentication and expiry
        if (!authenticated) {
            throw new SessionExpiredException("No session established");
        }
        if (isExpired()) {
            throw new SessionExpiredException("Session has expired");
        }

        // Check permission grant
        if (!permissionCodes.contains(permissionCode)) {
            throw new InsufficientPermissionException(
                    "Insufficient permission: " + permissionCode + " not granted");
        }

        // Update last activity to reset idle timeout
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * Invalidates the current session, clearing all state and requiring
     * re-authentication for subsequent operations.
     */
    public void invalidateSession() {
        this.userId = 0L;
        this.username = "";
        this.roleName = "";
        this.permissionCodes = Collections.emptySet();
        this.createdAt = null;
        this.expiresAt = null;
        this.lastActivityAt = null;
        this.authenticated = false;
    }

    /**
     * Returns session creation time, or null if no session is established.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns session expiry time, or null if no session is established.
     *
     * @return the expiry timestamp
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * Returns the last activity timestamp, used to track idle timeout.
     *
     * @return the last activity timestamp
     */
    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    /**
     * Represents an exception raised when a session has expired or no session
     * is established.
     */
    public static class SessionExpiredException extends RuntimeException {
        /**
         * Constructs a session-expired exception with the given message.
         *
         * @param message the error message
         */
        public SessionExpiredException(String message) {
            super(message);
        }
    }

    /**
     * Represents an exception raised when an authenticated session lacks the
     * required permission for an operation.
     */
    public static class InsufficientPermissionException extends RuntimeException {
        /**
         * Constructs an insufficient-permission exception with the given message.
         *
         * @param message the error message
         */
        public InsufficientPermissionException(String message) {
            super(message);
        }
    }
}
