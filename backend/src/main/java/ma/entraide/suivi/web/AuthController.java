package ma.entraide.suivi.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import ma.entraide.suivi.config.AppProperties;
import ma.entraide.suivi.domain.AppUser;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.security.JwtAuthFilter;
import ma.entraide.suivi.security.JwtService;
import ma.entraide.suivi.service.AuthService;
import ma.entraide.suivi.web.Dtos.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final JwtService jwt;
    private final AppProperties props;

    public AuthController(AuthService auth, JwtService jwt, AppProperties props) {
        this.auth = auth; this.jwt = jwt; this.props = props;
    }

    @PostMapping("/login")
    public Me login(@Valid @RequestBody LoginRequest r, HttpServletResponse res) {
        AppUser u = auth.login(r.email(), r.password());
        setCookie(res, jwt.issue(u.getId()), jwt.ttl().toSeconds());
        return AuthService.toMe(u);
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse res) { setCookie(res, "", 0); }

    @GetMapping("/me")
    public Me me(@AuthenticationPrincipal AppUserPrincipal p) { return auth.me(p); }

    @PostMapping("/change-password")
    public Me changePassword(@AuthenticationPrincipal AppUserPrincipal p, @Valid @RequestBody ChangePasswordRequest r) {
        auth.changePassword(p, r.currentPassword(), r.newPassword());
        return auth.me(p);
    }

    private void setCookie(HttpServletResponse res, String value, long maxAgeSeconds) {
        ResponseCookie c = ResponseCookie.from(JwtAuthFilter.COOKIE, value).httpOnly(true).secure(props.cookieSecure())
                .sameSite("Strict").path("/").maxAge(maxAgeSeconds).build();
        res.addHeader(HttpHeaders.SET_COOKIE, c.toString());
    }
}
