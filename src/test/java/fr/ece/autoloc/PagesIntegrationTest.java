package fr.ece.autoloc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Genere vraiment chaque page avec Thymeleaf : une erreur dans un template (fragment introuvable,
 * variable mal nommee...) fait echouer le test au lieu d'apparaitre dans le navigateur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PagesIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void la_page_de_connexion_s_affiche() throws Exception {
        mvc.perform(get("/connexion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Se connecter")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void la_page_vehicules_s_affiche_pour_le_user() throws Exception {
        mvc.perform(get("/vehicules").with(user("user@autoloc.fr").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Véhicules disponibles")))
                .andExpect(content().string(containsString("Mes locations")));
    }

    @Test
    void la_page_administration_s_affiche_pour_l_admin() throws Exception {
        mvc.perform(get("/admin").with(user("admin@autoloc.fr").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ajouter un véhicule")));
    }
}
