package fr.ece.autoloc.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contrat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idContrat;

    @Column(nullable = false)
    private LocalDate dateSignature;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTotal;

    @Column(nullable = false)
    private boolean valide;

    // Reservation 1 -> 0..1 Contrat. Le contrat est le cote PROPRIETAIRE : il porte la cle etrangere
    // reservation_id (unique = true : une reservation n'a jamais deux contrats).
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    // Contrat 1 -> * Paiement : COMPOSITION, un paiement n'existe pas sans son contrat.
    // ALL : enregistrer / supprimer le contrat enregistre / supprime ses paiements.
    // orphanRemoval : retirer un paiement de la liste le supprime de la base.
    @OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Paiement> paiements = new ArrayList<>();

    /** Ajoute un paiement en tenant a jour LES DEUX cotes de la relation. */
    public void ajouterPaiement(Paiement paiement) {
        paiements.add(paiement);
        paiement.setContrat(this);
    }
}
