package fr.ece.autoloc.repository;

import fr.ece.autoloc.domain.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Les contrats sont enregistres par cascade depuis leur reservation (Reservation.contrat, cascade ALL),
 * et leurs paiements par cascade depuis le contrat : ce repository sert surtout aux lectures directes.
 */
public interface ContratRepository extends JpaRepository<Contrat, Long> {
}
