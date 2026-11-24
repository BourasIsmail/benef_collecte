package ma.entraide.suivi.service;

import ma.entraide.suivi.config.AppProperties;
import ma.entraide.suivi.domain.AppUser;
import ma.entraide.suivi.repo.UserRepository;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.web.ApiException;
import ma.entraide.suivi.web.Dtos.Me;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final AppProperties props;
    private final String dummyHash; // constant-time-ish answer for unknown e-mails

    public AuthService(UserRepository users, PasswordEncoder encoder, AuditService audit, AppProperties props) {
        this.users = users; this.encoder = encoder; this.audit = audit; this.props = props;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    /** Same error for unknown user, wrong password, disabled or locked account. */
    @Transactional(noRollbackFor = ApiException.class)
    public AppUser login(String email, String password) {
        AppUser u = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (u == null) { encoder.matches(password, dummyHash); throw invalid(); }
        LocalDateTime now = LocalDateTime.now();
        if (!u.isEnabled() || (u.getLockedUntil() != null && u.getLockedUntil().isAfter(now))) {
            audit.log(u.getEmail(), "LOGIN_REFUSED", "User", u.getId(), u.isEnabled() ? "locked" : "disabled");
            throw invalid();
        }
        if (!encoder.matches(password, u.getPasswordHash())) {
            u.setFailedAttempts(u.getFailedAttempts() + 1);
            if (u.getFailedAttempts() >= MAX_ATTEMPTS) { u.setLockedUntil(now.plusMinutes(LOCK_MINUTES)); u.setFailedAttempts(0); }
            audit.log(u.getEmail(), "LOGIN_FAILED", "User", u.getId(), null);
            throw invalid();
        }
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        u.setLastLogin(now);
        audit.log(u.getEmail(), "LOGIN", "User", u.getId(), null);
        return users.findWithScopeById(u.getId()).orElseThrow();
    }

    @Transactional
    public void changePassword(AppUserPrincipal me, String current, String next) {
        AppUser u = users.findById(me.id()).orElseThrow();
        if (!encoder.matches(current, u.getPasswordHash())) throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_CURRENT_PASSWORD");
        if (!PasswordPolicy.isStrong(next)) throw new ApiException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD");
        if (encoder.matches(next, u.getPasswordHash()) || next.equals(props.defaultPassword()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_REUSED");
        u.setPasswordHash(encoder.encode(next));
        u.setMustChangePassword(false);
        audit.log(u.getEmail(), "PASSWORD_CHANGED", "User", u.getId(), null);
    }

    @Transactional(readOnly = true)
    public Me me(AppUserPrincipal p) {
        AppUser u = users.findWithScopeById(p.id()).orElseThrow();
        return toMe(u);
    }

    public static Me toMe(AppUser u) {
        var pr = u.getProvince();
        var rg = u.getRegion() != null ? u.getRegion() : (pr != null ? pr.getRegion() : null);
        return new Me(u.getId(), u.getEmail(), u.getRole(), u.isMustChangePassword(),
                pr == null ? null : pr.getId(), pr == null ? null : pr.getNomFr(), pr == null ? null : pr.getNomAr(),
                rg == null ? null : rg.getId(), rg == null ? null : rg.getNomFr(), rg == null ? null : rg.getNomAr());
    }

    private static ApiException invalid() { return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"); }
}
