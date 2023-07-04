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
    public static final String VOOT_SCHEME_NAME = "voot";
    public static final String ATTRIBUTE_AGGREGATION_SCHEME_NAME = "attributeAggregation";

    @Bean
    OpenAPI customOpenApi(@Value("${spring.security.oauth2.client.provider.oidcng.authorization-uri}") String authorizationUrl,
                          @Value("${spring.security.oauth2.client.provider.oidcng.token-uri}") String tokenUrl,
                          @Value("${oidcng.discovery-url}") String discoveryURL,
                          @Value("${oidcng.base-url}") String baseUrl) {
        SecurityScheme openIdSecuritySchema = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .openIdConnectUrl(discoveryURL)
                .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                        .authorizationUrl(authorizationUrl)
                        .tokenUrl(tokenUrl)
                        .scopes(new Scopes().addString("openid", "openid"))));
        SecurityScheme basicAuthentication = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic");

        Components components = new Components()
                .addSecuritySchemes(OPEN_ID_SCHEME_NAME, openIdSecuritySchema)
                .addSecuritySchemes(VOOT_SCHEME_NAME, basicAuthentication)
                .addSecuritySchemes(ATTRIBUTE_AGGREGATION_SCHEME_NAME, basicAuthentication);

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