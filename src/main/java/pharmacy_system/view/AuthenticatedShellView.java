package pharmacy_system.view;

import pharmacy_system.controller.common.NavigationController;
import pharmacy_system.controller.common.SessionController;
import pharmacy_system.controller.security_user.AuthenticateAuthoriseController;
import pharmacy_system.view.security_user.AccessDeniedView;
import pharmacy_system.view.security_user.LoginFormView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The authenticated application shell View, serving as the main container for
 * all role-filtered workspace views.
 *
 * <p>This View implements the outer frame of the authenticated application:
 * <ul>
 *   <li>Display the primary content area (changes based on active view).</li>
 *   <li>Display role-filtered navigation menu.</li>
 *   <li>Monitor session expiry and route to login if session times out
 *       (Requirement 1.4, Algorithm 4).</li>
 *   <li>Coordinate between content views, access-denied view, and login view.</li>
 *   <li>Provide logout functionality.</li>
 * </ul>
 *
 * <p>Requirements:
 * <ul>
 *   <li>FR-003, FR-004: Authenticate and present role-filtered workspace.</li>
 *   <li>FR-006: Monitor and enforce one-hour session expiry with re-authentication.</li>
 *   <li>SC-001: Single authenticated shell with role-filtered navigation.</li>
 *   <li>SC-003: Display access-denied error on unauthorised access attempt.</li>
 *   <li>SC-004: Require session validity for protected access.</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>This is the top-level container View. All role-specific content views are
 *       children or managed by this shell.</li>
 *   <li>Session expiry is monitored on a background timer; when detected, the shell
 *       routes to login and displays a session-expired message.</li>
 *   <li>The shell is responsible for coordinating between NavigationController
 *       (which holds the routing decisions) and the actual UI components.</li>
 * </ul>
 */
public class AuthenticatedShellView {
    private final SessionController sessionController;
    private final NavigationController navigationController;
    private final AuthenticateAuthoriseController authController;
    private final LoginFormView loginView;
    private final AccessDeniedView accessDeniedView;

    // Mapping of view names to view instances (populated by subclasses or configuration)
    private final Map<String, Object> registeredViews;

    // Session expiry monitoring
    private Timer sessionExpiryMonitor;
    private static final long SESSION_CHECK_INTERVAL_MS = 30_000; // Check every 30 seconds

    // UI state
    private boolean isVisible;
    private String currentContentViewName;
    private String sessionExpiryMessage;
    private boolean isSessionExpired;

    /**
     * Constructs an authenticated shell View with the required controller and
     * view dependencies.
     *
     * @param sessionController the session manager (used to check session state)
     * @param navigationController the navigation router (used for view routing)
     * @param authController the authentication controller (used for logout)
     * @param loginView the login form view (displayed on session expiry)
     * @param accessDeniedView the access-denied view (displayed on permission denial)
     * @throws NullPointerException if any parameter is null
     */
    public AuthenticatedShellView(
            SessionController sessionController,
            NavigationController navigationController,
            AuthenticateAuthoriseController authController,
            LoginFormView loginView,
            AccessDeniedView accessDeniedView) {
        this.sessionController = Objects.requireNonNull(sessionController, "sessionController must not be null");
        this.navigationController = Objects.requireNonNull(navigationController, "navigationController must not be null");
        this.authController = Objects.requireNonNull(authController, "authController must not be null");
        this.loginView = Objects.requireNonNull(loginView, "loginView must not be null");
        this.accessDeniedView = Objects.requireNonNull(accessDeniedView, "accessDeniedView must not be null");

        this.registeredViews = new HashMap<>();
        this.isVisible = false;
        this.currentContentViewName = null;
        this.sessionExpiryMessage = null;
        this.isSessionExpired = false;
    }

