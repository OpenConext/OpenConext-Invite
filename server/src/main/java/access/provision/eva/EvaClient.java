package access.provision.eva;

import access.model.User;
import access.provision.Provisioning;
import crypto.KeyStore;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.util.MultiValueMap;

import java.net.URI;

public class EvaClient {


    private final KeyStore keyStore;

    public EvaClient(KeyStore keyStore) {
        this.keyStore= keyStore;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public RequestEntity<String> newUserRequest(Provisioning provisioning, User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String encryptedEvaToken = provisioning.getEvaToken();
        String evaToken = keyStore.isEncryptedSecret(encryptedEvaToken) ? keyStore.decodeAndDecrypt(encryptedEvaToken) : encryptedEvaToken;
        headers.add("X-Api-Key", evaToken);

        MultiValueMap<String, String> map = new GuestAccount(user, provisioning).getRequest();
        String url = provisioning.getEvaUrl() + "/api/v1/guest/create";
        return new RequestEntity(map, headers, HttpMethod.POST, URI.create(url));
    }

}
