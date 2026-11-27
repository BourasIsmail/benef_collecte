package ma.entraide.suivi.service;

import ma.entraide.suivi.domain.Province;
import ma.entraide.suivi.domain.Role;
import ma.entraide.suivi.pdf.BeneficiaryPdf;
import ma.entraide.suivi.pdf.BeneficiaryPdf.*;
import ma.entraide.suivi.repo.ProvinceRepository;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.web.Dtos.BeneficiaryDto;
import ma.entraide.suivi.web.Dtos.Me;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the "Les bénéficiaires" PDF for the caller's perimeter and filters.
 * One province in the result -> "Les bénéficiaires de la province : X". Several provinces -> province bands.
 */
@Service
public class BeneficiaryPdfService {
    private final BeneficiaryService beneficiaries;
    private final ProvinceRepository provinces;
    private final AuthService auth;
    private final Scope scope;
    private final AuditService audit;

    public BeneficiaryPdfService(BeneficiaryService beneficiaries, ProvinceRepository provinces, AuthService auth, Scope scope, AuditService audit) {
        this.beneficiaries = beneficiaries; this.provinces = provinces; this.auth = auth; this.scope = scope; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public byte[] export(AppUserPrincipal me, String q, Long etabId, Long catId, Long assoId, Long provId, String sexe) {
        List<BeneficiaryDto> rows = beneficiaries.exportAll(me, q, etabId, catId, assoId, provId, sexe);

        // rows arrive ordered by province, association, établissement, name
        Map<Long, Prov> provs = new LinkedHashMap<>();
        Map<String, Asso> assos = new LinkedHashMap<>();
        Map<String, Etab> etabs = new LinkedHashMap<>();
        for (BeneficiaryDto b : rows) {
            Prov p = provs.computeIfAbsent(b.provinceId(), k -> new Prov(b.provinceNomFr(), b.provinceNomAr(), new ArrayList<>()));
            Asso a = assos.computeIfAbsent(b.provinceId() + "|" + b.associationId(), k -> { Asso x = new Asso(b.associationNom(), new ArrayList<>()); p.assos().add(x); return x; });
            Etab e = etabs.computeIfAbsent(b.associationId() + "|" + b.etablissementId(), k -> { Etab x = new Etab(b.etablissementNom(), new ArrayList<>()); a.etabs().add(x); return x; });
            e.people().add(new Person(b.nomFr() + " " + b.prenomFr(), b.nomAr() + " " + b.prenomAr(), b.cin(), b.dateNaissance(), b.sexe()));
        }

        // which single province (if any) does the document speak about?
        Long single = null;
        if (provId != null && scope.of(me).contains(provId)) single = provId;
        else if (me.role() == Role.PROVINCE) single = me.provinceId();
        else if (provs.size() == 1) single = provs.keySet().iterator().next();

        String titleFr, titleAr, regionFr, regionAr, placeFr = null, placeAr = null;
        boolean bands = true;
        if (single != null) {
            Province p = provinces.findById(single).orElseThrow();
            String arName = p.getNomAr() != null ? p.getNomAr() : p.getNomFr();
            titleFr = "Les bénéficiaires de la province : " + p.getNomFr();
            titleAr = "المستفيدون بإقليم : " + arName;
            regionFr = p.getRegion().getNomFr();
            regionAr = p.getRegion().getNomAr() != null ? p.getRegion().getNomAr() : regionFr;
            placeFr = p.getNomFr();
            placeAr = arName;
            bands = false;
        } else if (me.role() == Role.REGIONAL) {
            Me m = auth.me(me);
            String ar = m.regionNomAr() != null ? m.regionNomAr() : m.regionNomFr();
            titleFr = "Les bénéficiaires de la région : " + m.regionNomFr();
            titleAr = "المستفيدون بالجهة : " + ar;
            regionFr = m.regionNomFr();
            regionAr = ar;
        } else {
            titleFr = "Les bénéficiaires — toutes les provinces";
            titleAr = "المستفيدون — جميع الأقاليم";
            regionFr = "Toutes les régions";
            regionAr = "جميع الجهات";
        }

        byte[] pdf = BeneficiaryPdf.render(new Data(titleFr, titleAr, regionFr, regionAr, placeFr, placeAr,
                LocalDate.now(), me.email(), bands, new ArrayList<>(provs.values())));
        audit.log(me.email(), "EXPORT_PDF", "Beneficiary", null, rows.size() + " lignes");
        return pdf;
    }
}
