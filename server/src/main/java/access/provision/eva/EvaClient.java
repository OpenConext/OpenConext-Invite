package access.provision.eva;

import access.model.User;
import access.provision.Provisioning;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.util.MultiValueMap;

import java.net.URI;

public class EvaClient {

    @SuppressWarnings("unchecked")
    public RequestEntity<String> newUserRequest(Provisioning provisioning, User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("X-Api-Key", provisioning.getEvaToken());

        MultiValueMap<String, String> map = new GuestAccount(user, provisioning).getRequest();
        String url = provisioning.getEvaUrl() + "/api/v1/guest/create";
        return new RequestEntity(map, headers, HttpMethod.POST, URI.create(url));
    }

}