    /**
     * Shows the authenticated shell, starting session expiry monitoring.
     *
     * <p>This is called after successful login. The shell displays the appropriate
     * role-filtered home view and begins monitoring for session expiry.
     */
    public void show() {
        if (isVisible) {
            return; // Already visible
        }

        isVisible = true;
        isSessionExpired = false;
        sessionExpiryMessage = null;

        // Start session expiry monitoring (Requirement 1.4, Algorithm 4)
        startSessionExpiryMonitoring();

        // Navigate to the appropriate role-based home view
        navigationController.navigateToAuthorisedHome();
        updateCurrentContentView();
    }

    /**
     * Hides the authenticated shell and stops session monitoring.
     */
    public void hide() {
        isVisible = false;
        stopSessionExpiryMonitoring();
    }

    /**
     * Returns whether the shell is currently visible.
     *
     * @return {@code true} if visible; {@code false} otherwise
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Registers a content view with the shell so it can be displayed when the
     * NavigationController routes to it.
     *
     * <p>View names should correspond to the names used in NavigationController
     * registration (e.g., "PATIENT_HOME", "DOCTOR_HOME", "PHARMACIST_HOME",
     * "ADMIN_HOME").
     *
     * @param viewName the name of the view (used for routing)
     * @param viewInstance the view object to display when routed to
     */
    public void registerContentView(String viewName, Object viewInstance) {
        if (viewName != null && !viewName.isBlank()) {
            registeredViews.put(viewName, viewInstance);
        }
    }

    /**
     * Returns the currently displayed content view name, or null if no content
     * view is active.
     *
     * @return the current view name, or null
     */
    public String getCurrentContentViewName() {
        return currentContentViewName;
    }

    /**
     * Returns the currently active content view instance, or null if no view
     * is active.
     *
     * @return the current view instance, or null
     */
    public Object getCurrentContentView() {
        if (currentContentViewName != null) {
            return registeredViews.get(currentContentViewName);
        }
        return null;
    }

    /**
     * Returns whether the access-denied view should be displayed (i.e., an
     * unauthorised access attempt was made).
     *
     * @return {@code true} if access was denied; {@code false} otherwise
     */
    public boolean shouldShowAccessDenied() {
        return accessDeniedView.isVisible();
    }

    /**
     * Returns whether a session-expiry message should be displayed to the user.
     *
     * @return the message if session has expired, or null otherwise
     */
    public String getSessionExpiryMessage() {
        return sessionExpiryMessage;
    }

    /**
     * Returns whether the session has expired while the shell was active.
     *
     * @return {@code true} if session expired; {@code false} otherwise
     */
    public boolean hasSessionExpired() {
        return isSessionExpired;
    }

    /**
     * Handles a navigation request from a content view. Validates permissions
     * and updates the displayed content.
     *
     * <p>Flow:
     * <ol>
     *   <li>Validate session is still active (Requirement 1.4).</li>
     *   <li>Attempt to navigate via NavigationController.</li>
     *   <li>If navigation succeeds, update the displayed content view.</li>
     *   <li>If navigation fails due to insufficient permission, display
     *       access-denied view (Requirement 1.3).</li>
     * </ol>
     *
     * @param viewName the name of the target view
     * @return {@code true} if navigation succeeded; {@code false} otherwise
     */
    public boolean navigateTo(String viewName) {
        // Check session expiry (Requirement 1.4)
        if (sessionController.isExpired() || !sessionController.isAuthenticated()) {
            handleSessionExpiry();
            return false;
        }

        // Attempt to navigate
        boolean navigated = navigationController.navigateTo(viewName);

        if (!navigated) {
            // Navigation failed: display access-denied (Requirement 1.3)
            accessDeniedView.setAttemptedAction("access " + viewName);
            accessDeniedView.show();
            return false;
        }

        // Navigation succeeded: update content
        accessDeniedView.hide();
        updateCurrentContentView();
        return true;
    }

    /**
     * Handles the logout action, invalidating the session and returning to login.
     */
    public void onLogout() {
        stopSessionExpiryMonitoring();
        authController.logout();
        loginView.show();
        hide();
    }

