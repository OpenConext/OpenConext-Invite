package access.provision.scim;


import access.model.User;
import lombok.Getter;

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

    public UserRequest(User user) {
        this.externalId = user.getEduPersonPrincipalName();
        this.userName = user.getEduPersonPrincipalName();
        this.name = new Name(user.getName(), user.getFamilyName(), user.getGivenName());
        this.displayName = user.getName();
        this.emails = Collections.singletonList(new Email(user.getEmail()));
    }

    public UserRequest(User user, String remoteScimIdentifier) {
        this(user);
        this.id = remoteScimIdentifier;
    }
}
