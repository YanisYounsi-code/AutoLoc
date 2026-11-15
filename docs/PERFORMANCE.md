# Performance : méthode et mesures

Principe : **mesurer, optimiser, mesurer à nouveau**, et montrer l'écart en chiffres.

## 1. Optimisations en place

| Problème | Solution dans AutoLoc | Où |
|---|---|---|
| N+1 : afficher 10 locations déclenche des dizaines de requêtes | `join fetch` client + véhicule + agence + `left join fetch` contrat : **1 requête** pour la page (+ 1 de comptage) | `ReservationRepository.rechercher` |
| Côté inverse d'un `@OneToOne` (Reservation.contrat) jamais LAZY | Toujours chargé par `left join fetch r.contrat` dans les requêtes | `ReservationRepository` |
| Relations chargées inutilement | `fetch = FetchType.LAZY` sur toutes les associations (voir `fetch-strategy.md`) | `domain/*` |
| Équipements d'une page de véhicules | `@BatchSize(size = 50)` : 1 requête pour toute la page | `Vehicule.equipements` |
| Listes complètes renvoyées | Pagination (`PageRequest`) : 9 véhicules, 10 locations par page | services |
| Recherches lentes | Index sur `vehicule(statut)`, `vehicule(categorie)`, `reservation(vehicule_id, date_debut, date_fin)`, `reservation(statut)` | `@Table(indexes = ...)` |
| Liste des agences relue à chaque affichage | Cache Caffeine (`@Cacheable("agences")`) | `VehiculeService.agences` |
| Chiffres du tableau de bord | Calculés par la base (`COUNT`, `GROUP BY`), aucune ligne chargée en mémoire | `AdminService.tableauDeBord` |
| Connexion BDD gardée pendant toute la requête HTTP | `spring.jpa.open-in-view=false` | `application.properties` |
| Ouverture de connexions | Pool HikariCP (10 connexions réutilisées) | `application.properties` |
| Double location en cas d'accès simultanés | Verrou pessimiste `SELECT ... FOR UPDATE` sur le véhicule | `VehiculeRepository.findPourMiseAJour` |

## 2. Compter les requêtes SQL (problème N+1)

Lancer l'application avec le profil `dev` (IntelliJ : Run > Edit Configurations > Active profiles : `dev`).
Chaque appel affiche alors : `... nanoseconds spent executing N JDBC statements`.

Comparer dans Postman (Authorization > Basic Auth, compte admin) :

- `GET /api/admin/locations?page=0` : version optimisée, 2 requêtes SQL ;
- `GET /api/admin/locations/non-optimise` : version volontairement naïve (toutes les locations, sans `join fetch`),
  gardée pour la démonstration : plusieurs milliers de requêtes.

## 3. Test de charge (k6)

```bash
docker compose up -d --build                                  # application + MySQL
docker compose --profile perf run --rm k6                      # version optimisée
docker compose --profile perf run --rm -e CIBLE=naif k6        # version non optimisée
```

Scénario : montée de 0 à 50 utilisateurs simultanés pendant une minute ; chacun consulte les véhicules
libres (compte user) et la liste des locations de l'administration (compte admin), en HTTP Basic. Objectifs : 95 % des requêtes en moins de 500 ms
et moins de 1 % d'erreurs.

## 4. Résultats

### 4.1 Une requête, mesurée avec le profil `dev` et Postman

Mesure du 15/11/2026, PC Windows de développement, MySQL (MariaDB de XAMPP) en local,
1 338 locations en base (données de démonstration). Compte admin, HTTP Basic.

| Indicateur | Non optimisé (`/locations/non-optimise`) | Optimisé (`/locations?page=0`) | Gain |
|---|---|---|---|
| Requêtes SQL envoyées à MySQL (`executing N JDBC statements`) | 1 542 | 2 | ÷ 771 |
| Temps passé à exécuter le SQL | 975,6 ms | 14,7 ms | ≈ 66 × plus rapide |
| Temps de réponse vu par Postman | 2,37 s | 198 ms | ≈ 12 × plus rapide |
| Taille de la réponse | 428,45 Ko | 3,62 Ko | ≈ 118 × plus légère |

Lecture des résultats :

- **1 542 requêtes au lieu de 2 : c'est le problème N+1.** La version naïve lit d'abord toutes les locations
  (1 requête), puis Hibernate déclenche une requête de plus pour le contrat de chaque location (côté inverse
  d'un `@OneToOne`, chargé dès la lecture), et pour chaque client, véhicule et agence rencontré (proxys LAZY
  initialisés un par un). La version optimisée charge tout avec `join fetch` en 1 requête, plus 1 requête de
  comptage pour la pagination.
- **Preuves :** captures Postman et console IntelliJ dans `docs/captures/performance/`.
- **La pagination compte aussi :** la version naïve renvoie les 1 338 locations d'un coup, la version optimisée
  une page de 10. Les deux optimisations (`join fetch` et pagination) expliquent ensemble l'écart de temps et de taille.

### 4.2 Sous charge, avec k6 (Docker)

Mesure du 15/11/2026, application, MySQL 8.4 et k6 dans Docker Desktop sur le même PC Windows.
Scénario : 0 → 20 → 50 utilisateurs simultanés pendant 1 minute ; chaque itération appelle
`/api/vehicules` (compte user) puis la liste des locations (compte admin), avec 1 s de pause.

| Indicateur | Non optimisé (`CIBLE=naif`) | Optimisé | Gain |
|---|---|---|---|
| Temps de réponse moyen (`http_req_duration avg`) | 7,73 s | 1,08 s | ≈ 7 × plus rapide |
| Médiane (`med`) | 7,56 s | 937 ms | ≈ 8 × plus rapide |
| 95e percentile (`p(95)`) | 13,33 s | 2,51 s | ≈ 5 × plus rapide |
| Pire requête (`max`) | 19,8 s | 3,59 s | ≈ 5,5 × plus rapide |
| Requêtes traitées (`http_reqs`) | 234 (3,5 /s) | 1 040 (17,2 /s) | ≈ 4,9 × plus de débit |
| Taux d'erreur (`http_req_failed`) | 0 % | 0 % | — |
| Données reçues par k6 | 52 Mo | 3,1 Mo | ≈ 17 × moins |

Sorties complètes de k6 : `docs/captures/performance/k6-non-optimise.txt` et `k6-optimise.txt`.

Lecture des résultats :

- **Même nombre d'utilisateurs, 4,4 fois plus de requêtes servies** (1 040 contre 234) et un temps de réponse
  divisé par 7. Dans la version naïve, chaque appel lance environ 1 500 requêtes SQL : les 10 connexions du pool
  sont vite toutes occupées et les utilisateurs font la queue.
- **L'objectif `p(95) < 500 ms` n'est pas atteint, même optimisé (2,51 s).** Le coût restant ne vient plus de la
  base mais de **BCrypt** : en HTTP Basic, le mot de passe est vérifié à chaque requête, et BCrypt est lent
  volontairement (environ 100 ms de calcul). Les captures Postman le montrent : une requête refusée sans compte
  répond en 21 ms (`07-report-anonyme-401.png`), une requête authentifiée en 110 à 140 ms. Avec 50 utilisateurs
  sur les quelques processeurs alloués à Docker, ces calculs s'additionnent.
- **Piste d'amélioration :** vérifier le mot de passe une seule fois, puis utiliser une session ou un jeton (JWT)
  dont la vérification ne coûte presque rien. Baisser le coût de BCrypt serait plus rapide mais affaiblirait la sécurité.
