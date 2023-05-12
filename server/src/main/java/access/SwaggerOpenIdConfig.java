package access;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
@OpenAPIDefinition
public class SwaggerOpenIdConfig {

    public static final String OPEN_ID_SCHEME_NAME = "openId";

    @Bean
    OpenAPI customOpenApi(@Value("${oidcng.discovery-url}") String discoveryURL,
                          @Value("${oidcng.base-url}") String baseUrl) {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .openIdConnectUrl(discoveryURL)
                .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                        .authorizationUrl("https://connect.test2.surfconext.nl/oidc/authorize/")
                        .tokenUrl("https://connect.test2.surfconext.nl/oidc/token/")
                        .scopes(new Scopes().addString("openid", "openid"))));

        Components components = new Components()
                .addSecuritySchemes(OPEN_ID_SCHEME_NAME, securityScheme);

        OpenAPI openAPI = new OpenAPI()
                .info(new Info().description("Access app API endpoints").title("Access app API"))
                .addServersItem(new Server().url(baseUrl));

        openAPI.components(components)
                .addSecurityItem(new SecurityRequirement().addList(OPEN_ID_SCHEME_NAME));
        return openAPI;
    }

    @Bean
    ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}