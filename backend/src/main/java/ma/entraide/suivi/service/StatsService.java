package ma.entraide.suivi.service;

import jakarta.persistence.EntityManager;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.web.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatsService {
    private final EntityManager em;
    private final Scope scope;

    public StatsService(EntityManager em, Scope scope) { this.em = em; this.scope = scope; }

    /** Perimeter filter on the province of the établissement (alias p). */
    private static final String SCOPE = "(:all = true or p.id in (:ids))";

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, Scope.View v) {
        var q = em.createNativeQuery(sql);
        q.setParameter("all", v.all());
        q.setParameter("ids", v.ids());
        return q.getResultList();
    }

    private static long num(Object o) { return o == null ? 0 : ((Number) o).longValue(); }

    @Transactional(readOnly = true)
    public Stats compute(AppUserPrincipal me) {
        var v = scope.of(me);
        String base = " from beneficiaires b join etablissements e on e.id = b.etablissement_id join associations a on a.id = e.association_id join provinces p on p.id = a.province_id "
                + "where b.statut = 'ACTIF' and " + SCOPE;

        Object[] totals = rows("select count(*), coalesce(sum(b.sexe = 'F'), 0)" + base, v).get(0);
        long total = num(totals[0]), girls = num(totals[1]);

        long etabCount = num(rows("select count(*) from etablissements e join associations a on a.id = e.association_id join provinces p on p.id = a.province_id where e.actif = true and " + SCOPE, v).get(0));
        long assoCount = num(rows("select count(*) from associations a join provinces p on p.id = a.province_id where " + SCOPE, v).get(0));
        long provCount = num(rows("select count(*) from provinces p where " + SCOPE, v).get(0));

        List<Count> byProvince = rows("select p.nom_fr, p.nom_ar, count(b.id) from provinces p left join associations a on a.province_id = p.id left join etablissements e on e.association_id = a.id "
                + "left join beneficiaires b on b.etablissement_id = e.id and b.statut = 'ACTIF' where " + SCOPE
                + " group by p.id, p.nom_fr, p.nom_ar order by count(b.id) desc, p.nom_fr", v)
                .stream().map(r -> new Count((String) r[0], (String) r[1], num(r[2]))).toList();

        List<Count> byEtab = rows("select e.nom_ar, e.nom_ar, count(b.id) from etablissements e join associations a on a.id = e.association_id join provinces p on p.id = a.province_id "
                + "left join beneficiaires b on b.etablissement_id = e.id and b.statut = 'ACTIF' where e.actif = true and " + SCOPE
                + " group by e.id, e.nom_ar order by count(b.id) desc, e.nom_ar limit 15", v)
                .stream().map(r -> new Count((String) r[0], (String) r[1], num(r[2]))).toList();

        // age bands (beneficiaries cover all ages: children, adults, elderly)
        Object[] age = rows("select coalesce(sum(t.a < 6),0), coalesce(sum(t.a between 6 and 12),0), coalesce(sum(t.a between 13 and 17),0), "
                + "coalesce(sum(t.a between 18 and 59),0), coalesce(sum(t.a >= 60),0) from (select timestampdiff(year, b.date_naissance, curdate()) a" + base + ") t", v).get(0);
        String[] ageLabels = {"0–5", "6–12", "13–17", "18–59", "60+"};
        List<Count> ageBands = new ArrayList<>();
        for (int i = 0; i < ageLabels.length; i++) ageBands.add(new Count(ageLabels[i], ageLabels[i], num(age[i])));

        List<Count> byCat = rows("select c.nom_fr, c.nom_ar, count(x.id) from categories c "
                + "left join (select e.id, e.category_id from etablissements e join associations a on a.id = e.association_id join provinces p on p.id = a.province_id where e.actif = true and " + SCOPE + ") x on x.category_id = c.id "
                + "where c.actif = true group by c.id, c.nom_fr, c.nom_ar order by count(x.id) desc, c.id", v)
                .stream().map(r -> new Count((String) r[0], (String) r[1], num(r[2]))).toList();

        List<LatestDto> latest = rows("select b.id, b.nom_fr, b.prenom_fr, b.sexe, e.nom_ar" + base + " order by b.created_at desc, b.id desc limit 6", v)
                .stream().map(r -> new LatestDto(num(r[0]), (String) r[1], (String) r[2], String.valueOf(r[3]), (String) r[4])).toList();

        return new Stats(total, girls, total - girls, assoCount, etabCount, provCount, byProvince, byEtab, ageBands, byCat, latest);
    }
}
