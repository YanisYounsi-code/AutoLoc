package fr.ece.autoloc.web.controller;

import fr.ece.autoloc.service.ReservationService;
import fr.ece.autoloc.web.dto.ReservationRequest;
import fr.ece.autoloc.web.dto.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Locations de l'utilisateur connecte : role USER (voir SecurityConfig). */
@RestController
@RequestMapping("/api/locations")
@Tag(name = "2. Mes locations (USER)")
public class LocationApiController {

    private final ReservationService reservationService;

    public LocationApiController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    @Operation(summary = "Mes locations")
    public List<ReservationResponse> mesLocations(Authentication authentication) {
        // authentication.getName() = l'e-mail du compte connecte
        return reservationService.mesLocations(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Louer un véhicule (la demande est EN_ATTENTE)")
    public ReservationResponse louer(Authentication authentication, @Valid @RequestBody ReservationRequest requete) {
        return reservationService.louer(authentication.getName(), requete);
    }

    @PostMapping("/{id}/annulation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Annuler une de mes locations (avant le jour du départ)")
    public void annuler(Authentication authentication, @PathVariable Long id) {
        reservationService.annulerParUtilisateur(authentication.getName(), id);
    }
}
