package fr.ece.autoloc.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Agence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAgence;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String ville;

    @Column(length = 255)
    private String adresse;

    @Column(length = 20)
    private String telephone;

    // Agence 1 -> * Vehicule. Cote "inverse" (mappedBy) : la cle etrangere agence_id est dans la table vehicule.
    // Pas de cascade : un vehicule survit a la suppression de son agence (il sera reaffecte).
    @OneToMany(mappedBy = "agence", fetch = FetchType.LAZY)
    private List<Vehicule> vehicules = new ArrayList<>();

    // Agence 1 -> * Employe. Pas de cascade : un employe peut changer d'agence.
    @OneToMany(mappedBy = "agence", fetch = FetchType.LAZY)
    private List<Employe> employes = new ArrayList<>();
}
