package ma.entraide.suivi.service;

import ma.entraide.suivi.domain.Association;
import ma.entraide.suivi.domain.Category;
import ma.entraide.suivi.domain.Province;
import ma.entraide.suivi.repo.AssociationRepository;
import ma.entraide.suivi.repo.BeneficiaryRepository;
import ma.entraide.suivi.repo.CategoryRepository;
import ma.entraide.suivi.repo.ProvinceRepository;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.util.Text;
import ma.entraide.suivi.web.ApiException;
import ma.entraide.suivi.web.Dtos.AssociationDto;
import ma.entraide.suivi.web.Dtos.AssociationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssociationService {
    private final AssociationRepository repo;
    private final ProvinceRepository provinces;
    private final CategoryRepository categories;
    private final BeneficiaryRepository beneficiaries;
    private final Scope scope;
    private final AuditService audit;

    public AssociationService(AssociationRepository repo, ProvinceRepository provinces, CategoryRepository categories,
                              BeneficiaryRepository beneficiaries, Scope scope, AuditService audit) {
        this.repo = repo; this.provinces = provinces; this.categories = categories;
        this.beneficiaries = beneficiaries; this.scope = scope; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AssociationDto> list(AppUserPrincipal me, Long provinceId, Long categoryId, String q, boolean includeArchived) {
        var view = scope.of(me);
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : repo.countActiveBeneficiaries()) counts.put((Long) row[0], (Long) row[1]);
        String term = Text.normAr(q);
        return repo.findScoped(view.all(), view.ids()).stream()
                .filter(a -> includeArchived || a.isActif())
                .filter(a -> provinceId == null || a.getProvince().getId().equals(provinceId))
                .filter(a -> categoryId == null || a.getCategory() != null && a.getCategory().getId().equals(categoryId))
                .filter(a -> term.isEmpty() || Text.normAr(a.getNomAr()).contains(term))
                .map(a -> toDto(a, counts.getOrDefault(a.getId(), 0L))).toList();
    }

    @Transactional
    public AssociationDto create(AppUserPrincipal me, AssociationRequest r) {
        Province p = writableProvince(me, r.provinceId());
        String nom = Text.clean(r.nomAr());
        if (repo.existsByProvinceIdAndNomAr(p.getId(), nom)) throw new ApiException(HttpStatus.CONFLICT, "ASSOCIATION_EXISTS");
        Association a = new Association();
        a.setProvince(p);
        a.setNomAr(nom);
        a.setActif(true);
        a.setCategory(resolveCategory(r.categoryId()));
        repo.save(a);
        audit.log(me.email(), "ASSOCIATION_CREATED", "Association", a.getId(), nom);
        return toDto(a, 0);
    }

    @Transactional
    public AssociationDto update(AppUserPrincipal me, Long id, AssociationRequest r) {
        Association a = repo.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
        writableProvince(me, a.getProvince().getId());
        Province target = writableProvince(me, r.provinceId());
        String nom = Text.clean(r.nomAr());
        if (repo.existsByProvinceIdAndNomArAndIdNot(target.getId(), nom, id)) throw new ApiException(HttpStatus.CONFLICT, "ASSOCIATION_EXISTS");
        a.setProvince(target);
        a.setNomAr(nom);
        a.setCategory(resolveCategory(r.categoryId()));
        audit.log(me.email(), "ASSOCIATION_UPDATED", "Association", id, nom);
        return toDto(a, beneficiaries.countByAssociationId(id));
    }

    @Transactional
    public void delete(AppUserPrincipal me, Long id) {
        Association a = repo.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
        writableProvince(me, a.getProvince().getId());
        long n = beneficiaries.countByAssociationId(id);
        if (n > 0) throw new ApiException(HttpStatus.CONFLICT, "HAS_BENEFICIARIES", n);
        repo.delete(a);
        audit.log(me.email(), "ASSOCIATION_DELETED", "Association", id, a.getNomAr());
    }

    /** Archiving keeps the association and its beneficiaries but hides it from the entry forms. */
    @Transactional
    public void setActive(AppUserPrincipal me, Long id, boolean actif) {
        Association a = repo.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
        writableProvince(me, a.getProvince().getId());
        a.setActif(actif);
        audit.log(me.email(), actif ? "ASSOCIATION_UNARCHIVED" : "ASSOCIATION_ARCHIVED", "Association", id, a.getNomAr());
    }

    /** ADMIN: any province; PROVINCE: its own province only; REGIONAL: never. */
    private Province writableProvince(AppUserPrincipal me, Long provinceId) {
        if (!Scope.canWrite(me)) throw new ApiException(HttpStatus.FORBIDDEN, "READ_ONLY");
        if (!scope.of(me).contains(provinceId)) throw new ApiException(HttpStatus.FORBIDDEN, "OUT_OF_SCOPE");
        return provinces.findById(provinceId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROVINCE_NOT_FOUND"));
    }

    private Category resolveCategory(Long id) {
        return categories.findById(id).filter(Category::isActif)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CATEGORIES"));
    }

    private AssociationDto toDto(Association a, long count) {
        return new AssociationDto(a.getId(), a.getProvince().getId(), a.getProvince().getNomFr(), a.getProvince().getNomAr(),
                a.getNomAr(), a.isActif(), a.getCategory() == null ? null : a.getCategory().getId(), count);
    }
}
