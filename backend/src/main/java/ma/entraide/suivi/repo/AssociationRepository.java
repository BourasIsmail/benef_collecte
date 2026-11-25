package ma.entraide.suivi.repo;

import ma.entraide.suivi.domain.Association;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AssociationRepository extends JpaRepository<Association, Long> {
    boolean existsByProvinceIdAndNomAr(Long provinceId, String nomAr);
    boolean existsByProvinceIdAndNomArAndIdNot(Long provinceId, String nomAr, Long id);

    @Query("select a from Association a join fetch a.province p where (:all = true or p.id in :ids) order by p.nomFr, a.nomAr")
    List<Association> findScoped(@Param("all") boolean all, @Param("ids") Collection<Long> ids);

    @Query("select a from Association a join fetch a.province where a.id = :id")
    java.util.Optional<Association> findWithProvince(@Param("id") Long id);

    @Query("select e.association.id, count(e) from Etablissement e where e.actif = true and (:all = true or e.association.province.id in :ids) group by e.association.id")
    List<Object[]> countEtablissements(@Param("all") boolean all, @Param("ids") Collection<Long> ids);

    @Query("select e.association.id, count(b) from Beneficiary b join b.etablissement e where b.statut = 'ACTIF' and (:all = true or e.association.province.id in :ids) group by e.association.id")
    List<Object[]> countBeneficiaries(@Param("all") boolean all, @Param("ids") Collection<Long> ids);
}
