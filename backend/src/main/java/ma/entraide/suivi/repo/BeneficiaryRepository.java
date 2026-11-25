package ma.entraide.suivi.repo;

import ma.entraide.suivi.domain.Beneficiary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    String JOINS = " from Beneficiary b join fetch b.etablissement e join fetch e.association a join fetch a.province p";
    String FILTER = " where b.statut = 'ACTIF' and (:all = true or p.id in :ids)"
            + " and (:etabId is null or e.id = :etabId) and (:assoId is null or a.id = :assoId) and (:provId is null or p.id = :provId)"
            + " and (:catId is null or e.category.id = :catId)"
            + " and (:sexe is null or b.sexe = :sexe) and (:q is null or b.searchText like concat('%', :q, '%'))";

    @Query(value = "select b" + JOINS + FILTER + " order by b.createdAt desc, b.id desc",
           countQuery = "select count(b) from Beneficiary b join b.etablissement e join e.association a join a.province p" + FILTER)
    Page<Beneficiary> search(@Param("all") boolean all, @Param("ids") Collection<Long> ids, @Param("etabId") Long etabId,
                             @Param("catId") Long catId, @Param("assoId") Long assoId, @Param("provId") Long provId, @Param("sexe") String sexe,
                             @Param("q") String q, Pageable pageable);

    @Query("select b" + JOINS + FILTER + " order by p.nomFr, a.nomAr, e.nomAr, b.nomFr, b.prenomFr")
    List<Beneficiary> searchAll(@Param("all") boolean all, @Param("ids") Collection<Long> ids, @Param("etabId") Long etabId,
                                @Param("catId") Long catId, @Param("assoId") Long assoId, @Param("provId") Long provId, @Param("sexe") String sexe,
                                @Param("q") String q);

    @Query("select b" + JOINS + " where b.statut = 'ACTIF' and b.dedupKey in :keys")
    List<Beneficiary> findActiveByDedupKeys(@Param("keys") Collection<String> keys);

    @Query("select b" + JOINS + " where b.statut = 'ACTIF' and b.dateNaissance = :dob")
    List<Beneficiary> findActiveByDob(@Param("dob") LocalDate dob);

    boolean existsByCinAndIdNot(String cin, Long id);
    boolean existsByCin(String cin);
    long countByEtablissementId(Long etablissementId);
}
