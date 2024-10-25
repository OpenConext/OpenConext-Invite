package access.provision.eva;

import access.model.User;
import access.provision.Provisioning;
import access.repository.RemoteProvisionedUserRepository;
import crypto.KeyStore;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.List;

@SuppressWarnings("unchecked")
public class EvaClient {

    private final KeyStore keyStore;
    private final RemoteProvisionedUserRepository remoteProvisionedUserRepository;

    public EvaClient(KeyStore keyStore, RemoteProvisionedUserRepository remoteProvisionedUserRepository) {
        this.keyStore= keyStore;
        this.remoteProvisionedUserRepository = remoteProvisionedUserRepository;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public RequestEntity<String> newUserRequest(Provisioning provisioning, User user) {
        return doEvaRequest(provisioning, user, "/api/v1/guest/create", RequestType.create);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public RequestEntity updateUserRequest(Provisioning provisioning, User user) {
        return doEvaRequest(provisioning, user, "/api/v1/guest/create", RequestType.update);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public RequestEntity deleteUserRequest(Provisioning provisioning, User user) {
        return remoteProvisionedUserRepository.findByManageProvisioningIdAndUser(provisioning.getId(), user)
                .map(remoteProvisionedUser -> {
                    String path = "/api/v1/guest/disable/" + remoteProvisionedUser.getRemoteIdentifier();
                    remoteProvisionedUserRepository.delete(remoteProvisionedUser);
                    return doEvaRequest(
                            provisioning,
                            user,
                            path,
                            RequestType.delete);
                })
                .orElse(null);
    }

    private RequestEntity doEvaRequest(Provisioning provisioning, User user, String path, RequestType requestType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String encryptedEvaToken = provisioning.getEvaToken();
        String evaToken = keyStore.isEncryptedSecret(encryptedEvaToken) ? keyStore.decodeAndDecrypt(encryptedEvaToken) : encryptedEvaToken;
        headers.add("X-Api-Key", evaToken);

        String url = provisioning.getEvaUrl() + path;
        if (requestType.equals(RequestType.delete)) {
            return new RequestEntity(headers, HttpMethod.POST, URI.create(url));
        }
        MultiValueMap<String, String> map = new GuestAccount(user, provisioning).getRequest();
        if (requestType.equals(RequestType.update)) {
            this.remoteProvisionedUserRepository.findByManageProvisioningIdAndUser(provisioning.getId(), user)
                    .ifPresent(remoteProvisionedUser -> {
                        map.add("id", remoteProvisionedUser.getRemoteIdentifier());
                        map.replace("dateFrom", List.of(GuestAccount.dateFrom(remoteProvisionedUser)));
                    });
        }
        return new RequestEntity(map, headers, HttpMethod.POST, URI.create(url));
    }

    private enum RequestType {
        create, update, delete
    }

}
