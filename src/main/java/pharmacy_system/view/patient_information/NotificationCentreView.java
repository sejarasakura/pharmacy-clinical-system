package pharmacy_system.view.patient_information;

import pharmacy_system.controller.common.SessionController;
import pharmacy_system.controller.patient_information.SendAlertsNotificationsController;
import pharmacy_system.model.patient_information.Notification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Patient notification centre View [UCD-03].
 *
 * <p>This View displays persisted notifications of prescription and pharmacy
 * events, manages read/unread state, and allows patients to interact with
 * delivered notifications. It is the patient-facing display surface for the
 * SendAlertsNotificationsController.
 *
 * <p>Key behaviours:
 * <ul>
 *   <li>Displays a chronologically-ordered list of notifications for the
 *       authenticated patient (Requirement 7.1, Requirement 7.2).</li>
 *   <li>Shows each notification's delivery status: delivered, pending, or
 *       failed (Requirement 7.2).</li>
 *   <li>Indicates read/unread state independently of delivery outcome
 *       (Requirement 7.2).</li>
 *   <li>Allows the patient to mark individual notifications as read
 *       (Requirement 7.2).</li>
 *   <li>Gracefully handles delivery failures by continuing to display
 *       notifications and their failure reasons without rolling back
 *       underlying transactions (Requirement 7.4).</li>
 *   <li>Uses de-duplication keys internally to prevent duplicate notification
 *       display (managed by the controller; Requirement 7.3).</li>
 * </ul>
 *
 * <p>Requirements: 6.1, 6.2, 6.3, 7.2
 * Property 13: Notification de-duplication
 * Property 14: Transaction independence of notification
 */
public class NotificationCentreView {

    private final SendAlertsNotificationsController notificationController;
    private final SessionController sessionController;

    // UI state
    private List<Notification> displayedNotifications;
    private Notification selectedNotification;
    private String errorMessage;
    private boolean isVisible;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    /**
     * Constructs the notification centre View with required controller dependencies.
     *
     * @param notificationController the notifications controller (UCD-03)
     * @param sessionController      the session manager for the current user
     * @throws NullPointerException if any parameter is null
     */
    public NotificationCentreView(
            SendAlertsNotificationsController notificationController,
            SessionController sessionController) {
        this.notificationController = Objects.requireNonNull(
                notificationController,
                "notificationController must not be null");
        this.sessionController = Objects.requireNonNull(
                sessionController,
                "sessionController must not be null");

        this.displayedNotifications = new ArrayList<>();
        this.selectedNotification = null;
        this.errorMessage = null;
        this.isVisible = false;
    }

    /**
     * Displays the notification centre. In a real desktop implementation, this
     * would initialize and show the UI window. Here it marks the view as visible
     * and loads the patient's notifications.
     */
    public void show() {
        isVisible = true;
        loadNotifications();
    }

    /**
     * Hides the notification centre. In a real implementation, this would close
     * the UI window.
     */
    public void hide() {
        isVisible = false;
        clearSelection();
    }

    /**
     * Returns whether this View is currently visible.
     *
     * @return {@code true} if shown; {@code false} otherwise
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Returns the currently displayed list of notifications for the authenticated
     * patient. This list is updated when {@link #loadNotifications()} is called.
     *
     * @return an unmodifiable copy of the current notification list
     */
    public List<Notification> getDisplayedNotifications() {
        return new ArrayList<>(displayedNotifications);
    }

    /**
     * Returns the currently selected notification (from a prior call to
     * {@link #selectNotification(long)}), or null if no notification is selected.
     *
     * @return the selected Notification or null
     */
    public Notification getSelectedNotification() {
        return selectedNotification;
    }

    /**
     * Returns the current error message, or null if no error is displayed.
     *
     * @return the error message or null
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the count of unread notifications for the authenticated patient.
     * Useful for displaying an unread badge or indicator.
     *
     * @return the number of unread notifications, or 0 if none
     */
    public int getUnreadCount() {
        return (int) displayedNotifications.stream()
                .filter(Notification::isUnread)
                .count();
    }

