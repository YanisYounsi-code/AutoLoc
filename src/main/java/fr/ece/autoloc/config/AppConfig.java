package fr.ece.autoloc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /**
     * L'horloge est un bean : les services demandent "quelle date sommes-nous ?" a Spring.
     * Dans les tests, on la remplace par une horloge fixe pour des resultats reproductibles.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
