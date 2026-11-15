package fr.ece.autoloc.web.page;

import fr.ece.autoloc.domain.CategorieVehicule;
import fr.ece.autoloc.domain.StatutReservation;
import fr.ece.autoloc.domain.StatutVehicule;
import fr.ece.autoloc.service.AdminService;
import fr.ece.autoloc.service.ReservationService;
import fr.ece.autoloc.service.VehiculeService;
import fr.ece.autoloc.web.dto.VehiculeRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Page d'administration : reservee au role ADMIN (voir SecurityConfig). */
@Controller
@RequestMapping("/admin")
public class AdminPageController {

    private final AdminService adminService;
    private final ReservationService reservationService;
    private final VehiculeService vehiculeService;

    public AdminPageController(AdminService adminService, ReservationService reservationService,
                               VehiculeService vehiculeService) {
        this.adminService = adminService;
        this.reservationService = reservationService;
        this.vehiculeService = vehiculeService;
    }

    @GetMapping
    public String admin(@RequestParam(required = false) StatutReservation statut,
                        @RequestParam(defaultValue = "0") int pageLocations,
                        @RequestParam(defaultValue = "0") int pageVehicules,
                        Model model) {
        model.addAttribute("chiffres", adminService.tableauDeBord());
        model.addAttribute("statut", statut);
        model.addAttribute("statuts", StatutReservation.values());
        model.addAttribute("locations", reservationService.lister(statut, pageLocations));
        model.addAttribute("vehicules", vehiculeService.flotte(pageVehicules));
        model.addAttribute("agences", vehiculeService.agences());
        model.addAttribute("categories", CategorieVehicule.values());
        return "admin";
    }

    @PostMapping("/locations/{id}/confirmation")
    public String confirmer(@PathVariable Long id, RedirectAttributes redirection) {
        return executer(() -> reservationService.confirmer(id), "Location confirmée : contrat créé.", redirection);
    }

    @PostMapping("/locations/{id}/annulation")
    public String annuler(@PathVariable Long id, RedirectAttributes redirection) {
        return executer(() -> reservationService.annulerParAdmin(id), "Location annulée.", redirection);
    }

    @PostMapping("/locations/{id}/fin")
    public String terminer(@PathVariable Long id, RedirectAttributes redirection) {
        return executer(() -> reservationService.terminer(id), "Location terminée : véhicule disponible.", redirection);
    }

    @PostMapping("/vehicules")
    public String ajouterVehicule(@Valid @ModelAttribute VehiculeRequest vehicule, BindingResult validation,
                                  RedirectAttributes redirection) {
        if (validation.hasErrors()) {
            redirection.addFlashAttribute("erreur", validation.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin";
        }
        return executer(() -> vehiculeService.ajouter(vehicule), "Véhicule ajouté à la flotte.", redirection);
    }

    @PostMapping("/vehicules/{id}/statut")
    public String changerStatut(@PathVariable Long id, @RequestParam StatutVehicule statut,
                                RedirectAttributes redirection) {
        return executer(() -> vehiculeService.changerStatut(id, statut), "Statut du véhicule mis à jour.", redirection);
    }

    /** Execute l'action, puis revient sur la page avec un message de succes ou d'erreur. */
    private String executer(Runnable action, String messageSucces, RedirectAttributes redirection) {
        try {
            action.run();
            redirection.addFlashAttribute("succes", messageSucces);
        } catch (RuntimeException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin";
    }
}
