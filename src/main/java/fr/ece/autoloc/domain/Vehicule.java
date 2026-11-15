package fr.ece.autoloc.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "vehicule", indexes = {
        // Index : accelerent les filtres du catalogue (statut, categorie)
        @Index(name = "idx_vehicule_statut", columnList = "statut"),
        @Index(name = "idx_vehicule_categorie", columnList = "categorie")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idVehicule;

    @Column(nullable = false, unique = true, length = 20)
    private String immatriculation;

    @Column(nullable = false, length = 50)
    private String marque;

    @Column(nullable = false, length = 50)
    private String modele;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, length = 20)
    private CategorieVehicule categorie;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifJournalier;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutVehicule statut;

    // Plusieurs vehicules -> une agence. LAZY : l'agence n'est chargee que si on y accede.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;

    // Vehicule * <-> * Equipement : le vehicule est PROPRIETAIRE de la table de jointure vehicule_equipement.
    // Set (pas de doublon) et pas de cascade : un equipement est partage entre plusieurs vehicules.
    // @BatchSize : les equipements d'une page de vehicules sont charges en 1 requete au lieu d'une par vehicule.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "vehicule_equipement",
            joinColumns = @JoinColumn(name = "vehicule_id"),
            inverseJoinColumns = @JoinColumn(name = "equipement_id"))
    @BatchSize(size = 50)
    private Set<Equipement> equipements = new HashSet<>();

    // Vehicule 1 -> * Maintenance. PERSIST : une maintenance ajoutee au vehicule est enregistree avec lui.
    // Pas de REMOVE : l'historique d'entretien est conserve.
    @OneToMany(mappedBy = "vehicule", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Maintenance> maintenances = new ArrayList<>();

    /** Ajoute une maintenance en tenant a jour les deux cotes de la relation. */
    public void ajouterMaintenance(Maintenance maintenance) {
        maintenances.add(maintenance);
        maintenance.setVehicule(this);
    }
}
