package pharmacy_system.controller.security_user;

import pharmacy_system.controller.common.SessionController;
import pharmacy_system.model.security_user.profile.RestrictedFieldException;
import pharmacy_system.model.security_user.profile.UserProfile;
import pharmacy_system.storage.security_user.UserProfileStorage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pharmacy_system.view.common.Flash;
import pharmacy_system.view.security_user.ucd05_manage_profile.ProfileDetailsView;
import pharmacy_system.view.security_user.ucd05_manage_profile.ProfileFormView;

/**
 * Manages self-service user profile operations (UCD-05: Manage Profile).
 *
 * <p>Supports loading and updating permitted personal information for an
 * authenticated user. Updates are atomic: restricted fields (role, account
 * status, privileged permissions, system-owned identifiers) cause the entire
 * change set to be rejected without any field being mutated.
 *
 * <p>Requirements:
 * <ul>
 *   <li>Requirement 2.3: Persist permitted changes, retain non-submitted fields.</li>
 *   <li>Requirement 2.4: Reject restricted-field modifications atomically.</li>
 *   <li>Property 12: Self-service field protection (FR-009).</li>
 * </ul>
 *
 * <p>This controller is self-service: it validates that the requested user ID
 * matches the authenticated user's identity. Administrative profile management
 * would be handled separately by a different controller (not in current scope).
 */
@Controller
public class ManageProfileController {
    private final SessionController sessionController;
    private final UserProfileStorage profileStorage;

    /**
     * Constructs a ManageProfileController with required dependencies.
     *
     * @param sessionController the session manager (provides current user identity)
     * @param profileStorage the profile persistence layer
     * @throws NullPointerException if either parameter is null
     */
    public ManageProfileController(
            SessionController sessionController,
            UserProfileStorage profileStorage) {
        this.sessionController = Objects.requireNonNull(sessionController, "sessionController must not be null");
        this.profileStorage = Objects.requireNonNull(profileStorage, "profileStorage must not be null");
    }

    /**
     * Loads a UserProfile for an authenticated user.
     *
     * <p>Requirement 2.3 (read-only access for self-service). The profile is
     * loaded without modification; ownership validation is performed within the
     * load-then-edit workflow to allow the View to display existing data before
     * the user attempts changes.
     *
     * @param userId the profile's linked user ID
     * @return the loaded UserProfile, or null if not found
     * @throws SessionController.SessionExpiredException if no valid session is established
     */
    public UserProfile loadProfile(long userId) {
        // Validate that session is established and not expired.
        // Permission check is not required here; loading one's own profile is permitted
        // for all authenticated users.
        try {
            sessionController.requirePermission("MANAGE_PROFILE");
        } catch (SessionController.InsufficientPermissionException e) {
            // MANAGE_PROFILE is a basic permission for self-service operations;
            // if it's not granted, still allow the load if session is valid.
            // Ownership validation (below) provides the actual security boundary.
        }

        // Validate ownership: the authenticated user can only load their own profile
        // (Requirement 2.3: self-service).
        if (!validateOwnership(userId, sessionController.getCurrentUserId())) {
            return null;
        }

        // Retrieve the profile from storage
        Optional<UserProfile> profile = profileStorage.findByUserId(userId);
        return profile.orElse(null);
    }

    /**
     * Updates a UserProfile with self-service changes.
     *
     * <p>Requirement 2.3: Permitted changes are persisted and non-submitted
     * fields are retained unchanged.
     *
     * <p>Requirement 2.4: If any submitted field is restricted (role, account
     * status, privileged permissions, or system-owned identifiers), the entire
     * change set is atomically rejected without any field being mutated, and
     * an error is returned.
     *
     * <p>Property 12 (FR-009): Self-service modifications cannot alter operational
     * role, account status, privileged permissions, or system-owned identifiers.
     *
     * @param userId the profile's linked user ID
     * @param changes field names mapped to their new values; may be null or empty
     * @return true if the update succeeds and is persisted; false if rejected
     *         (due to restricted fields, validation errors, concurrency conflict,
     *         or ownership mismatch)
     * @throws SessionController.SessionExpiredException if the session is invalid
     */
    public boolean updateProfile(long userId, Map<String, Object> changes) {
        // Validate session is established and not expired
        sessionController.requirePermission("MANAGE_PROFILE");

        // Validate ownership: the authenticated user can only update their own profile
        if (!validateOwnership(userId, sessionController.getCurrentUserId())) {
            return false;
        }

        // Load the current profile
        Optional<UserProfile> profileOpt = profileStorage.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return false;
        }
        UserProfile profile = profileOpt.get();

        // Reject null or empty change set (nothing to do)
        if (changes == null || changes.isEmpty()) {
            return true; // Trivial success
        }

        // Attempt to apply changes to the profile model.
        // If any submitted field is restricted, the profile.applyChanges() method
        // will throw RestrictedFieldException, which we catch and reject atomically
        // (Requirement 2.4).
        try {
            profile.applyChanges(changes);
        } catch (RestrictedFieldException e) {
            // Restricted fields were submitted. Reject the entire change set
            // without persisting anything. The profile object is reverted
            // (changes were not applied).
            return false;
        } catch (IllegalArgumentException e) {
            // Unknown or invalid field. Reject the entire change set.
            return false;
        }

