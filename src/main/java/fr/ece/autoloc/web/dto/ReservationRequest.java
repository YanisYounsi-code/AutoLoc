package fr.ece.autoloc.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReservationRequest(
        @NotNull(message = "Le véhicule est obligatoire") Long vehiculeId,
        @NotNull(message = "La date de début est obligatoire") LocalDate dateDebut,
        @NotNull(message = "La date de fin est obligatoire") LocalDate dateFin) {
}
