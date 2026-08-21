package pharmacy_system.controller.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Enforces the application session boundary and protected-response cache policy. */
@Component
public class SessionExpiryFilter extends OncePerRequestFilter {
    private final SessionController sessionController;

    public SessionExpiryFilter(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }
        if (!sessionController.isAuthenticated() || sessionController.isExpired()) {
            String target = URLEncoder.encode(path, StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/login?returnTo=" + target);
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        return path.equals("/login") || path.equals("/password/recovery")
                || path.equals("/password/reset") || path.equals("/access-denied")
                || path.startsWith("/static/") || path.startsWith("/error");
    }
}
