package ma.entraide.suivi.repo;

import ma.entraide.suivi.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("select l from AuditLog l where (:action is null or l.action = :action) "
            + "and (:user is null or lower(l.userEmail) like lower(concat('%', :user, '%'))) "
            + "and l.at >= :from and l.at < :to order by l.id desc")
    Page<AuditLog> search(@Param("action") String action, @Param("user") String user,
                          @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("select distinct l.action from AuditLog l order by l.action")
    List<String> actions();
}
