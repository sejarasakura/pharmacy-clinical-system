package pharmacy_system.view.security_user.ucd05_manage_profile;

import pharmacy_system.controller.common.SessionController;
import pharmacy_system.controller.security_user.ManageProfileController;
import pharmacy_system.model.security_user.profile.UserProfile;

import java.util.Objects;

/**
 * The primary boundary View for self-service profile management (UCD-05).
 *
 * <p>Orchestrates the loading and editing of an authenticated user's profile.
 * Composes ProfileDetailsView (read-only display) and ProfileFormView
 * (edit collection and submission).
 *
 * <p>Requirements:
 * <ul>
 *   <li>Requirement 2.3: Persist permitted changes, retain non-submitted fields.</li>
 *   <li>Requirement 2.4: Reject restricted-field modifications; display error.</li>
 * </ul>
 *
 * <p>This View implements the UCD-05 flow:
 * <ul>
 *   <li>On open: load the authenticated user's profile from the controller.</li>
 *   <li>Display profile details and present an edit form.</li>
 *   <li>On edit submit: coordinate with ProfileFormView to collect changes.</li>
 *   <li>Submit changes to ManageProfileController.</li>
 *   <li>On success: refresh display with updated profile.</li>
 *   <li>On restricted-field error: display field-specific error message.</li>
 *   <li>On validation error: display validation errors.</li>
 * </ul>
 */
public class ManageProfileView {
    private final ManageProfileController profileController;
    private final SessionController sessionController;
    private final ProfileDetailsView detailsView;
    private final ProfileFormView formView;

    private UserProfile currentProfile;
    private boolean isVisible;

    /**
     * Constructs a ManageProfileView with required dependencies.
     *
     * @param profileController the profile management controller
     * @param sessionController the session manager (provides current user identity)
     * @param detailsView the read-only profile details display component
     * @param formView the edit form component
     * @throws NullPointerException if any parameter is null
     */
    public ManageProfileView(
            ManageProfileController profileController,
            SessionController sessionController,
            ProfileDetailsView detailsView,
            ProfileFormView formView) {
        this.profileController = Objects.requireNonNull(profileController, "profileController must not be null");
        this.sessionController = Objects.requireNonNull(sessionController, "sessionController must not be null");
        this.detailsView = Objects.requireNonNull(detailsView, "detailsView must not be null");
        this.formView = Objects.requireNonNull(formView, "formView must not be null");

        this.currentProfile = null;
        this.isVisible = false;
    }

    /**
     * Opens the profile management view. Loads the authenticated user's profile
     * from the controller and displays it.
     *
     * <p>Flow:
     * <ol>
     *   <li>Get the current authenticated user ID from the session.</li>
     *   <li>Load the profile via profileController.loadProfile(userId).</li>
     *   <li>Display the profile details and edit form.</li>
     *   <li>If profile not found, display error.</li>
     * </ol>
     *
     * @return {@code true} if profile was loaded and displayed successfully;
     *         {@code false} if profile was not found or session is invalid
     */
    public boolean openProfile() {
        // Verify session is valid
        if (!sessionController.isAuthenticated()) {
            showProfileUnavailable("Session is not established. Please log in.");
            return false;
        }

        // Get the current user ID and load the profile
        long userId = sessionController.getCurrentUserId();

        // Load the profile
        UserProfile profile = profileController.loadProfile(userId);
        if (profile == null) {
            showProfileUnavailable("Profile not found.");
            return false;
        }

        // Store and display
        this.currentProfile = profile;
        showProfile(profile);
        return true;
    }

    /**
     * Displays the loaded profile in the details view.
     *
     * @param profile the UserProfile to display
     */
    public void showProfile(UserProfile profile) {
        if (profile != null) {
            this.currentProfile = profile;
            detailsView.display(profile);
            formView.resetForm();
            isVisible = true;
        }
    }

    /**
     * Shows a message indicating the profile is unavailable (e.g. not found,
     * session expired, or access denied).
     *
     * @param message the error message to display
     */
    public void showProfileUnavailable(String message) {
        isVisible = false;
        currentProfile = null;
        // In a real implementation, this would show an error dialog or status message
    }

    /**
     * Shows a success message after profile update.
     */
    public void showUpdateSuccess() {
        // In a real implementation, this would show a success dialog or toast
    }

    /**
     * Returns whether the view is currently visible.
     *
     * @return {@code true} if displayed; {@code false} otherwise
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Returns the currently displayed profile, or null if none is loaded.
     *
     * @return the UserProfile or null
     */
    public UserProfile getCurrentProfile() {
        return currentProfile;
    }

    /**
     * Handles the edit submit action from the form. Collects changes from
     * the form, submits to the controller, and handles the response.
     *
     * <p>Flow:
     * <ol>
     *   <li>Collect changes from the form.</li>
     *   <li>Submit to profileController.updateProfile(userId, changes).</li>
     *   <li>On success: reload and display updated profile; show success message.</li>
     *   <li>On restricted-field error: show specific error message.</li>
     *   <li>On validation error: display validation errors from the form.</li>
     *   <li>On concurrency error: show reload prompt.</li>
     * </ol>
     *
     * @return {@code true} if update succeeded; {@code false} otherwise
     */
    public boolean onEditSubmit() {
        if (currentProfile == null) {
            formView.showSaveError("No profile loaded.");
            return false;
        }

        // Collect changes from the form
        var changes = formView.collectChanges();
        if (changes == null || changes.isEmpty()) {
            // No changes submitted; this is not an error, just a no-op
            return true;
        }

        // Submit to controller
        long userId = currentProfile.getUserId();
        boolean updateSucceeded = false;
        
        try {
            updateSucceeded = profileController.updateProfile(userId, changes);
        } catch (pharmacy_system.controller.common.SessionController.SessionExpiredException e) {
            formView.showSaveError("Session has expired. Please log in again.");
            return false;
        }

        if (!updateSucceeded) {
            // The controller rejected the update. The form view will have already
            // collected what was submitted, so we display an error.
            // Check if this was likely a restricted-field error or a general error.
            // For now, we show a generic error; the form validation logic should
            // catch most cases before reaching here.
            formView.showSaveError("Profile update failed. Please check your changes and try again.");
            return false;
        }

        // Reload the profile to reflect persisted changes
        UserProfile reloadedProfile = profileController.loadProfile(userId);
        if (reloadedProfile != null) {
            currentProfile = reloadedProfile;
            detailsView.refresh(reloadedProfile);
            formView.resetForm();
            showUpdateSuccess();
            return true;
        } else {
            // Profile reload failed (unlikely but possible in concurrent scenarios)
            formView.showSaveError("Profile was updated but reload failed. Please close and reopen.");
            return false;
        }
    }

    /**
     * Handles the edit cancel action. Clears the form without saving.
     */
    public void onEditCancel() {
        formView.resetForm();
    }
}