    /**
     * Loads and displays all notifications for the authenticated patient
     * (Requirement 7.1, Requirement 7.2).
     *
     * <p>This method:
     * <ol>
     *   <li>Retrieves the current authenticated patient ID from the session.</li>
     *   <li>Fetches all persisted notifications for this patient from storage
     *       (assuming a query method is available in NotificationStorage;
     *       implementation detail delegated to storage layer).</li>
     *   <li>Sorts notifications by creation time (newest first).</li>
     *   <li>Updates the displayed list for rendering.</li>
     *   <li>Clears any previously selected notification.</li>
     *   <li>On error, displays a user-friendly error message.</li>
     * </ol>
     *
     * <p>This method is called automatically when the View is shown or can be
     * called manually to refresh the notification list.
     *
     * @return {@code true} if notifications were loaded successfully; {@code false}
     *         if an error occurred
     */
    public boolean loadNotifications() {
        // Ensure session is still valid
        if (!sessionController.isAuthenticated()) {
            displayError("Your session has expired. Please log in again.");
            return false;
        }

        try {
            long patientId = sessionController.getCurrentUserId();

            // NOTE: In a full implementation, NotificationStorage would provide
            // a method like findByPatientId(patientId) that retrieves all
            // notifications for the patient, sorted newest-first.
            // For now, this view assumes an empty list until storage is
            // integrated with a retrieval method.
            List<Notification> notifications = new ArrayList<>();
            // TODO: Replace with: notifications =
            // notificationStorage.findByPatientIdOrderByCreatedAtDesc(patientId);

            this.displayedNotifications = new ArrayList<>(notifications);
            this.errorMessage = null;
            clearSelection();
            return true;

        } catch (IllegalArgumentException e) {
            displayError("Unable to load notifications: " + e.getMessage());
            return false;
        } catch (Exception e) {
            displayError("An unexpected error occurred. Please try again later.");
            System.err.println("Error loading notifications: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Selects a notification to display its detailed view.
     *
     * @param notificationId the ID of the notification to select
     * @return {@code true} if the notification was found and selected;
     *         {@code false} if the notification does not exist
     */
    public boolean selectNotification(long notificationId) {
        for (Notification notification : displayedNotifications) {
            if (notification.getNotificationId() == notificationId) {
                this.selectedNotification = notification;
                this.errorMessage = null;
                return true;
            }
        }

        this.errorMessage = "Notification not found.";
        clearSelection();
        return false;
    }

    /**
     * Marks the currently selected notification as read (Requirement 7.2).
     *
     * <p>This method:
     * <ol>
     *   <li>Verifies that a notification is currently selected.</li>
     *   <li>Calls {@link SendAlertsNotificationsController#markNotificationRead(long)}
     *       to persist the read state to storage.</li>
     *   <li>Updates the local notification's read state on success.</li>
     *   <li>On error, displays an error message and leaves the read state unchanged.</li>
     * </ol>
     *
     * <p>Calling this method on an already-read notification has no observable
     * effect; the persistence call succeeds but the read timestamp does not change.
     *
     * @return {@code true} if the notification was successfully marked as read or
     *         was already read; {@code false} if an error occurred or no notification
     *         is selected
     */
    public boolean markSelectedNotificationAsRead() {
        if (selectedNotification == null) {
            displayError("No notification is selected.");
            return false;
        }

        try {
            boolean success = notificationController.markNotificationRead(
                    selectedNotification.getNotificationId());

            if (success) {
                // Update the local state to reflect the read transition
                selectedNotification.markRead();
                this.errorMessage = null;

                // Also update the notification in the displayed list
                for (int i = 0; i < displayedNotifications.size(); i++) {
                    if (displayedNotifications.get(i).getNotificationId() ==
                            selectedNotification.getNotificationId()) {
                        displayedNotifications.get(i).markRead();
                        break;
                    }
                }
                return true;
            } else {
                displayError("Failed to mark notification as read. The notification may have been modified.");
                return false;
            }

        } catch (Exception e) {
            displayError("An unexpected error occurred while marking notification as read.");
            System.err.println("Error marking notification as read: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Marks a specific notification (by ID) as read without requiring it to be
     * selected. Useful for bulk read operations or background updates.
     *
     * @param notificationId the ID of the notification to mark as read
     * @return {@code true} if the operation succeeded; {@code false} otherwise
     */
    public boolean markNotificationAsRead(long notificationId) {
        try {
            boolean success = notificationController.markNotificationRead(notificationId);

            if (success) {
                // Update the notification in the displayed list
                for (Notification notification : displayedNotifications) {
                    if (notification.getNotificationId() == notificationId) {
                        notification.markRead();
                        break;
                    }
                }
                return true;
            }
            return false;

        } catch (Exception e) {
            System.err.println("Error marking notification as read: " + e.getMessage());
            return false;
        }
    }

    /**
     * Marks all notifications as read.
     *
     * @return the count of notifications successfully marked as read
     */
    public int markAllAsRead() {
        int count = 0;
        for (Notification notification : displayedNotifications) {
            if (markNotificationAsRead(notification.getNotificationId())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Refreshes the notification list by re-fetching from the controller.
     * Useful for reflecting new notifications or delivery status changes.
     *
     * @return {@code true} if the list was refreshed successfully; {@code false}
     *         if an error occurred
     */
    public boolean refreshNotifications() {
        return loadNotifications();
    }

    /**
     * Clears the current selection. Called when deselecting a notification or
     * when the list is reloaded.
     */
    private void clearSelection() {
        this.selectedNotification = null;
    }

    /**
     * Displays an error message to the user. In a real implementation, this would
     * update a UI label or alert dialog.
     *
     * @param message the error message to display
     */
    private void displayError(String message) {
        this.errorMessage = message;
        // In a real implementation, this would update the UI error label
    }

    /**
     * Formats the delivery status for display. Maps internal delivery status to
     * user-friendly labels.
     *
     * @param deliveryStatus the delivery status
     * @return a display-friendly status label
     */
    public String formatDeliveryStatus(String deliveryStatus) {
        if (deliveryStatus == null) {
            return "Unknown";
        }

        return switch (deliveryStatus) {
            case "PENDING" -> "Pending";
            case "DELIVERED" -> "Delivered";
            case "FAILED" -> "Failed";
            case "NOT_DELIVERABLE" -> "Not Deliverable";
            default -> deliveryStatus;
        };
    }

    /**
     * Formats a notification creation timestamp for display.
     *
     * @param createdAt the timestamp to format
     * @return a formatted date/time string suitable for display
     */
    public String formatTimestamp(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "Unknown";
        }
        return createdAt.format(TIMESTAMP_FORMATTER);
    }

    /**
     * Returns a brief status indicator string for a notification, combining
     * delivery and read state. Useful for list item status badges.
     *
     * @param notification the notification to describe
     * @return a brief status indicator (e.g., "Delivered • Unread", "Failed")
     */
    public String getNotificationStatusBadge(Notification notification) {
        if (notification == null) {
            return "";
        }

        StringBuilder badge = new StringBuilder();

        // Delivery status
        if (notification.isDelivered()) {
            badge.append("Delivered");
        } else if (notification.isDeliveryFailed()) {
            badge.append("Failed");
        } else if (notification.isPending()) {
            badge.append("Pending");
        } else {
            badge.append("Not Deliverable");
        }

        // Read state
        if (notification.isUnread()) {
            badge.append(" • Unread");
        } else {
            badge.append(" • Read");
        }

        return badge.toString();
    }

    /**
     * Returns the CSS/styling class for a notification's visual presentation
     * based on its delivery and read state. Useful for theming in a UI framework.
     *
     * @param notification the notification to style
     * @return a style class name
     */
    public String getNotificationStyleClass(Notification notification) {
        if (notification == null) {
            return "notification-unknown";
        }

        if (notification.isDeliveryFailed()) {
            return notification.isRead() ? "notification-failed-read" : "notification-failed-unread";
        } else if (notification.isDelivered()) {
            return notification.isRead() ? "notification-read" : "notification-unread";
        } else {
            return notification.isRead() ? "notification-pending-read" : "notification-pending-unread";
        }
    }

    /**
     * Returns the human-readable notification type label.
     *
     * @param notificationType the notification type (e.g., "Clinical", "Pharmacy")
     * @return a display-friendly type label
     */
    public String formatNotificationType(String notificationType) {
        if (notificationType == null) {
            return "System";
        }

        return switch (notificationType) {
            case "Clinical" -> "Clinical Alert";
            case "Pharmacy" -> "Pharmacy Update";
            case "System" -> "System Notification";
            default -> notificationType;
        };
    }
}