    /**
     * Updates the displayed content view based on the current navigation state
     * from NavigationController.
     *
     * <p>This internal method synchronizes the shell's content display with the
     * routing decisions made by NavigationController.
     */
    private void updateCurrentContentView() {
        String navControllerView = navigationController.getCurrentViewName();

        // Check if view exists in registered views
        if (navControllerView != null && registeredViews.containsKey(navControllerView)) {
            currentContentViewName = navControllerView;
            // In a real implementation, this would show the actual UI for this view
        } else {
            // View not registered; log warning and do not change current view
            currentContentViewName = null;
        }
    }

    /**
     * Starts the background session expiry monitor. The monitor checks every
     * 30 seconds whether the session has expired. If so, it handles expiry
     * (Requirement 1.4, Algorithm 4).
     */
    private void startSessionExpiryMonitoring() {
        if (sessionExpiryMonitor != null) {
            return; // Already running
        }

        sessionExpiryMonitor = new Timer("SessionExpiryMonitor", true);
        sessionExpiryMonitor.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAndHandleSessionExpiry();
            }
        }, SESSION_CHECK_INTERVAL_MS, SESSION_CHECK_INTERVAL_MS);
    }

    /**
     * Stops the background session expiry monitor.
     */
    private void stopSessionExpiryMonitoring() {
        if (sessionExpiryMonitor != null) {
            sessionExpiryMonitor.cancel();
            sessionExpiryMonitor = null;
        }
    }

    /**
     * Checks for session expiry and handles it if detected. Called by the
     * background monitor.
     *
     * <p>Requirement 1.4: Session expires one hour after establishment. When
     * expiry is detected, the user is routed to login and re-authentication
     * is required.
     */
    private void checkAndHandleSessionExpiry() {
        if (!isVisible || isSessionExpired) {
            return; // Shell not active or already handled
        }

        if (sessionController.isExpired() || !sessionController.isAuthenticated()) {
            handleSessionExpiry();
        }
    }

    /**
     * Handles session expiry by routing to login and displaying a message.
     */
    private void handleSessionExpiry() {
        if (isSessionExpired) {
            return; // Already handled
        }

        isSessionExpired = true;
        sessionExpiryMessage = "Your session has expired. Please log in again.";

        // Stop monitoring
        stopSessionExpiryMonitoring();

        // Route to login
        navigationController.navigateToLogin();
        loginView.show();
        hide();
    }

    /**
     * Handles an access-denied condition by displaying the access-denied view
     * and providing context about the attempted action.
     *
     * @param attemptedAction a brief description of the action the user tried
     *        to perform
     */
    public void showAccessDenied(String attemptedAction) {
        // Check session expiry first (Requirement 1.4)
        if (sessionController.isExpired() || !sessionController.isAuthenticated()) {
            handleSessionExpiry();
            return;
        }

        // Display access-denied view (Requirement 1.3)
        accessDeniedView.setAttemptedAction(attemptedAction);
        accessDeniedView.show();
    }

    /**
     * Returns the role-filtered navigation menu items available to the current
     * user. This is used by the UI to populate navigation menus.
     *
     * <p>Menu items are determined by the user's permissions; only items for
     * which the user has permission are included.
     *
     * @return an array of navigation menu items (view names) the user can access
     */
    public String[] getRoleFilteredNavigation() {
        // This would typically return view names based on user's permissions
        // For now, return an empty array as placeholder; real implementation would
        // iterate over registered views and filter by permission
        return new String[0];
    }

    /**
     * Helper method for content views to check if a navigation action should
     * proceed. This should be called from content views before attempting
     * actions that require permission or session validity.
     *
     * @return {@code true} if the session is still valid; {@code false} if expired
     */
    public boolean isSessionValid() {
        if (sessionController.isExpired() || !sessionController.isAuthenticated()) {
            handleSessionExpiry();
            return false;
        }
        return true;
    }
}
