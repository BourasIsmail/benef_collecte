package ma.entraide.suivi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "audit_log") @Getter @Setter
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private LocalDateTime at = LocalDateTime.now();
    @Column(name = "user_email") private String userEmail;
    private String action;
    private String entity;
    @Column(name = "entity_id") private Long entityId;
    @Column(columnDefinition = "TEXT") private String details;
}
