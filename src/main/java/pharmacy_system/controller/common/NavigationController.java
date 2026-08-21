package pharmacy_system.controller.common;

import pharmacy_system.view.common.NavItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Coordinates navigation between views, validates role-based access, and maintains
 * navigation history. Requires integration with SessionController for session
 * validation on protected operations (FR-003, FR-004, FR-006).
 *
 * <p>The NavigationController is the single point of navigation routing for the
 * authenticated shell. It ensures role-based access control at the navigation level
 * before allowing view transitions.</p>
 */
public class NavigationController {
    private final SessionController sessionController;
    private final Map<String, NavigationTarget> registeredTargets;
    private final List<String> navigationHistory;
    private String currentViewName;

    /**
     * Creates a navigation controller with the supplied session context.
     *
     * @param sessionController the session controller for permission validation
     * @throws NullPointerException if sessionController is null
     */
    public NavigationController(SessionController sessionController) {
        this.sessionController = Objects.requireNonNull(sessionController, "sessionController must not be null");
        this.registeredTargets = new HashMap<>();
        this.navigationHistory = new ArrayList<>();
        this.currentViewName = null;
        initializeDefaultTargets();
    }

    /**
     * Registers a navigation target with the specified required permissions.
     * If a target is re-registered, its permissions are updated.
     *
     * @param viewName the unique name of the view
     * @param requiredPermissions permissions required to access this view;
     *        empty set means accessible to all authenticated users
     * @throws IllegalArgumentException if viewName is blank or null
     * @throws NullPointerException if requiredPermissions is null
     */
    public void registerNavigationTarget(String viewName, java.util.Set<String> requiredPermissions) {
        if (viewName == null || viewName.isBlank()) {
            throw new IllegalArgumentException("viewName must not be blank");
        }
        Objects.requireNonNull(requiredPermissions, "requiredPermissions must not be null");
        registeredTargets.put(viewName, new NavigationTarget(viewName, requiredPermissions));
    }

    /**
     * Navigates to the specified view if the user has permission and is
     * authenticated. Adds the target view to the navigation history if successful.
     *
     * <p>This method validates the session and permissions before allowing
     * the transition. If the session is expired or the user lacks permission,
     * the navigation is denied and the current view remains active.</p>
     *
     * @param viewName the name of the target view
     * @return {@code true} if navigation succeeded; {@code false} if denied
     *         due to insufficient permission or expired session
     * @throws IllegalArgumentException if viewName is not registered
     */
    public boolean navigateTo(String viewName) {
        if (viewName == null || viewName.isBlank()) {
            return false;
        }
        if (!registeredTargets.containsKey(viewName)) {
            throw new IllegalArgumentException("View '" + viewName + "' is not registered");
        }

        // Validate session state
        if (!sessionController.isAuthenticated() || sessionController.isExpired()) {
            return false;
        }

        NavigationTarget target = registeredTargets.get(viewName);

        // Validate required permissions (all must be held)
        for (String permission : target.getRequiredPermissions()) {
            if (!sessionController.hasPermission(permission)) {
                return false;
            }
        }

        // Navigation successful: update current view and history
        if (!viewName.equals(currentViewName)) {
            navigationHistory.add(viewName);
        }
        currentViewName = viewName;
        return true;
    }

    /**
     * Attempts to navigate to the authenticated workspace home view based on
     * the user's role. The role is determined from the session context.
     *
     * <p>If the session is invalid or the user's role is not recognized,
     * navigation to login is initiated instead.</p>
     *
     * @return {@code true} if navigation to an authorised home succeeded;
     *         {@code false} otherwise
     */
    public boolean navigateToAuthorisedHome() {
        if (!sessionController.isAuthenticated() || sessionController.isExpired()) {
            navigateToLogin();
            return false;
        }

        // Determine role-specific home based on available permissions
        String homeView = determineHomeViewByRole();
        if (homeView != null) {
            return navigateTo(homeView);
        }

        // No role-specific home found; default to generic authenticated workspace
        String defaultHome = "AUTHENTICATED_HOME";
        if (registeredTargets.containsKey(defaultHome)) {
            return navigateTo(defaultHome);
        }

        // Fallback: no suitable home available
        navigateToLogin();
        return false;
    }

    /**
     * Navigates to the login view, clearing the current view and navigation
     * history. Typically called on logout or session expiry.
     *
     * @return {@code true} always, unless the login view is not registered
     */
    public boolean navigateToLogin() {
        currentViewName = null;
        navigationHistory.clear();
        if (registeredTargets.containsKey("LOGIN")) {
            currentViewName = "LOGIN";
            return true;
        }
        return false;
    }

    /**
     * Navigates to an access-denied view when the user lacks permission for
     * the requested operation.
     *
     * @return {@code true} if the access-denied view is registered and navigation
     *         succeeds; {@code false} otherwise
     */
    public boolean navigateToAccessDenied() {
        if (registeredTargets.containsKey("ACCESS_DENIED")) {
            currentViewName = "ACCESS_DENIED";
            navigationHistory.add("ACCESS_DENIED");
            return true;
        }
        return false;
    }

