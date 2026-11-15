package fr.ece.autoloc.web.controller;

import fr.ece.autoloc.domain.StatutReservation;
import fr.ece.autoloc.service.AdminService;
import fr.ece.autoloc.service.ReservationService;
import fr.ece.autoloc.service.VehiculeService;
import fr.ece.autoloc.web.dto.PageResponse;
import fr.ece.autoloc.web.dto.ReservationResponse;
import fr.ece.autoloc.web.dto.TableauDeBordResponse;
import fr.ece.autoloc.web.dto.VehiculeRequest;
import fr.ece.autoloc.web.dto.VehiculeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Administration : role ADMIN (voir SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "3. Administration (ADMIN)")
public class AdminApiController {

    private final AdminService adminService;
    private final ReservationService reservationService;
    private final VehiculeService vehiculeService;

    public AdminApiController(AdminService adminService, ReservationService reservationService,
                              VehiculeService vehiculeService) {
        this.adminService = adminService;
        this.reservationService = reservationService;
        this.vehiculeService = vehiculeService;
    }

    @GetMapping("/tableau-de-bord")
    @Operation(summary = "Chiffres clés : véhicules par statut, locations en attente")
    public TableauDeBordResponse tableauDeBord() {
        return adminService.tableauDeBord();
    }

    @GetMapping("/locations")
    @Operation(summary = "Locations paginées (10 par page), filtrables par statut")
    public PageResponse<ReservationResponse> locations(@RequestParam(required = false) StatutReservation statut,
                                                       @RequestParam(defaultValue = "0") int page) {
        return PageResponse.of(reservationService.lister(statut, page));
    }

    @GetMapping("/locations/non-optimise")
    @Operation(summary = "DÉMO PERFORMANCE : toutes les locations, sans pagination ni chargement groupé (problème N+1)")
    public List<ReservationResponse> locationsNonOptimise() {
        return reservationService.listerSansOptimisation();
    }

    @PostMapping("/locations/{id}/confirmation")
    @Operation(summary = "Confirmer une location : crée le contrat, le véhicule passe à LOUE")
    public ReservationResponse confirmer(@PathVariable Long id) {
        return reservationService.confirmer(id);
    }

    @PostMapping("/locations/{id}/annulation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Refuser ou annuler une location")
    public void annuler(@PathVariable Long id) {
        reservationService.annulerParAdmin(id);
    }

    @PostMapping("/locations/{id}/fin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Terminer une location : le véhicule redevient disponible")
    public void terminer(@PathVariable Long id) {
        reservationService.terminer(id);
    }

    @PostMapping("/vehicules")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajouter un véhicule")
    public VehiculeResponse ajouterVehicule(@Valid @RequestBody VehiculeRequest requete) {
        return vehiculeService.ajouter(requete);
    }
}
