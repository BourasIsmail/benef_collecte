package ma.entraide.suivi.web;

import ma.entraide.suivi.domain.AuditLog;
import ma.entraide.suivi.repo.AuditLogRepository;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.service.StatsService;
import ma.entraide.suivi.service.UserService;
import ma.entraide.suivi.web.Dtos.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public class MiscControllers {
    private MiscControllers() {}

    @RestController @RequestMapping("/api/stats")
    public static class StatsController {
        private final StatsService stats;
        public StatsController(StatsService stats) { this.stats = stats; }
        @GetMapping public Stats get(@AuthenticationPrincipal AppUserPrincipal me) { return stats.compute(me); }
    }

    @RestController @RequestMapping("/api/users") @PreAuthorize("hasRole('ADMIN')")
    public static class UserController {
        private final UserService users;
        public UserController(UserService users) { this.users = users; }
        @GetMapping public List<UserDto> list() { return users.list(); }
        @PostMapping("/{id}/reset-password")
        public void reset(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) { users.resetPassword(me, id); }
        @PutMapping("/{id}/enabled")
        public void enabled(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id, @RequestBody EnabledRequest r) { users.setEnabled(me, id, r.enabled()); }
    }

    @RestController @RequestMapping("/api/audit") @PreAuthorize("hasRole('ADMIN')")
    public static class AuditController {
        private final AuditLogRepository repo;
        public AuditController(AuditLogRepository repo) { this.repo = repo; }
        @GetMapping
        public PageDto<AuditDto> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size,
                                      @RequestParam(required = false) String action, @RequestParam(required = false) String user,
                                      @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                      @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
            var f = (from == null ? java.time.LocalDate.of(2000, 1, 1) : from).atStartOfDay();
            var t = (to == null ? java.time.LocalDate.of(2100, 1, 1) : to.plusDays(1)).atStartOfDay();
            var p = repo.search(action == null || action.isBlank() ? null : action, user == null || user.isBlank() ? null : user.trim(), f, t,
                    PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200)));
            return new PageDto<>(p.getContent().stream().map(MiscControllers::toDto).toList(), p.getTotalElements(), p.getNumber(), p.getSize());
        }

        @GetMapping("/actions")
        public List<String> actions() { return repo.actions(); }
    }

    static AuditDto toDto(AuditLog l) {
        return new AuditDto(l.getId(), l.getAt(), l.getUserEmail(), l.getAction(), l.getEntity(), l.getEntityId(), l.getDetails());
    }
}
