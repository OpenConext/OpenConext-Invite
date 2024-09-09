package access.provision.scim;


import access.api.RoleController;
import access.model.User;
import access.provision.Provisioning;
import access.provision.ScimUserIdentifier;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
public class UserRequest implements Serializable {

    private static final Log LOG = LogFactory.getLog(UserRequest.class);

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
        boolean missingScimUserIdentifierValue = false;
        String externalIdIdentifier = switch (scimUserIdentifier) {
            case subject_id -> {
                missingScimUserIdentifierValue = !StringUtils.hasText(user.getSubjectId());
                yield missingScimUserIdentifierValue ? defaultExternalId : user.getSubjectId();
            }
            case uids -> {
                missingScimUserIdentifierValue = !StringUtils.hasText(user.getUid());
                yield missingScimUserIdentifierValue ? defaultExternalId : user.getUid();
            }
            case email -> {
                missingScimUserIdentifierValue = !StringUtils.hasText(user.getEmail());
                yield missingScimUserIdentifierValue ? defaultExternalId : user.getEmail();
            }
            case eduID -> {
                missingScimUserIdentifierValue = !StringUtils.hasText(user.getEduId());
                yield missingScimUserIdentifierValue ? defaultExternalId : user.getEduId();
            }
            default -> defaultExternalId;
        };
        if (missingScimUserIdentifierValue) {
            LOG.warn(String.format(
                    "Missing attribute %s for SCIM provisioning to %s for user %s. Return defaultExternalId %s",
                    scimUserIdentifier, provisioning.getEntityId(), user.getSub(), defaultExternalId)
            );
        }
        return externalIdIdentifier;
    }
}
