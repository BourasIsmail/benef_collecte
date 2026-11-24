package ma.entraide.suivi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "regions") @Getter @Setter
public class Region {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String code;
    @Column(name = "nom_fr") private String nomFr;
    @Column(name = "nom_ar") private String nomAr;
}
