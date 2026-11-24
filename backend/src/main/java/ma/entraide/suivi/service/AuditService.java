package ma.entraide.suivi.service;

import ma.entraide.suivi.domain.AuditLog;
import ma.entraide.suivi.repo.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    public void log(String userEmail, String action, String entity, Long entityId, String details) {
        AuditLog l = new AuditLog();
        l.setUserEmail(userEmail);
        l.setAction(action);
        l.setEntity(entity);
        l.setEntityId(entityId);
        l.setDetails(details);
        repo.save(l);
    }
}
