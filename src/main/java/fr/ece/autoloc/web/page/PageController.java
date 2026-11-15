package fr.ece.autoloc.web.page;

import fr.ece.autoloc.domain.CategorieVehicule;
import fr.ece.autoloc.service.ReservationService;
import fr.ece.autoloc.service.VehiculeService;
import fr.ece.autoloc.web.dto.ReservationRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pages web de l'utilisateur, generees cote serveur avec Thymeleaf.
 * Une methode renvoie le NOM d'un template (src/main/resources/templates/xxx.html) ;
 * les donnees passent par le Model. Aucun JavaScript : chaque bouton est un formulaire.
 */
@Controller
public class PageController {

    private final VehiculeService vehiculeService;
    private final ReservationService reservationService;
    private final Clock clock;

    public PageController(VehiculeService vehiculeService, ReservationService reservationService, Clock clock) {
        this.vehiculeService = vehiculeService;
        this.reservationService = reservationService;
        this.clock = clock;
    }

    /** Apres connexion : l'admin arrive sur son tableau de bord, l'utilisateur sur les vehicules. */
    @GetMapping("/")
    public String accueil(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));
        return admin ? "redirect:/admin" : "redirect:/vehicules";
    }

    @GetMapping("/connexion")
    public String connexion() {
        return "connexion";
    }

    /** Liste des vehicules libres sur la periode choisie (par defaut : demain, pour 3 jours). */
    @GetMapping("/vehicules")
    public String vehicules(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                            @RequestParam(required = false) CategorieVehicule categorie,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        LocalDate demain = LocalDate.now(clock).plusDays(1);
        LocalDate dateDebut = debut != null ? debut : demain;
        LocalDate dateFin = fin != null && !fin.isBefore(dateDebut) ? fin : dateDebut.plusDays(2);
        model.addAttribute("debut", dateDebut);
        model.addAttribute("fin", dateFin);
        model.addAttribute("categorie", categorie);
        model.addAttribute("categories", CategorieVehicule.values());
        model.addAttribute("vehicules", vehiculeService.disponibles(categorie, dateDebut, dateFin, page));
        model.addAttribute("nbJours", ChronoUnit.DAYS.between(dateDebut, dateFin) + 1);
        return "vehicules";
    }

    @PostMapping("/louer")
    public String louer(Authentication authentication,
                        @RequestParam Long vehiculeId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
                        RedirectAttributes redirection) {
        try {
            reservationService.louer(authentication.getName(), new ReservationRequest(vehiculeId, dateDebut, dateFin));
            redirection.addFlashAttribute("succes", "Demande de location envoyée : l'agence va la confirmer.");
            return "redirect:/mes-locations";
        } catch (RuntimeException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/vehicules?debut=" + dateDebut + "&fin=" + dateFin;
        }
    }

    @GetMapping("/mes-locations")
    public String mesLocations(Authentication authentication, Model model) {
        model.addAttribute("locations", reservationService.mesLocations(authentication.getName()));
        model.addAttribute("aujourdhui", LocalDate.now(clock));
        return "mes-locations";
    }

    @PostMapping("/mes-locations/{id}/annulation")
    public String annuler(Authentication authentication, @PathVariable Long id, RedirectAttributes redirection) {
        try {
            reservationService.annulerParUtilisateur(authentication.getName(), id);
            redirection.addFlashAttribute("succes", "Location annulée.");
        } catch (RuntimeException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/mes-locations";
    }
}
