package fr.ece.autoloc.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Workshop 2, challenge Spring Security : trois routes pour verifier les regles d'acces dans Postman.
 *
 *   Requete                  Anonyme  USER  ADMIN
 *   GET /api/public/ping       200     200   200
 *   GET /api/hello             401     200   200
 *   GET /api/admin/report      401     403   200
 */
@RestController
@Tag(name = "0. Démo sécurité (Workshop 2)")
public class SecurityDemoController {

    @GetMapping("/api/public/ping")
    @Operation(summary = "Route publique : accessible sans être connecté")
    public String publicPing() {
        return "Public endpoint is reachable";
    }

    @GetMapping("/api/hello")
    @Operation(summary = "Route authentifiée : USER ou ADMIN")
    public String hello() {
        return "Hello, authenticated user";
    }

    @GetMapping("/api/admin/report")
    @Operation(summary = "Route ADMIN uniquement (USER reçoit 403)")
    public String adminReport() {
        return "Confidential admin report";
    }
}
