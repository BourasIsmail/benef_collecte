package ma.entraide.suivi.web;

import jakarta.validation.Valid;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.service.AssociationService;
import ma.entraide.suivi.web.Dtos.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/associations")
public class AssociationController {
    private final AssociationService service;

    public AssociationController(AssociationService service) { this.service = service; }

    @GetMapping
    public List<AssociationDto> list(@AuthenticationPrincipal AppUserPrincipal me,
                                     @RequestParam(required = false) Long provinceId, @RequestParam(required = false) Long categoryId,
                                     @RequestParam(required = false) String q, @RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.list(me, provinceId, categoryId, q, includeArchived);
    }

    @PostMapping
    public AssociationDto create(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody AssociationRequest r) {
        return service.create(me, r);
    }

    @PutMapping("/{id}")
    public AssociationDto update(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id, @Valid @RequestBody AssociationRequest r) {
        return service.update(me, id, r);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) { service.delete(me, id); }
}
