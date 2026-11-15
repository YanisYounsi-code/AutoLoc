package fr.ece.autoloc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security, comme dans le Workshop 2 (section 13) : deux comptes en memoire, mots de passe BCrypt.
 *
 * Chaque requete traverse une chaine de filtres AVANT d'arriver au controleur :
 *   1. authentification : qui es-tu ?   (sinon 401 sur l'API, redirection vers /connexion sur les pages)
 *   2. autorisation     : as-tu le role ? (sinon 403)
 *
 * Deux chaines, une par type de client :
 *   - /api/**  (Postman, Swagger) : HTTP Basic, sans session ni CSRF ;
 *   - le reste (pages Thymeleaf)  : formulaire de connexion, session, protection CSRF.
 */
@Configuration
public class SecurityConfig {

    /**
     * Les deux comptes. Les mots de passe viennent de application.properties, qui les lit dans des
     * variables d'environnement (ADMIN_PASSWORD, USER_PASSWORD) : aucun vrai secret n'est ecrit dans Git.
     * L'identifiant du compte USER est l'e-mail d'un client : c'est ce client qui loue.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder,
                                                 @Value("${autoloc.securite.admin.identifiant}") String admin,
                                                 @Value("${autoloc.securite.admin.mot-de-passe}") String motDePasseAdmin,
                                                 @Value("${autoloc.securite.user.identifiant}") String user,
                                                 @Value("${autoloc.securite.user.mot-de-passe}") String motDePasseUser) {
        UserDetails compteUser = User.withUsername(user)
                .password(encoder.encode(motDePasseUser))
                .roles("USER").build();
        UserDetails compteAdmin = User.withUsername(admin)
                .password(encoder.encode(motDePasseAdmin))
                .roles("ADMIN").build();
        return new InMemoryUserDetailsManager(compteUser, compteAdmin);
    }

    /** BCrypt : hachage lent et sale (un aleatoire different par mot de passe). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Chaine 1 : l'API REST. Les regles vont du plus precis au plus general (la premiere qui correspond gagne). */
    @Bean
    @Order(1)
    public SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/locations/**").hasRole("USER")
                        .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated())
                // Identifiants envoyes a chaque requete (en-tete Authorization: Basic ...)
                .httpBasic(basic -> { })
                // Sans etat : aucune session, donc le cookie d'une page ouverte ne donne pas acces a l'API
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Sans cookie de session, une attaque CSRF n'a rien a exploiter : protection inutile ici
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /** Chaine 2 : les pages web. */
    @Bean
    @Order(2)
    public SecurityFilterChain pages(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/connexion", "/css/**", "/favicon.svg", "/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/actuator/health").permitAll()
                        .requestMatchers("/admin/**", "/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/louer", "/mes-locations/**").hasRole("USER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/connexion")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/connexion?erreur")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/deconnexion")
                        .logoutSuccessUrl("/connexion?deconnecte"));
        // CSRF reste ACTIVE (par defaut) : Thymeleaf ajoute le jeton cache dans chaque formulaire.
        return http.build();
    }
}
