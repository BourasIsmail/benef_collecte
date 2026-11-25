package ma.entraide.suivi.repo;

import ma.entraide.suivi.domain.Etablissement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EtablissementRepository extends JpaRepository<Etablissement, Long> {
    @Query("select e from Etablissement e join fetch e.association a join fetch e.province p join fetch e.category c "
            + "where (:all = true or p.id in :ids) order by p.nomFr, a.nomAr, e.nomAr")
    List<Etablissement> findScoped(@Param("all") boolean all, @Param("ids") Collection<Long> ids);

    @Query("select e from Etablissement e join fetch e.association join fetch e.province join fetch e.category where e.id = :id")
    java.util.Optional<Etablissement> findWithRefs(@Param("id") Long id);

    boolean existsByAssociationIdAndProvinceIdAndNomAr(Long associationId, Long provinceId, String nomAr);
    boolean existsByAssociationIdAndProvinceIdAndNomArAndIdNot(Long associationId, Long provinceId, String nomAr, Long id);
    long countByAssociationId(Long associationId);

    @Query("select b.etablissement.id, count(b) from Beneficiary b where b.statut = 'ACTIF' group by b.etablissement.id")
    List<Object[]> countActiveBeneficiaries();
}
