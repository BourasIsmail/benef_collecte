package ma.entraide.suivi.service;

import ma.entraide.suivi.domain.Role;
import ma.entraide.suivi.repo.ProvinceRepository;
import ma.entraide.suivi.security.AppUserPrincipal;
import org.springframework.stereotype.Component;

import java.util.List;

/** Resolves which provinces a user may see. ADMIN = everything. */
@Component
public class Scope {
    private final ProvinceRepository provinces;

    public Scope(ProvinceRepository provinces) { this.provinces = provinces; }

    public record View(boolean all, List<Long> ids) {
        public boolean contains(Long provinceId) { return all || ids.contains(provinceId); }
    }

    public View of(AppUserPrincipal p) {
        return switch (p.role()) {
            case ADMIN -> new View(true, List.of(-1L));
            case PROVINCE -> new View(false, List.of(p.provinceId()));
            case REGIONAL -> {
                List<Long> ids = provinces.findByRegionId(p.regionId()).stream().map(x -> x.getId()).toList();
                yield new View(false, ids.isEmpty() ? List.of(-1L) : ids);
            }
        };
    }

    public static boolean canWrite(AppUserPrincipal p) { return p.role() != Role.REGIONAL; }
}
