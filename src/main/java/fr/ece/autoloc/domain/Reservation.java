package fr.ece.autoloc.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "reservation", indexes = {
        // Index compose : la verification "ce vehicule est-il deja reserve sur ces dates ?"
        // est la requete la plus frequente de l'application.
        @Index(name = "idx_reservation_vehicule_dates", columnList = "vehicule_id, date_debut, date_fin"),
        @Index(name = "idx_reservation_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReservation;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutReservation statut;

    // Plusieurs reservations -> un client
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // Plusieurs reservations -> un vehicule
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicule_id", nullable = false)
    private Vehicule vehicule;

    // Cote inverse du 1 -> 0..1 (la cle est dans contrat). ALL : le contrat suit le cycle de vie
    // de sa reservation (enregistre et supprime avec elle).
    // Attention : Hibernate ne sait pas charger en LAZY le cote inverse d'un @OneToOne (il doit lire
    // la table contrat pour savoir s'il vaut null). Les requetes font donc "left join fetch r.contrat".
    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Contrat contrat;

    /** Rattache le contrat en tenant a jour les deux cotes ; la cascade l'enregistrera avec la reservation. */
    public void attacherContrat(Contrat nouveauContrat) {
        this.contrat = nouveauContrat;
        nouveauContrat.setReservation(this);
    }

    /** Nombre de jours factures : du 10 au 12 = 3 jours. */
    public long nombreDeJours() {
        return ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;
    }

    /** Tarif journalier du vehicule x nombre de jours. */
    public BigDecimal montantCalcule() {
        return vehicule.getTarifJournalier().multiply(BigDecimal.valueOf(nombreDeJours()));
    }
}
