package ma.entraide.suivi.web;

import ma.entraide.suivi.repo.CategoryRepository;
import ma.entraide.suivi.repo.ProvinceRepository;
import ma.entraide.suivi.repo.RegionRepository;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.service.Scope;
import ma.entraide.suivi.web.Dtos.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ref")
public class RefController {
    private final RegionRepository regions;
    private final ProvinceRepository provinces;
    private final CategoryRepository categories;
    private final Scope scope;

    public RefController(RegionRepository regions, ProvinceRepository provinces, CategoryRepository categories, Scope scope) {
        this.regions = regions; this.provinces = provinces; this.categories = categories; this.scope = scope;
    }

    /** Reference lists, limited to the caller's perimeter. */
    @GetMapping
    @Transactional(readOnly = true)
    public RefData get(@AuthenticationPrincipal AppUserPrincipal me) {
        var v = scope.of(me);
        var provs = provinces.findAll().stream().filter(p -> v.contains(p.getId())).toList();
        var regionIds = provs.stream().map(p -> p.getRegion().getId()).collect(java.util.stream.Collectors.toSet());
        return new RefData(
                regions.findAll().stream().filter(r -> regionIds.contains(r.getId()))
                        .map(r -> new RegionDto(r.getId(), r.getCode(), r.getNomFr(), r.getNomAr())).toList(),
                provs.stream().map(p -> new ProvinceDto(p.getId(), p.getCode(), p.getRegion().getId(), p.getNomFr(), p.getNomAr())).toList(),
                categories.findAll().stream().filter(c -> c.isActif()).map(c -> new CategoryDto(c.getId(), c.getCode(), c.getNomFr(), c.getNomAr())).toList());
    }
}
