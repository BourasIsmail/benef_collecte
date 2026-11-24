package ma.entraide.suivi.service;

import ma.entraide.suivi.domain.Association;
import ma.entraide.suivi.domain.Beneficiary;
import ma.entraide.suivi.repo.AssociationRepository;
import ma.entraide.suivi.repo.BeneficiaryRepository;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.util.Text;
import ma.entraide.suivi.web.ApiException;
import ma.entraide.suivi.web.Dtos.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class BeneficiaryService {
    private static final Pattern CIN = Pattern.compile("^[A-Z]{1,2}[0-9]{4,8}$");
    private static final LocalDate MIN_DOB = LocalDate.of(1900, 1, 1);

    private final BeneficiaryRepository repo;
    private final AssociationRepository associations;
    private final DedupService dedup;
    private final Scope scope;
    private final AuditService audit;

    public BeneficiaryService(BeneficiaryRepository repo, AssociationRepository associations, DedupService dedup,
                              Scope scope, AuditService audit) {
        this.repo = repo; this.associations = associations; this.dedup = dedup; this.scope = scope; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageDto<BeneficiaryDto> search(AppUserPrincipal me, String q, Long assoId, Long provId, String sexe, int page, int size) {
        var v = scope.of(me);
        var p = repo.search(v.all(), v.ids(), assoId, provId, blankToNull(sexe), Text.searchTerm(q),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return new PageDto<>(p.getContent().stream().map(this::toDto).toList(), p.getTotalElements(), p.getNumber(), p.getSize());
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryDto> exportAll(AppUserPrincipal me, String q, Long assoId, Long provId, String sexe) {
        var v = scope.of(me);
        return repo.searchAll(v.all(), v.ids(), assoId, provId, blankToNull(sexe), Text.searchTerm(q)).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryDto get(AppUserPrincipal me, Long id) {
        Beneficiary b = find(id);
        requireVisible(me, b);
        return toDto(b);
    }

    @Transactional(readOnly = true)
    public DedupService.Result check(AppUserPrincipal me, CheckRequest r) {
        return dedup.check(r.nomFr(), r.prenomFr(), r.dateNaissance(), r.excludeId(), scope.of(me));
    }

    @Transactional
    public BeneficiaryDto create(AppUserPrincipal me, BeneficiaryRequest r) {
        Association a = writableAssociation(me, r.associationId());
        validate(r);
        String cin = normCin(r.cin());
        if (cin != null && repo.existsByCin(cin)) throw new ApiException(HttpStatus.CONFLICT, "CIN_EXISTS");
        var res = dedup.check(r.nomFr(), r.prenomFr(), r.dateNaissance(), null, scope.of(me));
        if (!res.exact().isEmpty() && !r.confirmDuplicate()) throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE", res);
        Beneficiary b = new Beneficiary();
        b.setCreatedBy(me.email());
        apply(b, a, r, cin, me);
        repo.save(b);
        audit.log(me.email(), res.exact().isEmpty() ? "BENEFICIARY_CREATED" : "BENEFICIARY_CREATED_DUPLICATE_CONFIRMED", "Beneficiary", b.getId(), b.getDedupKey());
        return toDto(b);
    }

    @Transactional
    public BeneficiaryDto update(AppUserPrincipal me, Long id, BeneficiaryRequest r) {
        Beneficiary b = find(id);
        requireWritable(me, b);
        Association a = writableAssociation(me, r.associationId());
        validate(r);
        String cin = normCin(r.cin());
        if (cin != null && repo.existsByCinAndIdNot(cin, id)) throw new ApiException(HttpStatus.CONFLICT, "CIN_EXISTS");
        var res = dedup.check(r.nomFr(), r.prenomFr(), r.dateNaissance(), id, scope.of(me));
        if (!res.exact().isEmpty() && !r.confirmDuplicate()) throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE", res);
        apply(b, a, r, cin, me);
        audit.log(me.email(), "BENEFICIARY_UPDATED", "Beneficiary", id, b.getDedupKey());
        return toDto(b);
    }

    /** Soft delete: the record is archived, never physically removed. */
    @Transactional
    public void archive(AppUserPrincipal me, Long id) {
        Beneficiary b = find(id);
        requireWritable(me, b);
        b.setStatut("ARCHIVE");
        b.setCin(null);
        b.setUpdatedBy(me.email());
        audit.log(me.email(), "BENEFICIARY_ARCHIVED", "Beneficiary", id, b.getDedupKey());
    }

    // ---- helpers

    private void apply(Beneficiary b, Association a, BeneficiaryRequest r, String cin, AppUserPrincipal me) {
        b.setAssociation(a);
        b.setNomFr(Text.clean(r.nomFr()).toUpperCase());
        b.setPrenomFr(Text.clean(r.prenomFr()).toUpperCase());
        b.setNomAr(Text.clean(r.nomAr()));
        b.setPrenomAr(Text.clean(r.prenomAr()));
        b.setDateNaissance(r.dateNaissance());
        b.setSexe(r.sexe());
        b.setCin(cin);
        b.setDedupKey(DedupKey.of(r.nomFr(), r.prenomFr(), r.dateNaissance()));
        b.setSearchText(Text.searchText(r.nomFr(), r.prenomFr(), r.nomAr(), r.prenomAr()));
        b.setUpdatedBy(me.email());
    }

    private void validate(BeneficiaryRequest r) {
        if (r.dateNaissance().isBefore(MIN_DOB)) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BIRTH_DATE");
        if (DedupKey.word(r.nomFr()).isEmpty() || DedupKey.word(r.prenomFr()).isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FR_NAME");
        if (normCin(r.cin()) != null && !CIN.matcher(normCin(r.cin())).matches()) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CIN");
    }

    private static String normCin(String cin) {
        if (cin == null || cin.isBlank()) return null;
        return cin.trim().toUpperCase().replaceAll("\\s+", "");
    }

    private Association writableAssociation(AppUserPrincipal me, Long id) {
        if (!Scope.canWrite(me)) throw new ApiException(HttpStatus.FORBIDDEN, "READ_ONLY");
        Association a = associations.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ASSOCIATION_NOT_FOUND"));
        if (!scope.of(me).contains(a.getProvince().getId())) throw new ApiException(HttpStatus.FORBIDDEN, "OUT_OF_SCOPE");
        if (!a.isActif()) throw new ApiException(HttpStatus.CONFLICT, "ASSOCIATION_ARCHIVED");
        return a;
    }

    private Beneficiary find(Long id) {
        return repo.findById(id).filter(b -> "ACTIF".equals(b.getStatut()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
    }

    private void requireVisible(AppUserPrincipal me, Beneficiary b) {
        if (!scope.of(me).contains(b.getAssociation().getProvince().getId())) throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    private void requireWritable(AppUserPrincipal me, Beneficiary b) {
        requireVisible(me, b);
        if (!Scope.canWrite(me)) throw new ApiException(HttpStatus.FORBIDDEN, "READ_ONLY");
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }

    private BeneficiaryDto toDto(Beneficiary b) {
        var a = b.getAssociation();
        var p = a.getProvince();
        return new BeneficiaryDto(b.getId(), a.getId(), a.getNomAr(), p.getId(), p.getNomFr(), p.getNomAr(), b.getNomFr(),
                b.getPrenomFr(), b.getNomAr(), b.getPrenomAr(), b.getDateNaissance(), b.getSexe(), b.getCin());
    }
}
