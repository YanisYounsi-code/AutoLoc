package fr.ece.autoloc.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "maintenance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Maintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMaintenance;

    @Column(nullable = false)
    private LocalDate dateDebut;

    // Vide tant que la maintenance est en cours
    private LocalDate dateFin;

    @Column(length = 255)
    private String description;

    // Plusieurs maintenances -> un vehicule
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicule_id", nullable = false)
    private Vehicule vehicule;
}
