package pharmacy_system.model.security_user.profile;

import java.util.List;

/**
 * Thrown when a self-service profile change attempts to modify a field that
 * is not user-modifiable: Operational_Role, account status, privileged
 * permissions, or a system-owned identifier.
 *
 * <p>Per Requirement 2.4 (FR-009), the entire submitted change set is
 * rejected and none of the targeted fields are changed when this is
 * thrown.</p>
 */
public class RestrictedFieldException extends RuntimeException {

    private final List<String> restrictedFields;

    public RestrictedFieldException(List<String> restrictedFields) {
        super("Fields are not user-modifiable: " + restrictedFields);
        this.restrictedFields = List.copyOf(restrictedFields);
    }

    /**
     * @return the submitted field names that were rejected as restricted
     */
    public List<String> getRestrictedFields() {
        return restrictedFields;
    }
}
