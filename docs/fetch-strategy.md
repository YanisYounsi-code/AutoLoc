# Stratégie de chargement (fetch) et de cascade

Workshop 2 : chaque association du modèle AutoLoc, son côté propriétaire, son chargement et sa cascade.

## Rappels

- **Côté propriétaire** : l'entité dont la table porte la clé étrangère (`@ManyToOne`, ou le `@OneToOne` avec `@JoinColumn`).
  C'est lui que Hibernate lit pour écrire la colonne.
- **Côté inverse** : `@OneToMany(mappedBy = "...")`, `@OneToOne(mappedBy = "...")`, `@ManyToMany(mappedBy = "...")`.
  Il ne crée **aucune** colonne ni table : `mappedBy` désigne le nom du champ du côté propriétaire.
- **LAZY** : la donnée liée n'est lue en base que si on y accède. **EAGER** : elle est lue tout de suite, à chaque fois.
- Par défaut JPA met `@ManyToOne` et `@OneToOne` en EAGER : on les passe **toutes en LAZY**.
  Quand un écran a besoin des données liées, la requête les charge explicitement (`join fetch`) : 1 requête au lieu de N+1.
- Lombok : `@Getter` / `@Setter` ciblés, jamais `@Data` (son `toString` / `equals` / `hashCode` bouclerait sur les relations bidirectionnelles).
- Les deux côtés sont tenus à jour par des méthodes utilitaires : `Contrat.ajouterPaiement`, `Reservation.attacherContrat`,
  `Vehicule.ajouterMaintenance`.

## Tableau des associations

| Association | Mapping / côté propriétaire | Fetch | Cascade | Justification |
|---|---|---|---|---|
| Contrat → Paiement | `@OneToMany(mappedBy = "contrat")` / `@ManyToOne` ; Paiement porte `contrat_id` | LAZY | ALL + `orphanRemoval` | Composition : un paiement n'existe pas sans son contrat ; il est enregistré et supprimé avec lui. |
| Agence → Vehicule | `@OneToMany(mappedBy = "agence")` / `@ManyToOne` ; Vehicule porte `agence_id` | LAZY | Aucune | Un véhicule survit à la fermeture d'une agence (il sera réaffecté). |
| Agence → Employe | `@OneToMany(mappedBy = "agence")` / `@ManyToOne` ; Employe porte `agence_id` | LAZY | Aucune | Un employé peut changer d'agence ; son cycle de vie est indépendant. |
| Vehicule ↔ Equipement | `@ManyToMany` ; Vehicule possède la table de jointure `vehicule_equipement` ; `Equipement.vehicules` en `mappedBy` | LAZY + `@BatchSize(50)` | Aucune | Un équipement (GPS…) est partagé par plusieurs véhicules : supprimer un véhicule ne doit pas le supprimer. `Set` pour éviter les doublons. |
| Client → Reservation | `@OneToMany(mappedBy = "client")` / `@ManyToOne` ; Reservation porte `client_id` | LAZY | PERSIST | Enregistrer un nouveau client enregistre ses nouvelles réservations ; pas de REMOVE pour garder l'historique. |
| Reservation → Vehicule | `@ManyToOne` ; Reservation porte `vehicule_id` | LAZY | Aucune | Le véhicule existe avant et après la réservation. |
| Reservation ↔ Contrat | `@OneToOne` ; Contrat porte `reservation_id` (unique) ; `Reservation.contrat` en `mappedBy` | LAZY (voir remarque) | ALL côté Reservation | Le contrat suit le cycle de vie de sa réservation. À la confirmation, `reservation.attacherContrat(contrat)` suffit : la cascade l'insère. |
| Vehicule → Maintenance | `@OneToMany(mappedBy = "vehicule")` / `@ManyToOne` ; Maintenance porte `vehicule_id` | LAZY | PERSIST | Une maintenance ajoutée au véhicule est enregistrée avec lui ; pas de REMOVE pour garder l'historique d'entretien. |

## Remarque : le côté inverse d'un `@OneToOne`

`Reservation.contrat` est déclaré LAZY, mais Hibernate ne peut pas en faire un proxy : pour savoir si la valeur
est `null` ou non, il doit de toute façon lire la table `contrat`. Sans précaution, charger 100 réservations
déclencherait donc 100 requêtes sur `contrat`. Toutes les requêtes de `ReservationRepository` font pour cela
`left join fetch r.contrat`. La méthode `listerSansOptimisation()` (endpoint `/api/admin/locations/non-optimise`)
ne le fait pas, volontairement, pour mesurer l'écart (voir `PERFORMANCE.md`).

## Étape 1 du TP : nom de la clé étrangère

Le PDF fait d'abord écrire `@ManyToOne(fetch = FetchType.LAZY) private Contrat contrat;` **sans** `@JoinColumn`,
pour observer le nom choisi par Hibernate : propriété + identifiant référencé, soit `contrat_id_contrat`.
Dans la version finale, chaque clé étrangère est nommée explicitement avec `@JoinColumn(name = "contrat_id")`
(et `agence_id`, `vehicule_id`, `client_id`, `reservation_id`) : des noms courts, identiques partout, et
indispensables pour l'index composé `reservation(vehicule_id, date_debut, date_fin)`.

Avant l'ajout du côté inverse, un paiement peut naviguer vers son contrat (`paiement.getContrat()`),
mais un contrat ne peut pas lister ses paiements : seule la classe `Paiement` connaît la relation.

## Étape 4 du TP : comparaison LAZY / EAGER (Contrat → Paiement)

| | Requêtes SQL quand on lit un contrat | Conséquence |
|---|---|---|
| LAZY (configuration finale) | 1 (`select ... from contrat`) ; les paiements seulement si on parcourt `getPaiements()` | Lecture légère. Accéder à la collection après la fin de la transaction lève `LazyInitializationException`. |
| EAGER (essai temporaire) | La ligne du contrat **et** ses paiements, à chaque lecture | Pratique, mais toutes les lectures de contrat deviennent plus coûteuses. |

Pour observer : profil `dev` (`spring.jpa.show-sql=true`), puis comparer les logs avec `fetch = FetchType.EAGER`
sur `Contrat.paiements`, et revenir à LAZY.

## Schéma généré (clés étrangères)

| Table | Clés étrangères |
|---|---|
| `vehicule` | `agence_id` → `agence` |
| `employe` | `agence_id` → `agence` |
| `vehicule_equipement` | `vehicule_id` → `vehicule`, `equipement_id` → `equipement` |
| `reservation` | `client_id` → `client`, `vehicule_id` → `vehicule` |
| `contrat` | `reservation_id` → `reservation` (unique) |
| `paiement` | `contrat_id` → `contrat` |
| `maintenance` | `vehicule_id` → `vehicule` |

Aucune colonne n'est créée pour les côtés inverses (`agence.vehicules`, `contrat.paiements`, `reservation.contrat`…) :
c'est la preuve que `mappedBy` est correct.
