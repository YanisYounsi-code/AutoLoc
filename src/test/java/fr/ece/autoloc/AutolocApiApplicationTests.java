package fr.ece.autoloc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AutolocApiApplicationTests {

    @Test
    void contextLoads() {
        // Reussit si toute la configuration (JPA, securite, cache...) demarre sans erreur.
    }
}
