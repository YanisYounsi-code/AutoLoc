package fr.ece.autoloc.service;

import fr.ece.autoloc.domain.StatutReservation;
import fr.ece.autoloc.domain.StatutVehicule;
import fr.ece.autoloc.repository.ReservationRepository;
import fr.ece.autoloc.repository.VehiculeRepository;
import fr.ece.autoloc.web.dto.TableauDeBordResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final VehiculeRepository vehiculeRepository;
    private final ReservationRepository reservationRepository;

    public AdminService(VehiculeRepository vehiculeRepository, ReservationRepository reservationRepository) {
        this.vehiculeRepository = vehiculeRepository;
        this.reservationRepository = reservationRepository;
    }

    /** Chaque chiffre est calcule par la base (COUNT, GROUP BY) : aucune ligne n'est chargee en memoire. */
    public TableauDeBordResponse tableauDeBord() {
        Map<StatutVehicule, Long> parStatut = new EnumMap<>(StatutVehicule.class);
        vehiculeRepository.compterParStatut().forEach(c -> parStatut.put(c.getStatut(), c.getTotal()));
        return new TableauDeBordResponse(
                parStatut.getOrDefault(StatutVehicule.DISPONIBLE, 0L),
                parStatut.getOrDefault(StatutVehicule.LOUE, 0L),
                parStatut.getOrDefault(StatutVehicule.MAINTENANCE, 0L),
                reservationRepository.countByStatut(StatutReservation.EN_ATTENTE));
    }
}
