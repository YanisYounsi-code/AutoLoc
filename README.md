# AutoLoc API

Application Spring Boot de location de véhicules : une API REST et une interface web très simple
(pages Thymeleaf, 100 % Java côté serveur), avec **deux comptes** : un utilisateur qui loue, un administrateur.
Projet du cours « Architecture et performance des systèmes d'information » (ECE).

## Technologies

| Rôle | Outil |
|---|---|
| Langage, build | Java 17, Maven |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation, Security, Thymeleaf, Cache, Actuator) |
| Persistance | JPA / Hibernate, MySQL 8 |
| Interface | Thymeleaf (HTML généré côté serveur, aucun JavaScript) |
| Sécurité | Spring Security : 2 comptes en mémoire, BCrypt, formulaire de connexion (pages) et HTTP Basic (API) |
| Performance | Pagination, LAZY + JOIN FETCH, index, cache Caffeine, pool HikariCP |
| Documentation | Swagger UI (springdoc-openapi) |
| Conteneurs | Docker, Docker Compose |
| Tests | JUnit 5, Mockito, MockMvc, H2 ; Postman ; test de charge k6 |

## Lancer le projet

### Option 1 : IntelliJ + XAMPP

1. Démarrer MySQL dans XAMPP (port 3306).
2. Ouvrir le projet dans IntelliJ et lancer `AutolocApiApplication`.
3. La base `autoloc_db`, les tables et les données de démonstration sont créées automatiquement.

Si le compte `root` de MySQL a un mot de passe : variable d'environnement `DB_PASSWORD` dans la configuration de lancement.

### Option 2 : Docker (tout en une commande)

```bash
docker compose up --build
```

Docker Desktop doit être lancé. MySQL tourne dans un conteneur (port 3307 côté PC, pour ne pas gêner XAMPP).

## Les deux comptes

| Rôle | Identifiant | Mot de passe par défaut | Ce qu'il peut faire |
|---|---|---|---|
| USER | user@autoloc.fr | user123 | voir les véhicules libres, louer, suivre et annuler ses locations |
| ADMIN | admin@autoloc.fr | admin123 | confirmer / annuler / terminer les locations, gérer la flotte |

Les mots de passe sont lus dans les variables d'environnement `USER_PASSWORD` et `ADMIN_PASSWORD`
(fichier `.env` avec Docker, voir `.env.example`). Les valeurs par défaut servent uniquement en salle de TP.
Le compte USER porte l'e-mail du client « Camille Martin » : c'est à son nom que les locations sont créées.

## Adresses

| Page | URL |
|---|---|
| Connexion | http://localhost:8080/connexion |
| Véhicules (USER et ADMIN) | http://localhost:8080/vehicules |
| Mes locations (USER) | http://localhost:8080/mes-locations |
| Administration (ADMIN) | http://localhost:8080/admin |
| Documentation de l'API | http://localhost:8080/swagger-ui.html |
| Santé de l'application | http://localhost:8080/actuator/health |
| phpMyAdmin (Docker) | http://localhost:8081 |

## Parcours de démonstration

1. Se connecter en `user@autoloc.fr`, choisir des dates, cliquer **Louer** : la location est « En attente ».
2. Se déconnecter, se connecter en `admin@autoloc.fr` : **Confirmer** la location (contrat créé, véhicule « Loué »).
3. **Terminer** la location : le solde est payé, le véhicule redevient disponible.

## API REST (Postman, HTTP Basic)

| Méthode et URL | Anonyme | USER | ADMIN |
|---|---|---|---|
| `GET /api/public/ping` | 200 | 200 | 200 |
| `GET /api/hello` | 401 | 200 | 200 |
| `GET /api/admin/report` | 401 | 403 | 200 |
| `GET /api/vehicules?debut=2026-12-01&fin=2026-12-03` | 401 | 200 | 200 |
| `GET /api/locations`, `POST /api/locations` | 401 | 200 / 201 | 403 |
| `GET /api/admin/locations`, `POST /api/admin/locations/{id}/confirmation` | 401 | 403 | 200 |

Dans Postman : onglet **Authorization**, type **Basic Auth**, identifiant et mot de passe d'un compte.

## Architecture

```
fr.ece.autoloc
├── config        SecurityConfig (comptes, BCrypt, règles d'accès), AppConfig (horloge),
│                 OpenApiConfig (Swagger), DonneesDemo (données au 1er démarrage)
├── domain        les 9 entités JPA et leurs associations, enums
├── repository    interfaces Spring Data JPA (requêtes JPQL avec join fetch)
├── service       règles métier et transactions
├── exception     exceptions métier (404, 409, 400)
└── web
    ├── controller   contrôleurs REST (JSON) + gestion des erreurs de l'API
    ├── page         contrôleurs des pages (renvoient un template Thymeleaf)
    └── dto          objets d'entrée / sortie (jamais les entités directement)
resources/templates  pages HTML Thymeleaf      resources/static/css  feuille de style
```

Une requête traverse : filtres Spring Security → contrôleur → service → repository → Hibernate → MySQL.

## Documentation du projet

- [docs/fetch-strategy.md](docs/fetch-strategy.md) : chaque association, son fetch et sa cascade, justifiés (Workshop 2).
- [docs/security-notes.md](docs/security-notes.md) : règles d'accès, BCrypt, limites des comptes en mémoire (Workshop 2).
- [docs/PERFORMANCE.md](docs/PERFORMANCE.md) : optimisations, mesure du N+1 et test de charge k6.

## Tests

```bash
mvn test
```

Les tests utilisent une base H2 en mémoire : MySQL n'est pas nécessaire.
