package ma.entraide.suivi.service;

import ma.entraide.suivi.domain.Association;
import ma.entraide.suivi.domain.Province;
import ma.entraide.suivi.repo.AssociationRepository;
import ma.entraide.suivi.repo.EtablissementRepository;
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

/** Everybody can read the associations of their perimeter; only the ADMIN can create, change or delete them. */
@Service
public class AssociationService {
    private final AssociationRepository repo;
    private final EtablissementRepository etablissements;
    private final ProvinceRepository provinces;
    private final Scope scope;
    private final AuditService audit;

    public AssociationService(AssociationRepository repo, EtablissementRepository etablissements, ProvinceRepository provinces,
                              Scope scope, AuditService audit) {
        this.repo = repo; this.etablissements = etablissements; this.provinces = provinces; this.scope = scope; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AssociationDto> list(AppUserPrincipal me, Long provinceId, Long categoryId, String q) {
        var v = scope.of(me);
        Map<Long, Long> etabCounts = toMap(repo.countEtablissements(v.all(), v.ids()));
        Map<Long, Long> benefCounts = toMap(repo.countBeneficiaries(v.all(), v.ids()));
        String term = Text.normAr(q);
        var withCategory = categoryId == null ? null : repo.idsWithCategory(categoryId);
        return repo.findScoped(v.all(), v.ids()).stream()
                .filter(a -> provinceId == null || a.getProvince().getId().equals(provinceId))
                .filter(a -> withCategory == null || withCategory.contains(a.getId()))
                .filter(a -> term.isEmpty() || Text.normAr(a.getNomAr()).contains(term))
                .map(a -> toDto(a, etabCounts.getOrDefault(a.getId(), 0L), benefCounts.getOrDefault(a.getId(), 0L))).toList();
    }

    @Transactional
    public AssociationDto create(AppUserPrincipal me, AssociationRequest r) {
        requireAdmin(me);
        Province p = provinces.findById(r.provinceId()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "PROVINCE_NOT_FOUND"));
        String nom = Text.clean(r.nomAr());
        if (repo.existsByProvinceIdAndNomAr(p.getId(), nom)) throw new ApiException(HttpStatus.CONFLICT, "ASSOCIATION_EXISTS");
        Association a = new Association();
        a.setProvince(p);
        a.setNomAr(nom);
        repo.save(a);
        audit.log(me.email(), "ASSOCIATION_CREATED", "Association", a.getId(), nom + " (" + p.getNomFr() + ")");
        return toDto(a, 0, 0);
    }

    @Transactional
    public AssociationDto update(AppUserPrincipal me, Long id, AssociationRequest r) {
        requireAdmin(me);
        Association a = repo.findWithProvince(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
        Province p = provinces.findById(r.provinceId()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "PROVINCE_NOT_FOUND"));
        String nom = Text.clean(r.nomAr());
        if (repo.existsByProvinceIdAndNomArAndIdNot(p.getId(), nom, id)) throw new ApiException(HttpStatus.CONFLICT, "ASSOCIATION_EXISTS");
        a.setProvince(p);   // its établissements and their beneficiaries follow the association
        a.setNomAr(nom);
        audit.log(me.email(), "ASSOCIATION_UPDATED", "Association", id, nom + " (" + p.getNomFr() + ")");
        return toDto(a, etablissements.countByAssociationId(id), 0);
    }

    @Transactional
    public void delete(AppUserPrincipal me, Long id) {
        requireAdmin(me);
        Association a = repo.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
        long n = etablissements.countByAssociationId(id);
        if (n > 0) throw new ApiException(HttpStatus.CONFLICT, "HAS_ETABLISSEMENTS", n);
        repo.delete(a);
        audit.log(me.email(), "ASSOCIATION_DELETED", "Association", id, a.getNomAr());
    }

    static void requireAdmin(AppUserPrincipal me) {
        if (me.role() != ma.entraide.suivi.domain.Role.ADMIN) throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_ONLY");
    }

    private static AssociationDto toDto(Association a, long etabs, long benefs) {
        return new AssociationDto(a.getId(), a.getProvince().getId(), a.getProvince().getNomFr(), a.getProvince().getNomAr(), a.getNomAr(), etabs, benefs);
    }

    private static Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> m = new HashMap<>();
        for (Object[] r : rows) m.put((Long) r[0], (Long) r[1]);
        return m;
    }
}
