package pharmacy_system.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

/** Cross-cutting model attributes and safe presentation of access failures. */
@ControllerAdvice
public class UiModelAdvice {
    private final SessionController sessionController;
    private final NavigationController navigationController;

    public UiModelAdvice(SessionController sessionController, NavigationController navigationController) {
        this.sessionController = sessionController;
        this.navigationController = navigationController;
    }

    @ModelAttribute
    public void shellAttributes(Model model, HttpServletRequest request) {
        String role = sessionController.getCurrentRole();
        String username = sessionController.getCurrentUsername();
        model.addAttribute("navItems", navigationController.navItemsFor(role));
        model.addAttribute("currentUser", username);
        model.addAttribute("currentUserInitial", username == null || username.isBlank()
                ? "?" : username.substring(0, 1).toUpperCase(java.util.Locale.ROOT));
        model.addAttribute("currentRole", role);
        model.addAttribute("authenticated", sessionController.isAuthenticated());
        model.addAttribute("homeHref", navigationController.homeFor(role));
        model.addAttribute("activeNavKey", activeKey(request.getRequestURI()));
    }

    @ExceptionHandler(SessionController.InsufficientPermissionException.class)
    public ModelAndView accessDenied() {
        ModelAndView view = new ModelAndView("auth/access-denied");
        view.addObject("title", "Access denied");
        view.addObject("breadcrumb", "Access denied");
        view.addObject("homeHref", navigationController.homeFor(sessionController.getCurrentRole()));
        return view;
    }

    @ExceptionHandler(SessionController.SessionExpiredException.class)
    public ModelAndView sessionExpired(HttpServletRequest request) {
        String target = java.net.URLEncoder.encode(request.getRequestURI(), java.nio.charset.StandardCharsets.UTF_8);
        return new ModelAndView("redirect:/login?returnTo=" + target);
    }

    private String activeKey(String path) {
        if (path.startsWith("/doctor/prescriptions") || path.startsWith("/patient/prescriptions")) return "prescriptions";
        if (path.startsWith("/patient/notifications")) return "notifications";
        if (path.startsWith("/pharmacy/dispensing")) return "dispensing";
        if (path.startsWith("/pharmacy/inventory")) return "inventory";
        if (path.startsWith("/admin/users")) return "users";
        if (path.startsWith("/admin/reports")) return "reports";
        if (path.startsWith("/profile")) return "profile";
        return "";
    }
}
