package invite.eduid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Optional;

@Service
public class EduID {

    private static final Log LOG = LogFactory.getLog(EduID.class);

    private final String uri;
    private final RestTemplate restTemplate;
    private final HttpHeaders headers;

    public EduID(@Value("${myconext.uri}") String uri,
                 @Value("${myconext.username}") String userName,
                 @Value("${myconext.password}") String password) {
        this.uri = uri;
        this.restTemplate = new RestTemplate();
        this.restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(userName, password));
        this.headers = initHttpHeaders();
    }

    public Optional<String> provisionEduid(EduIDProvision eduIDProvision) {
        HttpEntity<EduIDProvision> requestEntity = new HttpEntity<>(eduIDProvision, headers);
        try {
            ResponseEntity<EduIDProvision> responseEntity = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, EduIDProvision.class);
            return Optional.of(responseEntity.getBody().getEduIDValue());
        } catch (RuntimeException e) {
            LOG.error("Error in provisionEduid", e);
            return Optional.empty();
        }
    }

    private HttpHeaders initHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
