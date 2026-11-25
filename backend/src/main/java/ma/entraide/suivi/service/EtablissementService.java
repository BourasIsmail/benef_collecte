package ma.entraide.suivi.service;

import ma.entraide.suivi.domain.Association;
import ma.entraide.suivi.domain.Category;
import ma.entraide.suivi.domain.Etablissement;
import ma.entraide.suivi.domain.Province;
import ma.entraide.suivi.repo.*;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.util.Text;
import ma.entraide.suivi.web.ApiException;
import ma.entraide.suivi.web.Dtos.EtablissementDto;
import ma.entraide.suivi.web.Dtos.EtablissementRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Everybody can read the établissements of their perimeter; only the ADMIN can create, change, archive or delete them. */
@Service
public class EtablissementService {
    private final EtablissementRepository repo;
    private final AssociationRepository associations;
    private final ProvinceRepository provinces;
    private final CategoryRepository categories;
    private final BeneficiaryRepository beneficiaries;
    private final Scope scope;
    private final AuditService audit;

    public EtablissementService(EtablissementRepository repo, AssociationRepository associations, ProvinceRepository provinces,
                                CategoryRepository categories, BeneficiaryRepository beneficiaries, Scope scope, AuditService audit) {
        this.repo = repo; this.associations = associations; this.provinces = provinces; this.categories = categories;
        this.beneficiaries = beneficiaries; this.scope = scope; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<EtablissementDto> list(AppUserPrincipal me, Long associationId, Long provinceId, Long categoryId, String q, boolean includeArchived) {
        var v = scope.of(me);
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] r : repo.countActiveBeneficiaries()) counts.put((Long) r[0], (Long) r[1]);
        String term = Text.normAr(q);
        return repo.findScoped(v.all(), v.ids()).stream()
                .filter(e -> includeArchived || e.isActif())
                .filter(e -> associationId == null || e.getAssociation().getId().equals(associationId))
                .filter(e -> provinceId == null || e.getProvince().getId().equals(provinceId))
                .filter(e -> categoryId == null || e.getCategory().getId().equals(categoryId))
                .filter(e -> term.isEmpty() || Text.normAr(e.getNomAr()).contains(term) || Text.normAr(e.getAssociation().getNomAr()).contains(term))
                .map(e -> toDto(e, counts.getOrDefault(e.getId(), 0L))).toList();
    }

    @Transactional
    public EtablissementDto create(AppUserPrincipal me, EtablissementRequest r) {
        AssociationService.requireAdmin(me);
        Association a = associations.findById(r.associationId()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "ASSOCIATION_NOT_FOUND"));
        Province p = provinces.findById(r.provinceId()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "PROVINCE_NOT_FOUND"));
        Category c = category(r.categoryId());
        String nom = Text.clean(r.nomAr());
        if (repo.existsByAssociationIdAndProvinceIdAndNomAr(a.getId(), p.getId(), nom)) throw new ApiException(HttpStatus.CONFLICT, "ETABLISSEMENT_EXISTS");
        Etablissement e = new Etablissement();
        fill(e, a, p, c, nom, r);
        repo.save(e);
        audit.log(me.email(), "ETABLISSEMENT_CREATED", "Etablissement", e.getId(), nom);
        return toDto(e, 0);
    }

    @Transactional
    public EtablissementDto update(AppUserPrincipal me, Long id, EtablissementRequest r) {
        AssociationService.requireAdmin(me);
        Etablissement e = find(id);
        Association a = associations.findById(r.associationId()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "ASSOCIATION_NOT_FOUND"));
        Province p = provinces.findById(r.provinceId()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "PROVINCE_NOT_FOUND"));
        Category c = category(r.categoryId());
        String nom = Text.clean(r.nomAr());
        if (repo.existsByAssociationIdAndProvinceIdAndNomArAndIdNot(a.getId(), p.getId(), nom, id)) throw new ApiException(HttpStatus.CONFLICT, "ETABLISSEMENT_EXISTS");
        fill(e, a, p, c, nom, r);
        audit.log(me.email(), "ETABLISSEMENT_UPDATED", "Etablissement", id, nom);
        return toDto(e, beneficiaries.countByEtablissementId(id));
    }

    @Transactional
    public void setActive(AppUserPrincipal me, Long id, boolean actif) {
        AssociationService.requireAdmin(me);
        Etablissement e = find(id);
        e.setActif(actif);
        audit.log(me.email(), actif ? "ETABLISSEMENT_UNARCHIVED" : "ETABLISSEMENT_ARCHIVED", "Etablissement", id, e.getNomAr());
    }

    @Transactional
    public void delete(AppUserPrincipal me, Long id) {
        AssociationService.requireAdmin(me);
        Etablissement e = find(id);
        long n = beneficiaries.countByEtablissementId(id);
        if (n > 0) throw new ApiException(HttpStatus.CONFLICT, "HAS_BENEFICIARIES", n);
        repo.delete(e);
        audit.log(me.email(), "ETABLISSEMENT_DELETED", "Etablissement", id, e.getNomAr());
    }

    private Etablissement find(Long id) {
        return repo.findWithRefs(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
    }

    private Category category(Long id) {
        return categories.findById(id).filter(Category::isActif).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CATEGORIES"));
    }

    private static void fill(Etablissement e, Association a, Province p, Category c, String nom, EtablissementRequest r) {
        e.setAssociation(a);
        e.setProvince(p);
        e.setCategory(c);
        e.setNomAr(nom);
        e.setTypeEtablissement(blank(r.typeEtablissement()));
        e.setNumeroLicence(blank(r.numeroLicence()));
    }

    private static String blank(String s) { return s == null || s.isBlank() ? null : Text.clean(s); }

    static EtablissementDto toDto(Etablissement e, long beneficiaryCount) {
        return new EtablissementDto(e.getId(), e.getAssociation().getId(), e.getAssociation().getNomAr(), e.getProvince().getId(),
                e.getProvince().getNomFr(), e.getProvince().getNomAr(), e.getCategory().getId(), e.getNomAr(),
                e.getTypeEtablissement(), e.getNumeroLicence(), e.isActif(), beneficiaryCount);
    }
}
