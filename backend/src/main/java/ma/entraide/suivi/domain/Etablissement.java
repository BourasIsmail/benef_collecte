package ma.entraide.suivi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "etablissements") @Getter @Setter
public class Etablissement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "association_id") private Association association;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "province_id") private Province province;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id") private Category category;
    @Column(name = "nom_ar") private String nomAr;
    @Column(name = "type_etablissement") private String typeEtablissement;
    @Column(name = "numero_licence") private String numeroLicence;
    private boolean actif = true;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
