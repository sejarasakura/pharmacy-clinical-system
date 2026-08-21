package pharmacy_system.view.common;

/** One-time presentation message used by Post/Redirect/Get flows. */
public record Flash(String type, String message) {
    public Flash {
        if (!"success".equals(type) && !"warning".equals(type) && !"danger".equals(type)) {
            throw new IllegalArgumentException("Unsupported flash type: " + type);
        }
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
