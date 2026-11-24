package ma.entraide.suivi.service;

import ma.entraide.suivi.config.AppProperties;
import ma.entraide.suivi.domain.AppUser;
import ma.entraide.suivi.domain.Role;
import ma.entraide.suivi.repo.UserRepository;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.web.ApiException;
import ma.entraide.suivi.web.Dtos.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties props;
    private final AuditService audit;

    public UserService(UserRepository users, PasswordEncoder encoder, AppProperties props, AuditService audit) {
        this.users = users; this.encoder = encoder; this.props = props; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return users.findAllWithScope().stream().map(u -> new UserDto(u.getId(), u.getEmail(), u.getRole(),
                u.getProvince() == null ? null : u.getProvince().getNomFr(), u.getRegion() == null ? null : u.getRegion().getNomFr(),
                u.isEnabled(), u.isMustChangePassword(), u.getLastLogin())).toList();
    }

    /** Back to the default password; the user must choose a new one at next login. */
    @Transactional
    public void resetPassword(AppUserPrincipal admin, Long id) {
        AppUser u = users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
        if (u.getRole() == Role.ADMIN) throw new ApiException(HttpStatus.FORBIDDEN, "CANNOT_RESET_ADMIN");
        u.setPasswordHash(encoder.encode(props.defaultPassword()));
        u.setMustChangePassword(true);
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        audit.log(admin.email(), "USER_PASSWORD_RESET", "User", id, u.getEmail());
    }

    @Transactional
    public void setEnabled(AppUserPrincipal admin, Long id, boolean enabled) {
        AppUser u = users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
        if (u.getRole() == Role.ADMIN) throw new ApiException(HttpStatus.FORBIDDEN, "CANNOT_DISABLE_ADMIN");
        u.setEnabled(enabled);
        audit.log(admin.email(), enabled ? "USER_ENABLED" : "USER_DISABLED", "User", id, u.getEmail());
    }
}
