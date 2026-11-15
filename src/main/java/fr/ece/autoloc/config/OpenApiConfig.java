package fr.ece.autoloc.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation Swagger de l'API REST : http://localhost:8080/swagger-ui.html
 * Le bouton "Authorize" demande l'e-mail et le mot de passe d'un compte (HTTP Basic).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "AutoLoc API", version = "1.0",
                description = "API REST de location de véhicules - projet ECE"),
        security = @SecurityRequirement(name = "basic"))
@SecurityScheme(name = "basic", type = SecuritySchemeType.HTTP, scheme = "basic")
public class OpenApiConfig {
}
