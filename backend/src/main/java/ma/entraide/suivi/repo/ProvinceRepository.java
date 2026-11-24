package ma.entraide.suivi.repo;

import ma.entraide.suivi.domain.Province;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProvinceRepository extends JpaRepository<Province, Long> {
    Optional<Province> findByCode(String code);
    List<Province> findByRegionId(Long regionId);
}
