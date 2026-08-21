package pharmacy_system.view.security_user.ucd05_manage_profile;

import pharmacy_system.model.security_user.profile.UserProfile;

import java.util.Objects;

/**
 * A component View that displays user profile details in read-only form.
 *
 * <p>This View is responsible for:
 * <ul>
 *   <li>Displaying the user's current profile information.</li>
 *   <li>Presenting fields in a read-only manner when displaying.</li>
 *   <li>Supporting refresh when the profile is updated.</li>
 * </ul>
 *
 * <p>The profile data is never mutated in this View; all editing is delegated
 * to ProfileFormView.
 *
 * <p>Requirement 2.3 (Manage Profile): Display permitted personal information.
 */
public class ProfileDetailsView {
    private UserProfile displayedProfile;

    /**
     * Constructs a ProfileDetailsView.
     */
    public ProfileDetailsView() {
        this.displayedProfile = null;
    }

    /**
     * Displays a profile for viewing. The profile data is presented in
     * read-only format.
     *
     * @param profile the UserProfile to display; must not be null
     * @throws NullPointerException if profile is null
     */
    public void display(UserProfile profile) {
        this.displayedProfile = Objects.requireNonNull(profile, "profile must not be null");
        // In a real implementation, this would populate read-only UI fields
        // with the profile's data (fullName, phoneNumber, contactEmail, address, etc.)
    }

    /**
     * Refreshes the displayed profile with updated data. Called after a
     * successful profile update to show the persisted changes.
     *
     * @param profile the updated UserProfile to display
     */
    public void refresh(UserProfile profile) {
        display(profile);
    }

    /**
     * Returns the currently displayed profile.
     *
     * @return the UserProfile or null if no profile is displayed
     */
    public UserProfile getDisplayedProfile() {
        return displayedProfile;
    }
}
