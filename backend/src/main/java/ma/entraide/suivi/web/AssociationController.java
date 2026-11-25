package ma.entraide.suivi.web;

import jakarta.validation.Valid;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.service.AssociationService;
import ma.entraide.suivi.service.EtablissementService;
import ma.entraide.suivi.web.Dtos.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public class AssociationController {
    private AssociationController() {}

    @RestController @RequestMapping("/api/associations")
    public static class Associations {
        private final AssociationService service;
        public Associations(AssociationService service) { this.service = service; }

        @GetMapping
        public List<AssociationDto> list(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam(required = false) Long provinceId,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) String q) { return service.list(me, provinceId, categoryId, q); }

        @PostMapping
        public AssociationDto create(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody AssociationRequest r) { return service.create(me, r); }

        @PutMapping("/{id}")
        public AssociationDto update(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id, @Valid @RequestBody AssociationRequest r) { return service.update(me, id, r); }

        @DeleteMapping("/{id}")
        public void delete(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) { service.delete(me, id); }
    }

    @RestController @RequestMapping("/api/etablissements")
    public static class Etablissements {
        private final EtablissementService service;
        public Etablissements(EtablissementService service) { this.service = service; }

        @GetMapping
        public List<EtablissementDto> list(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam(required = false) Long associationId,
                                           @RequestParam(required = false) Long provinceId, @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String q, @RequestParam(defaultValue = "false") boolean includeArchived) {
            return service.list(me, associationId, provinceId, categoryId, q, includeArchived);
        }

        @PostMapping
        public EtablissementDto create(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody EtablissementRequest r) { return service.create(me, r); }

        @PutMapping("/{id}")
        public EtablissementDto update(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id, @Valid @RequestBody EtablissementRequest r) { return service.update(me, id, r); }

        @PostMapping("/{id}/archive")
        public void archive(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) { service.setActive(me, id, false); }

        @PostMapping("/{id}/unarchive")
        public void unarchive(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) { service.setActive(me, id, true); }

        @DeleteMapping("/{id}")
        public void delete(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) { service.delete(me, id); }
    }
}
