package ma.entraide.suivi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "provinces") @Getter @Setter
public class Province {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "region_id") private Region region;
    @Column(name = "nom_fr") private String nomFr;
    @Column(name = "nom_ar") private String nomAr;
}
