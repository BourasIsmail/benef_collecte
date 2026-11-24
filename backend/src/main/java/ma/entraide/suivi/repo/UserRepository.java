package ma.entraide.suivi.repo;

import ma.entraide.suivi.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    @Query("select u from AppUser u left join fetch u.province p left join fetch p.region left join fetch u.region where u.id = :id")
    Optional<AppUser> findWithScopeById(@Param("id") Long id);

    @Query("select u from AppUser u left join fetch u.province p left join fetch u.region r order by u.role, u.email")
    List<AppUser> findAllWithScope();
}
