package fr.ece.autoloc.repository;

import fr.ece.autoloc.domain.CategorieVehicule;
import fr.ece.autoloc.domain.StatutReservation;
import fr.ece.autoloc.domain.StatutVehicule;
import fr.ece.autoloc.domain.Vehicule;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VehiculeRepository extends JpaRepository<Vehicule, Long> {

    /**
     * Vehicules louables sur la periode [debut, fin] : hors maintenance, et sans reservation
     * active qui chevauche ces dates. Filtre optionnel sur la categorie.
     * "join fetch v.agence" charge l'agence dans la meme requete (pas de N+1).
     * La countQuery separee sert a calculer le nombre total de pages.
     */
    @Query(value = """
            select v from Vehicule v join fetch v.agence
            where v.statut <> :statutExclu
              and (:categorie is null or v.categorie = :categorie)
              and not exists (
                    select r.idReservation from Reservation r
                    where r.vehicule = v and r.statut in :statutsActifs
                      and r.dateDebut <= :fin and r.dateFin >= :debut)
            """,
            countQuery = """
            select count(v) from Vehicule v
            where v.statut <> :statutExclu
              and (:categorie is null or v.categorie = :categorie)
              and not exists (
                    select r.idReservation from Reservation r
                    where r.vehicule = v and r.statut in :statutsActifs
                      and r.dateDebut <= :fin and r.dateFin >= :debut)
            """)
    Page<Vehicule> rechercherDisponibles(@Param("statutExclu") StatutVehicule statutExclu,
                                         @Param("categorie") CategorieVehicule categorie,
                                         @Param("debut") LocalDate debut,
                                         @Param("fin") LocalDate fin,
                                         @Param("statutsActifs") Collection<StatutReservation> statutsActifs,
                                         Pageable pageable);

    /** Toute la flotte pour l'administration, avec l'agence chargee en meme temps. */
    @Query(value = "select v from Vehicule v join fetch v.agence",
            countQuery = "select count(v) from Vehicule v")
    Page<Vehicule> findAllAvecAgence(Pageable pageable);

    /**
     * Verrou pessimiste (SELECT ... FOR UPDATE) : deux locations simultanees du meme
     * vehicule sont traitees l'une apres l'autre, ce qui empeche la double reservation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicule v where v.idVehicule = :id")
    Optional<Vehicule> findPourMiseAJour(@Param("id") Long id);

    boolean existsByImmatriculationIgnoreCase(String immatriculation);

    /** Nombre de vehicules par statut, calcule par la base (GROUP BY). */
    @Query("select v.statut as statut, count(v) as total from Vehicule v group by v.statut")
    List<CompteParStatut> compterParStatut();

    interface CompteParStatut {
        StatutVehicule getStatut();

        Long getTotal();
    }
}
