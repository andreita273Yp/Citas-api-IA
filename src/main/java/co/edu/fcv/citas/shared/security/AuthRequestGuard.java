package co.edu.fcv.citas.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protege login/refresh/logout (que usan la cookie refresh) frente a peticiones cross-site: si llega un
 * {@code Origin}, debe ser el permitido y venir con {@code X-Requested-With: XMLHttpRequest}.
 */
@Component
class AuthRequestGuard extends OncePerRequestFilter {
    private static final Set<String> PROTECTED_PATHS = Set.of("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout");
    private final String allowedOrigin;

    AuthRequestGuard(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equals(request.getMethod()) && PROTECTED_PATHS.contains(request.getRequestURI())) {
            String origin = request.getHeader(HttpHeaders.ORIGIN);
            if (origin != null && (!allowedOrigin.equals(origin) || !"XMLHttpRequest".equals(request.getHeader("X-Requested-With")))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
