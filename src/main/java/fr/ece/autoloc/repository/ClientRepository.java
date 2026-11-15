package fr.ece.autoloc.repository;

import fr.ece.autoloc.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    /** Le compte USER se connecte avec l'e-mail d'un client : c'est ainsi qu'on retrouve "qui loue". */
    Optional<Client> findByEmailIgnoreCase(String email);
}
