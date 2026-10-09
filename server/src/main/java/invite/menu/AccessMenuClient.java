package invite.menu;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fetches the menu model of a user from the SURF Access server. Access is leading in which menu items a user is allowed
 * to see. Access runs on another domain, so the Invite server does the call (authenticated with basic authentication)
 * for the user it has authenticated itself, the browser never talks to Access for this.
 */
@Service
public class AccessMenuClient {

    private static final Log LOG = LogFactory.getLog(AccessMenuClient.class);

    private final String uri;
    private final RestTemplate restTemplate;

    public AccessMenuClient(@Value("${access.menu-uri}") String uri,
                            @Value("${access.username}") String userName,
                            @Value("${access.password}") String password) {
        this.uri = uri;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(5_000);
        this.restTemplate = new RestTemplate(requestFactory);
        this.restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(userName, password));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> menu(String sub, String organizationId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uri).queryParam("sub", "{sub}");
        if (StringUtils.hasText(organizationId)) {
            builder.queryParam("organizationId", "{organizationId}");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        try {
            //No retries, a menu is not worth waiting for
            return restTemplate.exchange(builder.build(false).toUriString(), HttpMethod.GET, new HttpEntity<>(headers), Map.class,
                    organizationId == null ? Map.of("sub", sub) : Map.of("sub", sub, "organizationId", organizationId)).getBody();
        } catch (RuntimeException e) {
            LOG.warn(String.format("Could not fetch the Access menu for %s: %s", sub, e.getMessage()));
            return fallback();
        }
    }

    /**
     * The menu is switched off with the config.access-menu-enabled feature toggle
     */
    public static Map<String, Object> disabled() {
        return Map.of("menuItems", List.of(), "organizations", List.of(), "disabled", true);
    }

    /**
     * Invite is always reachable for the users of Invite, even if Access is not.
     */
    public static Map<String, Object> fallback() {
        return Map.of("menuItems", List.of("invite"), "organizations", List.of(), "fallback", true);
    }
}