    /**
     * Navigates back to the previous view in the navigation history if one exists.
     *
     * <p>This re-validates permissions at navigation time to ensure the user
     * still has access to the previous view.</p>
     *
     * @return {@code true} if a previous view was available and navigation
     *         succeeded; {@code false} if there is no previous view
     */
    public boolean navigateBack() {
        if (navigationHistory.isEmpty()) {
            return false;
        }

        // Remove the current view from history
        if (!navigationHistory.isEmpty() && currentViewName != null
                && navigationHistory.get(navigationHistory.size() - 1).equals(currentViewName)) {
            navigationHistory.remove(navigationHistory.size() - 1);
        }

        // Navigate to the previous view if available
        if (!navigationHistory.isEmpty()) {
            String previousView = navigationHistory.get(navigationHistory.size() - 1);
            if (navigateTo(previousView)) {
                return true;
            }
        }

        // No valid previous view; return to home
        return navigateToAuthorisedHome();
    }

    /**
     * Returns the name of the currently active view, or {@code null} if no
     * view is currently displayed.
     *
     * @return the current view name, or null if unauthenticated
     */
    public String getCurrentViewName() {
        return currentViewName;
    }

    /**
     * Returns an unmodifiable copy of the navigation history, with the most
     * recent view last.
     *
     * @return the current navigation history
     */
    public List<String> getNavigationHistory() {
        return Collections.unmodifiableList(new ArrayList<>(navigationHistory));
    }

    /**
     * Clears the navigation history (typically on logout or session reset).
     */
    public void clearNavigationHistory() {
        navigationHistory.clear();
    }

    /** Returns the exact ordered navigation model permitted for one operational role. */
    public List<NavItem> navItemsFor(String role) {
        if (role == null) {
            return List.of();
        }
        return switch (role.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "DOCTOR" -> List.of(
                    new NavItem("prescriptions", "Prescriptions", "/doctor/prescriptions", "Rx"),
                    new NavItem("profile", "My Profile", "/profile", "Me"));
            case "PATIENT" -> List.of(
                    new NavItem("prescriptions", "My Prescriptions", "/patient/prescriptions", "Rx"),
                    new NavItem("notifications", "Notifications", "/patient/notifications", "!"),
                    new NavItem("profile", "My Profile", "/profile", "Me"));
            case "PHARMACIST", "PHARMACY" -> List.of(
                    new NavItem("dispensing", "Dispensing", "/pharmacy/dispensing", "Rx"),
                    new NavItem("inventory", "Inventory", "/pharmacy/inventory", "#"),
                    new NavItem("profile", "My Profile", "/profile", "Me"));
            case "ADMINISTRATOR", "ADMIN" -> List.of(
                    new NavItem("users", "User Accounts", "/admin/users", "@"),
                    new NavItem("reports", "Reports", "/admin/reports", "%"),
                    new NavItem("profile", "My Profile", "/profile", "Me"));
            default -> List.of();
        };
    }

    public String homeFor(String role) {
        List<NavItem> items = navItemsFor(role);
        return items.isEmpty() ? "/login" : items.get(0).href();
    }

    // ===== Private helpers =====

    /**
     * Initializes default navigation targets (login, access denied, etc.).
     * These are always available and require no special permissions.
     */
    private void initializeDefaultTargets() {
        registerNavigationTarget("LOGIN", java.util.Set.of());
        registerNavigationTarget("ACCESS_DENIED", java.util.Set.of());
        // Additional default targets can be added here as needed
    }

    /**
     * Determines the appropriate home view based on the user's role/permissions.
     * Returns a role-specific home view name if available.
     *
     * @return a home view name suitable for the user's role, or null if none
     *         can be determined
     */
    private String determineHomeViewByRole() {
        // Determine home based on role-specific permission patterns
        // This is a heuristic; actual role mapping would depend on permission structure

        // Check for role-specific permissions in order of specificity
        if (sessionController.hasPermission("GENERATE_REPORTS")) {
            return "ADMIN_HOME"; // Administrator or reporting-focused user
        }
        if (sessionController.hasPermission("DISPENSE_MEDICATION")) {
            return "PHARMACIST_HOME"; // Pharmacist
        }
        if (sessionController.hasPermission("PRESCRIPTION_STATUS_UPDATE")) {
            return "DOCTOR_HOME"; // Doctor
        }
        if (sessionController.hasPermission("VIEW_PRESCRIPTION_STATUS")) {
            return "PATIENT_HOME"; // Patient
        }

        // No specific role detected; return null to use default
        return null;
    }

    /**
     * Internal representation of a navigation target with its required
     * permissions.
     */
    private static final class NavigationTarget {
        private final String viewName;
        private final java.util.Set<String> requiredPermissions;

        NavigationTarget(String viewName, java.util.Set<String> requiredPermissions) {
            this.viewName = viewName;
            this.requiredPermissions = Collections.unmodifiableSet(
                    new java.util.LinkedHashSet<>(requiredPermissions));
        }

        String getViewName() {
            return viewName;
        }

        java.util.Set<String> getRequiredPermissions() {
            return requiredPermissions;
        }
    }
}
