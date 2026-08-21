package pharmacy_system.view.security_user;

import pharmacy_system.controller.common.NavigationController;
import pharmacy_system.controller.common.SessionController;
import pharmacy_system.controller.security_user.AuthenticateAuthoriseController;

import java.util.Objects;

/**
 * The login form View, responsible for presenting the authentication interface
 * to unauthenticated users and handling credential submission.
 *
 * <p>This View implements the UCD-04 login flow:
 * <ul>
 *   <li>Present username and password input fields.</li>
 *   <li>Submit credentials to AuthenticateAuthoriseController.authenticate().</li>
 *   <li>On success: navigate to the authenticated shell.</li>
 *   <li>On failure: display a generic authentication error (does not distinguish
 *       between invalid username and invalid password per Requirement 1.2).</li>
 * </ul>
 *
 * <p>Requirements:
 * <ul>
 *   <li>FR-003, FR-004: Authenticate and present role-filtered workspace on success.</li>
 *   <li>FR-006: Session established with one-hour expiry.</li>
 *   <li>SC-002: Generic authentication error (not username-specific, not password-specific).</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>This is a desktop View (not web). Implementation details (Swing, JavaFX, etc.)
 *       are deferred; this class defines the contract and behaviour.</li>
 *   <li>No authentication state is stored in this View; the SessionController holds
 *       all authenticated state.</li>
 *   <li>On successful login, the View yields control to the authenticated shell.</li>
 * </ul>
 */
public class LoginFormView {
    private final AuthenticateAuthoriseController authController;
    private final NavigationController navigationController;
    private final SessionController sessionController;

    // UI state (would be backed by actual UI components in a real implementation)
    private String usernameInput;
    private char[] passwordInput;
    private String errorMessage;
    private boolean isVisible;

    /**
     * Constructs a login form View with the required controller dependencies.
     *
     * @param authController the authentication and authorisation controller
     * @param navigationController the navigation router (used to redirect to shell
     *        on successful login)
     * @param sessionController the session manager (used to check session state)
     * @throws NullPointerException if any parameter is null
     */
    public LoginFormView(
            AuthenticateAuthoriseController authController,
            NavigationController navigationController,
            SessionController sessionController) {
        this.authController = Objects.requireNonNull(authController, "authController must not be null");
        this.navigationController = Objects.requireNonNull(navigationController,
                "navigationController must not be null");
        this.sessionController = Objects.requireNonNull(sessionController, "sessionController must not be null");

        this.usernameInput = "";
        this.passwordInput = new char[0];
        this.errorMessage = null;
        this.isVisible = true;
    }

    /**
     * Displays the login form. In a real desktop implementation, this would
     * initialize and show the UI window. Here it marks the view as visible.
     */
    public void show() {
        isVisible = true;
        clearForm();
    }

    /**
     * Hides the login form. In a real implementation, this would close the
     * UI window. Here it marks the view as not visible.
     */
    public void hide() {
        isVisible = false;
        clearForm();
    }

    /**
     * Returns whether the login form is currently visible.
     *
     * @return {@code true} if the form is shown; {@code false} otherwise
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Sets the username input field value. In a real implementation, this would
     * reflect user input from a text field.
     *
     * @param username the username to set
     */
    public void setUsernameInput(String username) {
        this.usernameInput = username != null ? username : "";
    }

    /**
     * Returns the current username input.
     *
     * @return the username from the input field
     */
    public String getUsernameInput() {
        return usernameInput;
    }

    /**
     * Sets the password input field value. In a real implementation, this would
     * be masked in the UI.
     *
     * @param password the raw password characters to set
     */
    public void setPasswordInput(char[] password) {
        this.passwordInput = password != null ? password.clone() : new char[0];
    }

    /**
     * Returns the current password input (for authentication submission).
     *
     * @return a clone of the password characters
     */
    public char[] getPasswordInput() {
        return passwordInput.clone();
    }

    /**
     * Returns the error message currently displayed, or null if no error
     * is shown.
     *
     * @return the error message or null
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Clears the form, resetting all input fields and error messages.
     * Called on successful login and when showing the form fresh.
     */
    private void clearForm() {
        this.usernameInput = "";
        this.passwordInput = new char[0];
        this.errorMessage = null;
    }

    /**
     * Handles the login button submit action. Coordinates with the authentication
     * controller to verify credentials.
     *
     * <p>Flow:
     * <ol>
     *   <li>Validate that username and password are provided (not blank).</li>
     *   <li>Submit to authController.authenticate(username, password).</li>
     *   <li>On success: clear form, navigate to authenticated shell.</li>
     *   <li>On failure: display generic authentication error (Requirement 1.2).</li>
     * </ol>
     *
     * @return {@code true} if authentication succeeded and navigation to the
     *         authenticated shell was successful; {@code false} if authentication
     *         failed or navigation failed
     */
    public boolean onLoginSubmit() {
        // Validate input (both fields required)
        if (usernameInput == null || usernameInput.isBlank() || passwordInput.length == 0) {
            displayAuthenticationError("Username and password are required.");
            return false;
        }

        // Submit to authentication controller
        boolean authenticated = authController.authenticate(usernameInput, passwordInput);

        if (!authenticated) {
            // Authentication failed: display generic error (Requirement 1.2)
            displayAuthenticationError("Invalid username or password.");
            return false;
        }

        // Authentication succeeded: navigate to authenticated shell
        clearForm();

        // Verify session is established (should be after successful authenticate)
        if (!sessionController.isAuthenticated()) {
            displayAuthenticationError("Session establishment failed. Please try again.");
            return false;
        }

        // Navigate to the role-appropriate home view
        boolean navigated = navigationController.navigateToAuthorisedHome();
        if (!navigated) {
            displayAuthenticationError("Navigation to workspace failed. Please try again.");
            return false;
        }

        // Success: hide this view
        hide();
        return true;
    }

    /**
     * Handles the logout action, invalidating the session and returning to the
     * login form.
     */
    public void onLogout() {
        authController.logout();
        clearForm();
        show();
    }

    /**
     * Displays an authentication error message to the user. In a real implementation,
     * this would update a UI label or alert dialog.
     *
     * <p>Per Requirement 1.2, the error message is generic and does not reveal
     * whether the username or password was incorrect.
     *
     * @param message the error message to display
     */
    private void displayAuthenticationError(String message) {
        this.errorMessage = message;
        // In a real implementation, this would update the UI error label
    }

    /**
     * Checks whether a session has expired while the login view may be displayed
     * after a timeout. If expired, clears the form and displays an appropriate
     * message.
     *
     * <p>This method is typically called from the authenticated shell's session
     * monitoring to detect expiry and route back to login (Requirement 1.4).
     *
     * @return {@code true} if the session is valid; {@code false} if expired
     */
    public boolean checkSessionExpiry() {
        if (sessionController.isExpired() || !sessionController.isAuthenticated()) {
            clearForm();
            displayAuthenticationError("Your session has expired. Please log in again.");
            show();
            return false;
        }
        return true;
    }
}
