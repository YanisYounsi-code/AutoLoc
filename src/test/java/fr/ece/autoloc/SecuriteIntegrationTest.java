package fr.ece.autoloc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'integration : toute l'application demarre (base H2 en memoire) et on envoie des requetes HTTP
 * simulees avec les VRAIS comptes. Ce test automatise le tableau 200 / 401 / 403 du Workshop 2,
 * en plus de la verification manuelle demandee dans Postman.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecuriteIntegrationTest {

    private static final String USER = "user@autoloc.fr";
    private static final String ADMIN = "admin@autoloc.fr";

    @Autowired
    private MockMvc mvc;

    // ---------------------------------------------------------------- tableau du Workshop 2 (HTTP Basic)

    @Test
    void la_route_publique_repond_a_tout_le_monde() throws Exception {
        mvc.perform(get("/api/public/ping")).andExpect(status().isOk());
        mvc.perform(get("/api/public/ping").with(httpBasic(USER, "user123"))).andExpect(status().isOk());
        mvc.perform(get("/api/public/ping").with(httpBasic(ADMIN, "admin123"))).andExpect(status().isOk());
    }

    @Test
    void hello_exige_d_etre_authentifie() throws Exception {
        mvc.perform(get("/api/hello")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/hello").with(httpBasic(USER, "user123"))).andExpect(status().isOk());
        mvc.perform(get("/api/hello").with(httpBasic(ADMIN, "admin123"))).andExpect(status().isOk());
    }

    @Test
    void le_rapport_admin_est_reserve_au_role_admin() throws Exception {
        mvc.perform(get("/api/admin/report")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/report").with(httpBasic(USER, "user123"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/report").with(httpBasic(ADMIN, "admin123"))).andExpect(status().isOk());
    }

    @Test
    void un_mauvais_mot_de_passe_donne_401() throws Exception {
        mvc.perform(get("/api/hello").with(httpBasic(USER, "faux"))).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- API metier

    @Test
    void le_tableau_de_bord_est_reserve_a_l_admin() throws Exception {
        mvc.perform(get("/api/admin/tableau-de-bord").with(httpBasic(USER, "user123")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/tableau-de-bord").with(httpBasic(ADMIN, "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationsEnAttente").value(0));
    }

    @Test
    void des_donnees_invalides_renvoient_400_avec_le_detail_des_champs() throws Exception {
        mvc.perform(post("/api/admin/vehicules").with(httpBasic(ADMIN, "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"immatriculation\":\"\",\"marque\":\"Test\",\"modele\":\"X\",\"categorie\":\"SUV\",\"tarifJournalier\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.immatriculation").exists())
                .andExpect(jsonPath("$.erreurs.tarifJournalier").exists());
    }

    // ---------------------------------------------------------------- pages web (formulaire + session)

    @Test
    void une_page_sans_connexion_redirige_vers_le_formulaire() throws Exception {
        mvc.perform(get("/vehicules"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/connexion"));
    }

    @Test
    void la_connexion_par_formulaire_fonctionne() throws Exception {
        mvc.perform(formLogin("/connexion").user(USER).password("user123"))
                .andExpect(redirectedUrl("/"));
        mvc.perform(formLogin("/connexion").user(USER).password("faux"))
                .andExpect(redirectedUrl("/connexion?erreur"));
    }

    @Test
    void un_user_ne_peut_pas_ouvrir_l_administration() throws Exception {
        mvc.perform(get("/admin").with(user(USER).roles("USER"))).andExpect(status().isForbidden());
    }

    @Test
    void un_formulaire_sans_jeton_csrf_est_refuse() throws Exception {
        mvc.perform(post("/louer").with(user(USER).roles("USER"))
                        .param("vehiculeId", "1").param("dateDebut", "2030-01-01").param("dateFin", "2030-01-02"))
                .andExpect(status().isForbidden());
    }
}
