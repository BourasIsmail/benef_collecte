package ma.entraide.suivi.repo;

import ma.entraide.suivi.domain.Association;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AssociationRepository extends JpaRepository<Association, Long> {
    boolean existsByNomAr(String nomAr);
    boolean existsByNomArAndIdNot(String nomAr, Long id);

    /** Associations having at least one établissement in the caller's perimeter. */
    @Query("select distinct a from Etablissement e join e.association a where (:all = true or e.province.id in :ids) order by a.nomAr")
    List<Association> findScoped(@Param("all") boolean all, @Param("ids") Collection<Long> ids);

    @Query("select a from Association a order by a.nomAr")
    List<Association> findAllSorted();

    @Query("select e.association.id, count(e) from Etablissement e where e.actif = true and (:all = true or e.province.id in :ids) group by e.association.id")
    List<Object[]> countEtablissements(@Param("all") boolean all, @Param("ids") Collection<Long> ids);

    @Query("select e.association.id, count(b) from Beneficiary b join b.etablissement e where b.statut = 'ACTIF' and (:all = true or e.province.id in :ids) group by e.association.id")
    List<Object[]> countBeneficiaries(@Param("all") boolean all, @Param("ids") Collection<Long> ids);
}
