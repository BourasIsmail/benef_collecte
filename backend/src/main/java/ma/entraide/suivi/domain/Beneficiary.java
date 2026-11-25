package ma.entraide.suivi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "beneficiaires") @Getter @Setter
public class Beneficiary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "etablissement_id") private Etablissement etablissement;
    @Column(name = "nom_ar") private String nomAr;
    @Column(name = "prenom_ar") private String prenomAr;
    @Column(name = "nom_fr") private String nomFr;
    @Column(name = "prenom_fr") private String prenomFr;
    @Column(name = "date_naissance") private LocalDate dateNaissance;
    private String sexe;
    private String cin;
    @Column(name = "dedup_key") private String dedupKey;
    @Column(name = "search_text") private String searchText;
    private String statut = "ACTIF";
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "created_by") private String createdBy;
    @Column(name = "updated_by") private String updatedBy;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
