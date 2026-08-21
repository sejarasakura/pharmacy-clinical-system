package pharmacy_system.controller.common;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/** Converts servlet failures into safe, branded recovery states. */
@Controller
public class UiErrorController implements ErrorController {
    private final SessionController sessionController;
    private final NavigationController navigationController;

    public UiErrorController(SessionController sessionController, NavigationController navigationController) {
        this.sessionController = sessionController;
        this.navigationController = navigationController;
    }

    @RequestMapping("/error")
    public String error(HttpServletRequest request, Model model) {
        int status = statusCode(request);
        boolean authenticated = sessionController.isAuthenticated();
        model.addAttribute("title", titleFor(status));
        model.addAttribute("breadcrumb", "Error " + status);
        model.addAttribute("statusCode", status);
        model.addAttribute("statusTone", status >= 500 ? "danger" : status == 403 ? "warning" : "neutral");
        model.addAttribute("statusTitle", titleFor(status));
        model.addAttribute("statusMessage", messageFor(status));
        model.addAttribute("primaryHref", authenticated
                ? navigationController.homeFor(sessionController.getCurrentRole()) : "/login");
        model.addAttribute("primaryLabel", authenticated ? "Return home" : "Return to sign in");
        model.addAttribute("showBack", true);
        return "errors/error";
    }

    private int statusCode(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        return value instanceof Integer code ? code : 500;
    }

    private String titleFor(int status) {
        return switch (status) {
            case 400 -> "Request could not be completed";
            case 403 -> "Access denied";
            case 404 -> "Page not found";
            default -> "Something went wrong";
        };
    }

    private String messageFor(int status) {
        return switch (status) {
            case 400 -> "Check the submitted information and try again.";
            case 403 -> "Your account cannot access this function, or the request has expired.";
            case 404 -> "The requested page or record is unavailable.";
            default -> "We could not complete your request. No changes have been confirmed; please try again.";
        };
    }
}
