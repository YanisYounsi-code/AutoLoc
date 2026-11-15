package fr.ece.autoloc.web.dto;

import fr.ece.autoloc.domain.CategorieVehicule;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Ce que l'application accepte pour ajouter un vehicule (formulaire admin ou API). */
public record VehiculeRequest(
        @NotBlank(message = "L'immatriculation est obligatoire") @Size(max = 20) String immatriculation,
        @NotBlank(message = "La marque est obligatoire") @Size(max = 50) String marque,
        @NotBlank(message = "Le modèle est obligatoire") @Size(max = 50) String modele,
        @NotNull(message = "La catégorie est obligatoire") CategorieVehicule categorie,
        @NotNull(message = "Le tarif est obligatoire")
        @DecimalMin(value = "1.00", message = "Le tarif doit être d'au moins 1 euro")
        @Digits(integer = 8, fraction = 2, message = "Le tarif a au plus 2 décimales") BigDecimal tarifJournalier,
        @NotNull(message = "L'agence est obligatoire") Long agenceId) {
}
