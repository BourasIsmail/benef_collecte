package ma.entraide.suivi.repo;

import ma.entraide.suivi.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {
    java.util.Optional<Region> findByCode(String code);
}
