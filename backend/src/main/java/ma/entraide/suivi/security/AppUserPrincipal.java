package ma.entraide.suivi.security;

import ma.entraide.suivi.domain.AppUser;
import ma.entraide.suivi.domain.Role;

public record AppUserPrincipal(Long id, String email, Role role, Long provinceId, Long regionId, boolean mustChangePassword) {
    public static AppUserPrincipal of(AppUser u) {
        return new AppUserPrincipal(u.getId(), u.getEmail(), u.getRole(),
                u.getProvince() == null ? null : u.getProvince().getId(),
                u.getRegion() == null ? null : u.getRegion().getId(), u.isMustChangePassword());
    }
}
