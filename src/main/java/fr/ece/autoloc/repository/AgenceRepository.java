package fr.ece.autoloc.repository;

import fr.ece.autoloc.domain.Agence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// JpaRepository fournit deja save, findById, findAll, deleteById, count...
// Spring Data genere l'implementation au demarrage : on n'ecrit aucun SQL.
public interface AgenceRepository extends JpaRepository<Agence, Long> {

    // Requete deduite du nom de la methode : select ... order by ville, nom
    List<Agence> findAllByOrderByVilleAscNomAsc();
}
