package access.provision.scim;


import access.model.User;
import access.provision.Provisioning;
import access.provision.ScimUserIdentifier;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
public class UserRequest implements Serializable {

    private final List<String> schemas = Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:User");
    private final String externalId;
    private final String userName;
    private final Name name;
    private String id;
    private final String displayName;
    private final List<Email> emails;

    public UserRequest(User user, Provisioning provisioning) {
        this.externalId = this.resolveExternalId(user, provisioning);
        this.userName = user.getEduPersonPrincipalName();
        this.name = new Name(user.getName(), user.getFamilyName(), user.getGivenName());
        this.displayName = user.getName();
        this.emails = Collections.singletonList(new Email(user.getEmail()));
    }

    public UserRequest(User user, Provisioning provisioning, String remoteScimIdentifier) {
        this(user, provisioning);
        this.id = remoteScimIdentifier;
    }

    private String resolveExternalId(User user, Provisioning provisioning) {
        ScimUserIdentifier scimUserIdentifier = provisioning.getScimUserIdentifier();
        //Backward compatibility for older Provisionings without default
        String defaultExternalId = user.getEduPersonPrincipalName();
        if (scimUserIdentifier == null) {
            return defaultExternalId;
        }
        return switch (scimUserIdentifier) {
            case subject_id -> StringUtils.hasText(user.getSubjectId()) ? user.getSubjectId() : defaultExternalId;
            case uids -> StringUtils.hasText(user.getUid()) ? user.getUid() : defaultExternalId;
            case email -> StringUtils.hasText(user.getEmail()) ? user.getEmail() : defaultExternalId;
            case eduID -> StringUtils.hasText(user.getEduId()) ? user.getEduId() : defaultExternalId;
            default -> defaultExternalId;
        };
    }
}
