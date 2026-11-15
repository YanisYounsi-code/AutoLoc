package fr.ece.autoloc.web.dto;

import fr.ece.autoloc.domain.Agence;
import fr.ece.autoloc.domain.CategorieVehicule;
import fr.ece.autoloc.domain.Equipement;
import fr.ece.autoloc.domain.StatutVehicule;
import fr.ece.autoloc.domain.Vehicule;

import java.math.BigDecimal;
import java.util.List;

/** Ce que l'application montre d'un vehicule : jamais l'entite JPA elle-meme. */
public record VehiculeResponse(
        Long id,
        String immatriculation,
        String marque,
        String modele,
        CategorieVehicule categorie,
        BigDecimal tarifJournalier,
        StatutVehicule statut,
        String agenceVille,
        List<String> equipements) {

    public static VehiculeResponse from(Vehicule v) {
        Agence agence = v.getAgence();
        List<String> equipements = v.getEquipements().stream().map(Equipement::getLibelle).sorted().toList();
        return new VehiculeResponse(v.getIdVehicule(), v.getImmatriculation(), v.getMarque(), v.getModele(),
                v.getCategorie(), v.getTarifJournalier(), v.getStatut(), agence.getVille(), equipements);
    }

    /** Prix total pour une location de "jours" jours (affiche sur la carte du vehicule). */
    public BigDecimal montantPour(long jours) {
        return tarifJournalier.multiply(BigDecimal.valueOf(jours));
    }

    /** Tests de statut utilises par la page d'administration. */
    public boolean disponible() {
        return statut == StatutVehicule.DISPONIBLE;
    }

    public boolean enMaintenance() {
        return statut == StatutVehicule.MAINTENANCE;
    }

    public boolean loue() {
        return statut == StatutVehicule.LOUE;
    }

    public String categorieLibelle() {
        return categorie.libelle();
    }

    public String statutLibelle() {
        return statut.libelle();
    }
}
