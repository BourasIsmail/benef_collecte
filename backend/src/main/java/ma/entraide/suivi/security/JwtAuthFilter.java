package ma.entraide.suivi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.entraide.suivi.domain.AppUser;
import ma.entraide.suivi.repo.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/** Reads the JWT from the HttpOnly cookie, reloads the user (so disabling/reset applies immediately). */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    public static final String COOKIE = "entraide_token";
    private static final Set<String> ALLOWED_WHEN_MUST_CHANGE = Set.of("/api/auth/me", "/api/auth/change-password", "/api/auth/logout");

    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) { this.jwt = jwt; this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String token = null;
        if (req.getCookies() != null) for (Cookie c : req.getCookies()) if (COOKIE.equals(c.getName())) token = c.getValue();
        if (token != null) {
            jwt.parse(token).flatMap(users::findWithScopeById).filter(AppUser::isEnabled).ifPresent(u -> {
                AppUserPrincipal p = AppUserPrincipal.of(u);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        p, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))));
            });
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal p && p.mustChangePassword()
                && !ALLOWED_WHEN_MUST_CHANGE.contains(req.getRequestURI())) {
            res.setStatus(403);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"code\":\"PASSWORD_CHANGE_REQUIRED\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
