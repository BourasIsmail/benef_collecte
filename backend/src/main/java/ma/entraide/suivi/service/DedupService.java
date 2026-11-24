package ma.entraide.suivi.service;

import ma.entraide.suivi.domain.Beneficiary;
import ma.entraide.suivi.repo.BeneficiaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DedupService {
    public record Match(Long id, String nomFr, String prenomFr, LocalDate dateNaissance, String associationNom,
                        String provinceNomFr, String provinceNomAr, boolean visible) {}
    public record Similar(Match match, String reason) {}
    public record Result(String key, List<Match> exact, List<Similar> similar) {}

    private final BeneficiaryRepository repo;

    public DedupService(BeneficiaryRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public Result check(String nomFr, String prenomFr, LocalDate dob, Long excludeId, Scope.View scope) {
        String key = DedupKey.of(nomFr, prenomFr, dob);
        String names = DedupKey.names(nomFr, prenomFr);
        List<Match> exact = new ArrayList<>();
        Map<Long, Similar> similar = new LinkedHashMap<>();

        // candidates: exact key, swapped day/month, neighbouring years
        List<String> keys = new ArrayList<>(List.of(key));
        LocalDate swapped = swapDayMonth(dob);
        String swappedKey = swapped == null ? null : names + "|" + swapped;
        if (swappedKey != null) keys.add(swappedKey);
        String yearBefore = yearKey(names, dob, -1), yearAfter = yearKey(names, dob, 1);
        if (yearBefore != null) keys.add(yearBefore);
        if (yearAfter != null) keys.add(yearAfter);

        for (Beneficiary b : repo.findActiveByDedupKeys(keys)) {
            if (b.getId().equals(excludeId)) continue;
            Match m = toMatch(b, scope);
            if (b.getDedupKey().equals(key)) exact.add(m);
            else if (b.getDedupKey().equals(swappedKey)) similar.put(b.getId(), new Similar(m, "SWAPPED_DATE"));
            else similar.put(b.getId(), new Similar(m, "NEIGHBOUR_YEAR"));
        }
        // same birth date, very close names
        for (Beneficiary b : repo.findActiveByDob(dob)) {
            if (b.getId().equals(excludeId) || b.getDedupKey().equals(key) || similar.containsKey(b.getId())) continue;
            String other = DedupKey.names(b.getNomFr(), b.getPrenomFr());
            if (DedupKey.levenshtein(names, other) <= 2) similar.put(b.getId(), new Similar(toMatch(b, scope), "CLOSE_NAME"));
        }
        return new Result(key, exact, new ArrayList<>(similar.values()));
    }

    private static LocalDate swapDayMonth(LocalDate d) {
        if (d.getDayOfMonth() > 12 || d.getDayOfMonth() == d.getMonthValue()) return null;
        return LocalDate.of(d.getYear(), d.getDayOfMonth(), d.getMonthValue());
    }

    private static String yearKey(String names, LocalDate d, int delta) {
        try { return names + "|" + d.withYear(d.getYear() + delta); } catch (Exception e) { return null; }
    }

    private Match toMatch(Beneficiary b, Scope.View scope) {
        var prov = b.getAssociation().getProvince();
        boolean visible = scope.contains(prov.getId());
        // outside the user's perimeter: confirm that it exists, reveal nothing else
        return new Match(visible ? b.getId() : null, visible ? b.getNomFr() : null, visible ? b.getPrenomFr() : null, visible ? b.getDateNaissance() : null,
                visible ? b.getAssociation().getNomAr() : null, visible ? prov.getNomFr() : null,
                visible ? prov.getNomAr() : null, visible);
    }
}
