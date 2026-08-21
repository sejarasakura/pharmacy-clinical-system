package pharmacy_system.view.security_user.ucd06_manage_user_account;

import pharmacy_system.model.security_user.UserAccount;

import java.util.List;
import java.util.Objects;

/**
 * A component View that displays the list of user accounts for administrative
 * selection and management.
 *
 * <p>This View is responsible for:
 * <ul>
 *   <li>Displaying all user accounts in a list.</li>
 *   <li>Allowing the administrator to select an account for further action.</li>
 *   <li>Showing account status and key information.</li>
 * </ul>
 *
 * <p>The account list is read-only in this component; modifications are
 * handled by the parent View (ManageUserAccountView) and the controller.
 *
 * <p>Requirement 2.1 (Manage User Accounts): Display all user accounts
 * for administrative management.
 */
public class UserAccountListView {
    private List<UserAccount> displayedAccounts;
    private long selectedUserId;

    /**
     * Constructs a UserAccountListView.
     */
    public UserAccountListView() {
        this.displayedAccounts = List.of();
        this.selectedUserId = -1;
    }

    /**
     * Displays a list of user accounts for selection and viewing.
     *
     * @param accounts the list of UserAccounts to display; must not be null
     * @throws NullPointerException if accounts is null
     */
    public void displayAccounts(List<UserAccount> accounts) {
        this.displayedAccounts = Objects.requireNonNull(accounts, "accounts must not be null");
        this.selectedUserId = -1;
        // In a real implementation, this would populate a list UI component
        // with the account data (username, email, status, registration approval, etc.)
    }

    /**
     * Selects an account by user ID for further administrative actions.
     *
     * @param userId the user ID of the account to select
     */
    public void selectAccount(long userId) {
        // Verify the selected user exists in the displayed accounts
        boolean found = displayedAccounts.stream()
                .anyMatch(account -> account.getUserId() == userId);
        if (found) {
            this.selectedUserId = userId;
            // In a real implementation, this would update the UI to highlight the selection
        }
    }

    /**
     * Returns the user ID of the currently selected account.
     *
     * @return the user ID, or -1 if no account is selected
     */
    public long getSelectedUserId() {
        return selectedUserId;
    }

    /**
     * Returns the currently displayed accounts.
     *
     * @return the list of UserAccounts
     */
    public List<UserAccount> getDisplayedAccounts() {
        return displayedAccounts;
    }

    /**
     * Returns the currently selected account, or null if no selection.
     *
     * @return the selected UserAccount or null
     */
    public UserAccount getSelectedAccount() {
        if (selectedUserId <= 0) {
            return null;
        }
        return displayedAccounts.stream()
                .filter(account -> account.getUserId() == selectedUserId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Clears the current selection.
     */
    public void clearSelection() {
        this.selectedUserId = -1;
    }
}
