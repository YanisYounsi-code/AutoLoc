package fr.ece.autoloc.web.dto;

import fr.ece.autoloc.domain.CategorieVehicule;
import fr.ece.autoloc.domain.Client;
import fr.ece.autoloc.domain.Contrat;
import fr.ece.autoloc.domain.Reservation;
import fr.ece.autoloc.domain.StatutReservation;
import fr.ece.autoloc.domain.Vehicule;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Une location telle qu'affichee.
 * montant : montant du contrat s'il existe, sinon estimation tarif x jours.
 * A appeler DANS la transaction, sur une reservation chargee avec client, vehicule, agence et contrat.
 */
public record ReservationResponse(
        Long id,
        LocalDate dateDebut,
        LocalDate dateFin,
        long nbJours,
        StatutReservation statut,
        Long vehiculeId,
        String vehiculeLibelle,
        CategorieVehicule vehiculeCategorie,
        String immatriculation,
        String agenceVille,
        String clientNom,
        String clientEmail,
        BigDecimal montant,
        Long contratId) {

    public static ReservationResponse from(Reservation r) {
        Contrat contrat = r.getContrat();
        Vehicule v = r.getVehicule();
        Client c = r.getClient();
        BigDecimal montant = contrat != null ? contrat.getMontantTotal() : r.montantCalcule();
        return new ReservationResponse(
                r.getIdReservation(), r.getDateDebut(), r.getDateFin(), r.nombreDeJours(), r.getStatut(),
                v.getIdVehicule(), v.getMarque() + " " + v.getModele(), v.getCategorie(), v.getImmatriculation(),
                v.getAgence().getVille(),
                c.getPrenom() + " " + c.getNom(), c.getEmail(),
                montant,
                contrat != null ? contrat.getIdContrat() : null);
    }

    public String statutLibelle() {
        return statut.libelle();
    }

    /** Classe CSS de la pastille de statut. */
    public String statutClasse() {
        return switch (statut) {
            case EN_ATTENTE -> "attente";
            case CONFIRMEE -> "ok";
            case ANNULEE -> "ko";
            case TERMINEE -> "neutre";
        };
    }

    /** Tests de statut utilises par les pages (plus lisibles qu'une comparaison de texte dans le template). */
    public boolean enAttente() {
        return statut == StatutReservation.EN_ATTENTE;
    }

    public boolean confirmee() {
        return statut == StatutReservation.CONFIRMEE;
    }

    /** En attente ou confirmee : la location peut encore etre annulee. */
    public boolean enCours() {
        return enAttente() || confirmee();
    }

    /** L'utilisateur peut annuler une location en attente ou confirmee, jusqu'a la veille du depart. */
    public boolean annulablePar(LocalDate aujourdhui) {
        return enCours() && dateDebut.isAfter(aujourdhui);
    }
}
