package fr.ece.autoloc.service;

import fr.ece.autoloc.domain.Agence;
import fr.ece.autoloc.domain.CategorieVehicule;
import fr.ece.autoloc.domain.StatutReservation;
import fr.ece.autoloc.domain.StatutVehicule;
import fr.ece.autoloc.domain.Vehicule;
import fr.ece.autoloc.exception.RegleMetierException;
import fr.ece.autoloc.exception.RequeteInvalideException;
import fr.ece.autoloc.exception.RessourceIntrouvableException;
import fr.ece.autoloc.repository.AgenceRepository;
import fr.ece.autoloc.repository.VehiculeRepository;
import fr.ece.autoloc.web.dto.AgenceResponse;
import fr.ece.autoloc.web.dto.VehiculeRequest;
import fr.ece.autoloc.web.dto.VehiculeResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class VehiculeService {

    static final List<StatutReservation> STATUTS_ACTIFS =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.CONFIRMEE);

    private final VehiculeRepository vehiculeRepository;
    private final AgenceRepository agenceRepository;

    public VehiculeService(VehiculeRepository vehiculeRepository, AgenceRepository agenceRepository) {
        this.vehiculeRepository = vehiculeRepository;
        this.agenceRepository = agenceRepository;
    }

    /** Vehicules libres sur la periode, du moins cher au plus cher, 9 par page. */
    @Transactional(readOnly = true)
    public Page<VehiculeResponse> disponibles(CategorieVehicule categorie, LocalDate debut, LocalDate fin, int page) {
        if (fin.isBefore(debut)) {
            throw new RequeteInvalideException("La date de fin doit être après la date de début.");
        }
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), 9,
                Sort.by("tarifJournalier").and(Sort.by("idVehicule")));
        return vehiculeRepository.rechercherDisponibles(StatutVehicule.MAINTENANCE, categorie, debut, fin,
                STATUTS_ACTIFS, pageRequest).map(VehiculeResponse::from);
    }

    /** Toute la flotte, 10 vehicules par page (administration). */
    @Transactional(readOnly = true)
    public Page<VehiculeResponse> flotte(int page) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), 10,
                Sort.by("marque").and(Sort.by("modele")).and(Sort.by("idVehicule")));
        return vehiculeRepository.findAllAvecAgence(pageRequest).map(VehiculeResponse::from);
    }

    public VehiculeResponse ajouter(VehiculeRequest requete) {
        String immatriculation = requete.immatriculation().trim().toUpperCase(Locale.ROOT);
        if (vehiculeRepository.existsByImmatriculationIgnoreCase(immatriculation)) {
            throw new RegleMetierException("Un véhicule porte déjà l'immatriculation " + immatriculation + ".");
        }
        Agence agence = agenceRepository.findById(requete.agenceId())
                .orElseThrow(() -> new RessourceIntrouvableException("Agence introuvable."));
        Vehicule vehicule = new Vehicule();
        vehicule.setImmatriculation(immatriculation);
        vehicule.setMarque(requete.marque().trim());
        vehicule.setModele(requete.modele().trim());
        vehicule.setCategorie(requete.categorie());
        vehicule.setTarifJournalier(requete.tarifJournalier());
        vehicule.setStatut(StatutVehicule.DISPONIBLE);
        vehicule.setAgence(agence);
        return VehiculeResponse.from(vehiculeRepository.save(vehicule));
    }

    /** Met un vehicule en maintenance (il sort du catalogue) ou le remet a disposition. */
    public VehiculeResponse changerStatut(Long id, StatutVehicule statut) {
        Vehicule vehicule = vehiculeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Véhicule introuvable."));
        if (statut == StatutVehicule.LOUE) {
            throw new RegleMetierException("Le statut « Loué » est donné automatiquement à la confirmation d'une location.");
        }
        vehicule.setStatut(statut);
        // Pas de save() : l'entite est "managed", Hibernate enregistre le changement a la fin de la transaction.
        return VehiculeResponse.from(vehicule);
    }

    /** Liste des agences : lue a chaque affichage du formulaire, modifiee rarement, donc mise en cache. */
    @Cacheable("agences")
    @Transactional(readOnly = true)
    public List<AgenceResponse> agences() {
        return agenceRepository.findAllByOrderByVilleAscNomAsc().stream().map(AgenceResponse::from).toList();
    }
}
