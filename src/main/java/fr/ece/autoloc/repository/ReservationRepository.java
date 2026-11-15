package fr.ece.autoloc.repository;

import fr.ece.autoloc.domain.Reservation;
import fr.ece.autoloc.domain.StatutReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Nombre de reservations du vehicule qui chevauchent la periode [debut, fin].
     * Deux periodes se chevauchent si l'une commence avant la fin de l'autre et inversement.
     */
    @Query("""
            select count(r) from Reservation r
            where r.vehicule.idVehicule = :vehiculeId
              and r.statut in :statuts
              and r.dateDebut <= :fin and r.dateFin >= :debut
              and (:exclueId is null or r.idReservation <> :exclueId)
            """)
    long compterChevauchements(@Param("vehiculeId") Long vehiculeId,
                               @Param("statuts") Collection<StatutReservation> statuts,
                               @Param("debut") LocalDate debut,
                               @Param("fin") LocalDate fin,
                               @Param("exclueId") Long exclueId);

    /** Administration : une page de locations avec client, vehicule, agence et contrat en 1 requete. */
    @Query(value = """
            select r from Reservation r
            join fetch r.client
            join fetch r.vehicule v
            join fetch v.agence
            left join fetch r.contrat
            where (:statut is null or r.statut = :statut)
            """,
            countQuery = "select count(r) from Reservation r where (:statut is null or r.statut = :statut)")
    Page<Reservation> rechercher(@Param("statut") StatutReservation statut, Pageable pageable);

    /** Toutes les locations d'un client. */
    @Query("""
            select r from Reservation r
            join fetch r.vehicule v
            join fetch v.agence
            join fetch r.client c
            left join fetch r.contrat
            where c.idClient = :clientId
            order by r.dateDebut desc
            """)
    List<Reservation> findDuClient(@Param("clientId") Long clientId);

    @Query("""
            select r from Reservation r
            join fetch r.client
            join fetch r.vehicule v
            join fetch v.agence
            left join fetch r.contrat
            where r.idReservation = :id
            """)
    Optional<Reservation> findDetail(@Param("id") Long id);

    long countByStatut(StatutReservation statut);
}
