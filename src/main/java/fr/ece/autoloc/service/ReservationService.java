package fr.ece.autoloc.service;

import fr.ece.autoloc.domain.Client;
import fr.ece.autoloc.domain.Contrat;
import fr.ece.autoloc.domain.ModePaiement;
import fr.ece.autoloc.domain.Paiement;
import fr.ece.autoloc.domain.Reservation;
import fr.ece.autoloc.domain.StatutReservation;
import fr.ece.autoloc.domain.StatutVehicule;
import fr.ece.autoloc.domain.Vehicule;
import fr.ece.autoloc.exception.RegleMetierException;
import fr.ece.autoloc.exception.RequeteInvalideException;
import fr.ece.autoloc.exception.RessourceIntrouvableException;
import fr.ece.autoloc.repository.ClientRepository;
import fr.ece.autoloc.repository.ReservationRepository;
import fr.ece.autoloc.repository.VehiculeRepository;
import fr.ece.autoloc.web.dto.ReservationRequest;
import fr.ece.autoloc.web.dto.ReservationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Cycle de vie d'une location :
 * EN_ATTENTE -> (l'admin confirme : contrat cree, vehicule LOUE) -> CONFIRMEE
 * -> (l'admin termine : paiement du solde, vehicule DISPONIBLE) -> TERMINEE.
 * Une location en attente ou confirmee peut etre annulee (par l'utilisateur avant le depart, ou par l'admin).
 */
@Service
@Transactional
public class ReservationService {

    private static final int DUREE_MAX_JOURS = 60;

    private final ReservationRepository reservationRepository;
    private final VehiculeRepository vehiculeRepository;
    private final ClientRepository clientRepository;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository,
                              VehiculeRepository vehiculeRepository,
                              ClientRepository clientRepository,
                              Clock clock) {
        this.reservationRepository = reservationRepository;
        this.vehiculeRepository = vehiculeRepository;
        this.clientRepository = clientRepository;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ utilisateur (USER)

    public ReservationResponse louer(String email, ReservationRequest requete) {
        Client client = clientConnecte(email);
        verifierPeriode(requete.dateDebut(), requete.dateFin());

        // Verrou sur la ligne du vehicule : deux personnes ne peuvent pas le louer en meme temps.
        Vehicule vehicule = vehiculeRepository.findPourMiseAJour(requete.vehiculeId())
                .orElseThrow(() -> new RessourceIntrouvableException("Véhicule introuvable."));
        if (vehicule.getStatut() == StatutVehicule.MAINTENANCE) {
            throw new RegleMetierException("Ce véhicule est en maintenance.");
        }
        if (reservationRepository.compterChevauchements(vehicule.getIdVehicule(), VehiculeService.STATUTS_ACTIFS,
                requete.dateDebut(), requete.dateFin(), null) > 0) {
            throw new RegleMetierException("Ce véhicule est déjà loué sur une partie de ces dates.");
        }

        Reservation reservation = new Reservation();
        reservation.setClient(client);
        reservation.setVehicule(vehicule);
        reservation.setDateDebut(requete.dateDebut());
        reservation.setDateFin(requete.dateFin());
        reservation.setStatut(StatutReservation.EN_ATTENTE);
        reservationRepository.save(reservation);
        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> mesLocations(String email) {
        Client client = clientConnecte(email);
        return reservationRepository.findDuClient(client.getIdClient()).stream()
                .map(ReservationResponse::from).toList();
    }

    public void annulerParUtilisateur(String email, Long idReservation) {
        Client client = clientConnecte(email);
        Reservation reservation = reservationRepository.findDetail(idReservation)
                // Viser la location d'un autre donne "introuvable" : on ne revele rien
                .filter(r -> r.getClient().getIdClient().equals(client.getIdClient()))
                .orElseThrow(() -> new RessourceIntrouvableException("Location introuvable."));
        if (!reservation.getDateDebut().isAfter(LocalDate.now(clock))) {
            throw new RegleMetierException("L'annulation n'est plus possible à partir du jour du départ.");
        }
        annuler(reservation);
    }

    // ------------------------------------------------------------------ administration (ADMIN)

    /** Une page de locations (10 par page), filtrable par statut. */
    @Transactional(readOnly = true)
    public Page<ReservationResponse> lister(StatutReservation statut, int page) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), 10,
                Sort.by(Sort.Direction.DESC, "dateDebut").and(Sort.by(Sort.Direction.DESC, "idReservation")));
        return reservationRepository.rechercher(statut, pageRequest).map(ReservationResponse::from);
    }

    /** Confirme la demande : le contrat est cree au montant tarif x jours, le vehicule passe a LOUE. */
    public ReservationResponse confirmer(Long idReservation) {
        Reservation reservation = reservationDetail(idReservation);
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new RegleMetierException("Seule une location en attente peut être confirmée.");
        }
        Vehicule vehicule = vehiculeRepository.findPourMiseAJour(reservation.getVehicule().getIdVehicule())
                .orElseThrow(() -> new RessourceIntrouvableException("Véhicule introuvable."));
        if (vehicule.getStatut() == StatutVehicule.MAINTENANCE) {
            throw new RegleMetierException("Le véhicule est en maintenance : confirmation impossible.");
        }
        reservation.setStatut(StatutReservation.CONFIRMEE);
        vehicule.setStatut(StatutVehicule.LOUE);

        Contrat contrat = new Contrat();
        contrat.setDateSignature(LocalDate.now(clock));
        contrat.setMontantTotal(reservation.montantCalcule());
        contrat.setValide(true);
        // Pas de contratRepository.save() : la reservation est "managed" et sa relation contrat est en
        // cascade ALL, donc Hibernate insere le contrat a la fin de la transaction.
        reservation.attacherContrat(contrat);
        reservationRepository.flush(); // force l'INSERT maintenant pour connaitre l'id du contrat
        return ReservationResponse.from(reservation);
    }

    /** Refus d'une demande ou annulation d'une location confirmee. */
    public void annulerParAdmin(Long idReservation) {
        annuler(reservationDetail(idReservation));
    }

    /**
     * Retour du vehicule : le solde est encaisse, la location est terminee et le vehicule redevient disponible.
     * Le paiement est enregistre par cascade (Contrat.paiements, cascade ALL) : aucun save() a ecrire.
     */
    public void terminer(Long idReservation) {
        Reservation reservation = reservationDetail(idReservation);
        if (reservation.getStatut() != StatutReservation.CONFIRMEE || reservation.getContrat() == null) {
            throw new RegleMetierException("Seule une location confirmée peut être terminée.");
        }
        Contrat contrat = reservation.getContrat();
        Paiement paiement = new Paiement();
        paiement.setMontant(contrat.getMontantTotal());
        paiement.setDatePaiement(LocalDate.now(clock));
        paiement.setModePaiement(ModePaiement.CARTE);
        contrat.ajouterPaiement(paiement);

        reservation.setStatut(StatutReservation.TERMINEE);
        liberer(reservation.getVehicule());
    }

    /**
     * VERSION VOLONTAIREMENT NON OPTIMISEE, gardee pour la demonstration de performance :
     * toutes les locations sans pagination, et des requetes SQL en plus pour chaque location
     * (contrat, client, vehicule, agence) : c'est le probleme "N+1".
     * Comparez avec lister(), qui fait 2 requetes au total (la page + le comptage).
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> listerSansOptimisation() {
        return reservationRepository.findAll(Sort.by(Sort.Direction.DESC, "dateDebut")).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    // ------------------------------------------------------------------ outils internes

    private void annuler(Reservation reservation) {
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE
                && reservation.getStatut() != StatutReservation.CONFIRMEE) {
            throw new RegleMetierException("Cette location ne peut plus être annulée.");
        }
        if (reservation.getStatut() == StatutReservation.CONFIRMEE) {
            liberer(reservation.getVehicule());
        }
        reservation.setStatut(StatutReservation.ANNULEE);
        if (reservation.getContrat() != null) {
            reservation.getContrat().setValide(false);
        }
    }

    private static void liberer(Vehicule vehicule) {
        if (vehicule.getStatut() == StatutVehicule.LOUE) {
            vehicule.setStatut(StatutVehicule.DISPONIBLE);
        }
    }

    private void verifierPeriode(LocalDate debut, LocalDate fin) {
        if (debut.isBefore(LocalDate.now(clock))) {
            throw new RequeteInvalideException("La date de début ne peut pas être dans le passé.");
        }
        if (fin.isBefore(debut)) {
            throw new RequeteInvalideException("La date de fin doit être après la date de début.");
        }
        if (debut.plusDays(DUREE_MAX_JOURS - 1L).isBefore(fin)) {
            throw new RequeteInvalideException("Une location dure au maximum " + DUREE_MAX_JOURS + " jours.");
        }
    }

    /** Le compte USER est identifie par l'e-mail d'un client (voir SecurityConfig). */
    private Client clientConnecte(String email) {
        return clientRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RegleMetierException("Ce compte n'est rattaché à aucun client."));
    }

    private Reservation reservationDetail(Long id) {
        return reservationRepository.findDetail(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Location introuvable."));
    }
}