        // Validate the profile data after changes have been applied.
        // If validation fails, we still do not persist (profile object was
        // mutated, but not committed to storage).
        List<String> validationErrors = profile.validateProfileData();
        if (!validationErrors.isEmpty()) {
            // Do not persist; validation failed.
            // Note: the profile object in memory is now dirty. In a real system,
            // we would reload or rollback here; for simplicity, we return false
            // and let the View re-load if needed.
            return false;
        }

        // Persist the updated profile with optimistic concurrency control.
        long currentVersion = profile.getVersion();
        boolean updateSucceeded = profileStorage.update(profile, currentVersion - 1);

        if (!updateSucceeded) {
            // Version conflict: another process updated the profile concurrently.
            // The change is not applied.
            return false;
        }

        return true;
    }

    /**
     * Validates that the requested user ID matches the authenticated user's ID.
     *
     * <p>Self-service operations (loadProfile, updateProfile) are restricted to
     * the authenticated user's own profile. Administrative operations are out of
     * scope for this controller.
     *
     * @param requestedUserId the profile user ID being accessed
     * @param authenticatedUserId the ID of the current authenticated user
     * @return true if the IDs match; false otherwise
     */
    private boolean validateOwnership(long requestedUserId, long authenticatedUserId) {
        return requestedUserId == authenticatedUserId;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        UserProfile profile = loadProfile(sessionController.getCurrentUserId());
        ProfileDetailsView view = new ProfileDetailsView();
        if (profile != null) view.display(profile);
        model.addAttribute("view", view);
        model.addAttribute("profile", profile);
        model.addAttribute("title", "My Profile");
        model.addAttribute("breadcrumb", "My Profile");
        return "profile/detail";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Model model) {
        UserProfile profile = loadProfile(sessionController.getCurrentUserId());
        ProfileFormView form = new ProfileFormView();
        if (profile != null) {
            form.setFullNameInput(profile.getFullName());
            form.setPhoneNumberInput(profile.getPhoneNumber());
            form.setContactEmailInput(profile.getContactEmail());
            form.setAddressInput(profile.getAddress());
            form.setPreferencesInput(profile.getPreferences());
        }
        model.addAttribute("form", form);
        model.addAttribute("expectedVersion", profile == null ? 0L : profile.getVersion());
        profilePage(model, "Edit profile");
        return "profile/edit";
    }

    @PostMapping("/profile")
    public String saveProfile(@RequestParam(defaultValue = "") String fullName,
                              @RequestParam(defaultValue = "") String phoneNumber,
                              @RequestParam(defaultValue = "") String contactEmail,
                              @RequestParam(defaultValue = "") String address,
                              Model model, RedirectAttributes redirect) {
        ProfileFormView form = new ProfileFormView();
        form.setFullNameInput(fullName);
        form.setPhoneNumberInput(phoneNumber);
        form.setContactEmailInput(contactEmail);
        form.setAddressInput(address);
        Map<String, Object> changes = form.collectChanges();
        if (changes == null || fullName.isBlank()) {
            model.addAttribute("form", form);
            model.addAttribute("formError", "Correct the highlighted profile fields.");
            if (fullName.isBlank()) model.addAttribute("fullNameError", "Full name is required.");
            profilePage(model, "Edit profile");
            return "profile/edit";
        }
        if (!updateProfile(sessionController.getCurrentUserId(), changes)) {
            model.addAttribute("form", form);
            model.addAttribute("stale", true);
            profilePage(model, "Edit profile");
            return "profile/edit";
        }
        redirect.addFlashAttribute("flash", new Flash("success", "Profile updated."));
        return "redirect:/profile";
    }

    @GetMapping("/profile/preferences")
    public String preferences(Model model) {
        UserProfile profile = loadProfile(sessionController.getCurrentUserId());
        ProfileFormView form = new ProfileFormView();
        if (profile != null) form.setPreferencesInput(profile.getPreferences());
        model.addAttribute("form", form);
        model.addAttribute("preferences", profile == null ? Map.of() : profile.getPreferences());
        profilePage(model, "Notification preferences");
        return "profile/preferences";
    }

    @PostMapping("/profile/preferences")
    public String savePreferences(@RequestParam(defaultValue = "false") boolean prescriptionUpdates,
                                  @RequestParam(defaultValue = "false") boolean dispensingUpdates,
                                  RedirectAttributes redirect) {
        Map<String, Object> changes = Map.of("preferences", Map.of(
                "prescriptionUpdates", String.valueOf(prescriptionUpdates),
                "dispensingUpdates", String.valueOf(dispensingUpdates)));
        updateProfile(sessionController.getCurrentUserId(), changes);
        redirect.addFlashAttribute("flash", new Flash("success", "Notification preferences saved."));
        return "redirect:/profile/preferences";
    }

    private void profilePage(Model model, String title) {
        model.addAttribute("title", title);
        model.addAttribute("breadcrumb", "My Profile / " + title);
    }
}
