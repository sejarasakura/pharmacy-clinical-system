package pharmacy_system.view.security_user;

import pharmacy_system.controller.common.NavigationController;
import pharmacy_system.controller.common.SessionController;

import java.util.Objects;

/**
 * The access-denied View, displayed when a user attempts to access a protected
 * function or view for which their role lacks permission.
 *
 * <p>This View implements part of the UCD-04 (Authenticate & Authorise) authorization flow:
 * <ul>
 *   <li>Display a message indicating the user does not have permission.</li>
 *   <li>Provide a button to navigate back to the authorised home view.</li>
 *   <li>Monitor session expiry and route to login if session expires.</li>
 * </ul>
 *
 * <p>Requirements:
 * <ul>
 *   <li>FR-003, FR-004: Deny access to protected functions lacking permission.</li>
 *   <li>SC-003: Display authorisation error indicating function not permitted.</li>
 *   <li>SC-004: Require session validity for protected access.</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>This is a transient state view; users spend little time here before
 *       navigating back to an authorised view.</li>
 *   <li>The view does not attempt to perform the forbidden operation; it only
 *       informs and redirects.</li>
 * </ul>
 */
public class AccessDeniedView {
    private final SessionController sessionController;
    private final NavigationController navigationController;

    // UI state
    private String denialMessage;
    private String attemptedAction;
    private boolean isVisible;

    /**
     * Constructs an access-denied View with the required controller dependencies.
     *
     * @param sessionController the session manager (used to check session state
     *        and user identity)
     * @param navigationController the navigation router (used to navigate back
     *        to home or login)
     * @throws NullPointerException if any parameter is null
     */
    public AccessDeniedView(
            SessionController sessionController,
            NavigationController navigationController) {
        this.sessionController = Objects.requireNonNull(sessionController, "sessionController must not be null");
        this.navigationController = Objects.requireNonNull(navigationController,
                "navigationController must not be null");

        this.denialMessage = "You do not have permission to access this function.";
        this.attemptedAction = null;
        this.isVisible = false;
    }

    /**
     * Displays the access-denied view. In a real desktop implementation, this would
     * show a dialog or view with the denial message and a back button.
     */
    public void show() {
        isVisible = true;
    }

    /**
     * Hides the access-denied view.
     */
    public void hide() {
        isVisible = false;
        this.denialMessage = "You do not have permission to access this function.";
        this.attemptedAction = null;
    }

    /**
     * Returns whether the access-denied view is currently visible.
     *
     * @return {@code true} if visible; {@code false} otherwise
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Sets the message explaining why access was denied. This can be a generic
     * message or a more specific one if the attempted operation is known.
     *
     * @param message the denial message to display
     */
    public void setDenialMessage(String message) {
        this.denialMessage = message != null ? message : "You do not have permission to access this function.";
    }

    /**
     * Returns the current denial message.
     *
     * @return the message displayed to the user
     */
    public String getDenialMessage() {
        return denialMessage;
    }

    /**
     * Sets a description of the action the user attempted, for context in the
     * error message. For example, "generate reports" or "dispense medication".
     *
     * @param action a brief description of the attempted action
     */
    public void setAttemptedAction(String action) {
        this.attemptedAction = action;
    }

    /**
     * Returns the description of the attempted action, if any.
     *
     * @return the action description, or null if not set
     */
    public String getAttemptedAction() {
        return attemptedAction;
    }

    /**
     * Builds and returns a complete denial message incorporating the attempted
     * action (if known) and the base denial message.
     *
     * @return a formatted message suitable for display to the user
     */
    public String getFormattedMessage() {
        if (attemptedAction != null && !attemptedAction.isBlank()) {
            return "You attempted to " + attemptedAction + ". " + denialMessage;
        }
        return denialMessage;
    }

    /**
     * Handles the back button action, routing the user back to the authenticated
     * home view.
     *
     * <p>This method also validates that the session is still active; if the
     * session has expired, the user is routed to login instead (Requirement 1.4).
     *
     * @return {@code true} if navigation succeeded; {@code false} otherwise
     */
    public boolean onBackButtonClicked() {
        // Check session expiry (Requirement 1.4)
        if (sessionController.isExpired() || !sessionController.isAuthenticated()) {
            // Session expired: route to login
            boolean navigated = navigationController.navigateToLogin();
            hide();
            return navigated;
        }

        // Session valid: navigate back to home
        boolean navigated = navigationController.navigateToAuthorisedHome();
        hide();
        return navigated;
    }

    /**
     * Checks whether the current session has expired while the user was viewing
     * this access-denied message.
     *
     * <p>Requirement 1.4: Session expiry is checked on every protected operation.
     * If the session has expired while the user is on this view, they are routed
     * to login.
     *
     * @return {@code true} if the session is valid; {@code false} if expired
     *         and the user has been routed to login
     */
    public boolean checkSessionExpiry() {
        if (sessionController.isExpired() || !sessionController.isAuthenticated()) {
            // Session expired: route to login
            navigationController.navigateToLogin();
            hide();
            return false;
        }
        return true;
    }
}
