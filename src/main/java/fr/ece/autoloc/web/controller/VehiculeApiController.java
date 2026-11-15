package fr.ece.autoloc.web.controller;

import fr.ece.autoloc.domain.CategorieVehicule;
import fr.ece.autoloc.service.VehiculeService;
import fr.ece.autoloc.web.dto.PageResponse;
import fr.ece.autoloc.web.dto.VehiculeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** API REST : renvoie du JSON (alors que les controleurs du package "page" renvoient des pages HTML). */
@RestController
@RequestMapping("/api/vehicules")
@Tag(name = "1. Véhicules")
public class VehiculeApiController {

    private final VehiculeService vehiculeService;

    public VehiculeApiController(VehiculeService vehiculeService) {
        this.vehiculeService = vehiculeService;
    }

    @GetMapping
    @Operation(summary = "Véhicules libres sur une période (9 par page)")
    public PageResponse<VehiculeResponse> disponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) CategorieVehicule categorie,
            @RequestParam(defaultValue = "0") int page) {
        return PageResponse.of(vehiculeService.disponibles(categorie, debut, fin, page));
    }
}
