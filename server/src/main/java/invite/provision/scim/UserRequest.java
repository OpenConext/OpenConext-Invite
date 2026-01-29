package invite.provision.scim;


import invite.model.User;
import invite.provision.Provisioning;
import invite.provision.ScimUserIdentifier;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * The SCIM client (e.g., the Invite server) generates and owns the externalId. It also supplies the userName,
 * whose uniqueness and semantics are enforced by the SCIM service provider (e.g. the educational institutions). The
 * SCIM service provider generates the immutable id and returns it in the response to a 'create' request. The SCIM
 * client stores this id and uses it in subsequent PUT or PATCH requests to address the user resource.
 *
 * The username which is used by the Invite server can be configured in Manage with the `scim_user_identifier`
 * attribute, and defaults to the EPPN.
 */
@Getter
public class UserRequest implements Serializable {

    private static final Log LOG = LogFactory.getLog(UserRequest.class);

    private final List<String> schemas = Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:User");
    private String externalId;
    private String userName;
    private final Name name;
    private String id;
    private final String displayName;
    private final boolean active = true;
    private final List<Email> emails;
    private final List<PhoneNumber> phoneNumbers;

    public UserRequest(User user, Provisioning provisioning) {
        String resolvedUserName = this.resolveUserName(user, provisioning);
        this.userName = resolvedUserName;
        this.externalId = resolvedUserName;
        this.name = new Name(user.getName(), user.getFamilyName(), user.getGivenName());
        this.displayName = user.getName();
        this.emails = List.of(new Email("other",user.getEmail()));
        //Add a default phone number for remote systems that require that
        this.phoneNumbers = Collections.singletonList(new PhoneNumber("+31600000000"));
        if (StringUtils.hasText(user.getInternalPlaceholderIdentifier())) {
            this.id = user.getInternalPlaceholderIdentifier();
        }
    }

    public UserRequest(User user, Provisioning provisioning, String remoteScimIdentifier) {
        this(user, provisioning);
        this.id = remoteScimIdentifier;
    }

    public void setInsitutionalEduID(String eduIDValue) {
        this.userName = eduIDValue;
        this.externalId = eduIDValue;
    }


    private String resolveUserName(User user, Provisioning provisioning) {
        ScimUserIdentifier scimUserIdentifier = provisioning.getScimUserIdentifier();
        //Backward compatibility for older Provisionings without default
        String defaultUserName = user.getEduPersonPrincipalName();
        if (scimUserIdentifier == null) {
            return defaultUserName;
        }
        boolean missingScimUserIdentifierValue = false;
        String configuredUserName = switch (scimUserIdentifier) {
            case subject_id -> {
                missingScimUserIdentifierValue = !StringUtils.hasText(user.getSubjectId());
                yield missingScimUserIdentifierValue ? defaultUserName : user.getSubjectId();
            }
            case uids -> {
                missingScimUserIdentifierValue = !StringUtils.hasText(user.getUid());
                yield missingScimUserIdentifierValue ? defaultUserName : user.getUid();
            }
            case email -> {
                missingScimUserIdentifierValue = !StringUtils.hasText(user.getEmail());
                yield missingScimUserIdentifierValue ? defaultUserName : user.getEmail();
            }
            case eduID -> {
                missingScimUserIdentifierValue = !StringUtils.hasText(user.getEduId());
                yield missingScimUserIdentifierValue ? defaultUserName : user.getEduId();
            }
            default -> defaultUserName;
        };
        if (missingScimUserIdentifierValue) {
            LOG.warn(String.format(
                    "Missing attribute %s for SCIM provisioning to %s for user %s. Return defaultExternalId %s",
                    scimUserIdentifier, provisioning.getEntityId(), user.getSub(), defaultUserName)
            );
        }
        return configuredUserName;
    }
}
