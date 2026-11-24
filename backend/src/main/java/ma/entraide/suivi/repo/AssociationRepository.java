package ma.entraide.suivi.repo;

import ma.entraide.suivi.domain.Association;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AssociationRepository extends JpaRepository<Association, Long> {
    @Query("select a from Association a join fetch a.province p where (:all = true or p.id in :ids) order by p.nomFr, a.nomAr")
    List<Association> findScoped(@Param("all") boolean all, @Param("ids") Collection<Long> ids);

    boolean existsByProvinceIdAndNomArAndIdNot(Long provinceId, String nomAr, Long id);
    boolean existsByProvinceIdAndNomAr(Long provinceId, String nomAr);

    @Query("select b.association.id, count(b) from Beneficiary b where b.statut = 'ACTIF' group by b.association.id")
    List<Object[]> countActiveBeneficiaries();
}
