package ma.entraide.suivi.repo;

import ma.entraide.suivi.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> { }
