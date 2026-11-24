package ma.entraide.suivi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "users") @Getter @Setter
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String email;
    @Column(name = "password_hash") private String passwordHash;
    @Enumerated(EnumType.STRING) private Role role;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "province_id") private Province province;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "region_id") private Region region;
    private boolean enabled = true;
    @Column(name = "must_change_password") private boolean mustChangePassword = true;
    @Column(name = "failed_attempts") private int failedAttempts;
    @Column(name = "locked_until") private LocalDateTime lockedUntil;
    @Column(name = "last_login") private LocalDateTime lastLogin;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
}
