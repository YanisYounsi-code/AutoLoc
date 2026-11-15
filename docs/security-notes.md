# Notes de sécurité

Workshop 2, section 13 : authentification et autorisation avec Spring Security.

## Authentification et autorisation

- **Authentification** : vérifier *qui* fait la requête (identifiant + mot de passe). Échec : **401 Unauthorized**.
- **Autorisation** : vérifier que cette personne a le *droit* d'accéder à la ressource (son rôle). Échec : **403 Forbidden**.

401 veut dire « je ne sais pas qui vous êtes » ; 403 veut dire « je sais qui vous êtes, mais c'est interdit ».

## Les deux comptes

Déclarés dans `fr.ece.autoloc.config.SecurityConfig` (package du PDF), méthode `userDetailsService()`,
avec un `InMemoryUserDetailsManager` :

| Identifiant | Rôle | Mot de passe |
|---|---|---|
| `user@autoloc.fr` | USER | variable `USER_PASSWORD` (défaut de TP : `user123`) |
| `admin@autoloc.fr` | ADMIN | variable `ADMIN_PASSWORD` (défaut de TP : `admin123`) |

Différence avec le PDF : les identifiants sont des e-mails (`user@autoloc.fr` au lieu de `student`), car le compte
USER doit correspondre à un client de la base pour pouvoir louer (`ClientRepository.findByEmailIgnoreCase`).
Les mots de passe par défaut suivent le même modèle que le PDF (`user123`, `admin123`).

Aucun vrai mot de passe n'est écrit dans Git : `application.properties` lit les variables d'environnement,
et le fichier `.env` (Docker) est dans `.gitignore`.

## Deux chaînes de filtres

| Chaîne | URL | Authentification | Session | CSRF |
|---|---|---|---|---|
| `api` (`@Order(1)`) | `/api/**` | HTTP Basic (en-tête `Authorization` à chaque requête) | Aucune (STATELESS) | Désactivé |
| `pages` (`@Order(2)`) | tout le reste | Formulaire `/connexion` | Cookie de session | **Activé** |

Différence avec le PDF : le PDF n'a qu'une chaîne (API seule). AutoLoc a aussi des pages web, d'où une
seconde chaîne ; les règles de l'API sont exactement celles du PDF, plus `/api/locations/**` réservé au USER.

Pourquoi deux chaînes : Postman et Swagger envoient les identifiants à chaque appel, tandis qu'un navigateur
se connecte une fois et garde une session. CSRF (falsification de requête inter-sites) exploite un cookie envoyé
automatiquement par le navigateur : il est donc indispensable sur les pages (Thymeleaf ajoute le jeton caché dans
chaque formulaire `th:action`), et inutile sur une API sans session.

## Règles d'accès de l'API

```java
.requestMatchers("/api/public/**").permitAll()
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/locations/**").hasRole("USER")
.requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
```

Résultats attendus (à vérifier dans Postman, Authorization > Basic Auth) :

| Requête | Anonyme | USER | ADMIN |
|---|---|---|---|
| `GET /api/public/ping` | 200 | 200 | 200 |
| `GET /api/hello` | 401 | 200 | 200 |
| `GET /api/admin/report` | 401 | 403 | 200 |

Le livrable du PDF est la vérification **manuelle** dans Postman, avec captures d'écran : les 9 essais sont
dans `docs/captures/securite/` (une image par essai, et le récapitulatif `tests-securite-postman.pdf`). Le test
`SecuriteIntegrationTest` rejoue en plus ce même tableau automatiquement, à chaque `mvn test`.

## Réponses aux questions du Workshop

**Pourquoi `/api/admin/**` doit-il être déclaré avant `/api/**` ?**
Spring Security applique la **première** règle qui correspond à l'URL. `/api/**` correspond aussi à `/api/admin/report` :
déclaré en premier, il laisserait un USER accéder à l'administration. On va donc du plus précis au plus général.

**Différence entre 401 et 403 ?**
401 : non authentifié (pas d'identifiants, ou identifiants faux). 403 : authentifié, mais rôle insuffisant.

**Pourquoi BCrypt plutôt que le mot de passe en clair ?**
Si la base ou la configuration fuit, un mot de passe en clair est lisible immédiatement. BCrypt est un hachage
à sens unique, **salé** (un aléa différent par mot de passe, donc deux mots de passe identiques donnent deux empreintes
différentes) et **lent** volontairement (facteur de coût), ce qui rend les attaques par force brute très coûteuses.

**Pourquoi des comptes en mémoire conviennent au TP mais pas à la production ?**
Ils sont recréés à chaque démarrage, figés dans le code (on ne peut ni s'inscrire ni changer de mot de passe
sans redéployer), non partagés entre plusieurs instances de l'application, et sans gestion de verrouillage ou d'audit.

**Que faudrait-il pour charger les utilisateurs et rôles depuis MySQL ?**
Une entité `Utilisateur` (identifiant unique, empreinte BCrypt, rôle), un `UtilisateurRepository`
avec `findByEmail`, et une classe qui implémente `UserDetailsService` : sa méthode `loadUserByUsername` lit
l'utilisateur en base et renvoie un `User.withUsername(...).password(empreinte).roles(role)`. On retire alors le
`InMemoryUserDetailsManager` ; le reste de la configuration ne change pas.
