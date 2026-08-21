package pharmacy_system.view.common;

/** Presentation-only description of one role-permitted navigation destination. */
public record NavItem(String key, String label, String href, String iconName) {
    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getHref() {
        return href;
    }

    public String getIconName() {
        return iconName;
    }
}
