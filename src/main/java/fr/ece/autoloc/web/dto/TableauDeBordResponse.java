package fr.ece.autoloc.web.dto;

/** Les chiffres affiches en haut de la page d'administration. */
public record TableauDeBordResponse(
        long vehiculesDisponibles,
        long vehiculesLoues,
        long vehiculesEnMaintenance,
        long locationsEnAttente) {
}
