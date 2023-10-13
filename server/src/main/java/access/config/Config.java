package access.config;

import access.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "config")
@Getter
@Setter
@NoArgsConstructor
public class Config {

    private String clientUrl;
    private String welcomeUrl;
    private String serverUrl;
    private String serverWelcomeUrl;
    private String eduidEntityId;
    private boolean roleSearchRequired;
    private boolean pastDateAllowed;
    private String groupUrnPrefix;
    private boolean authenticated;
    private String name;
    private String eduidIdpSchacHomeOrganization;
    private List<String> missingAttributes;

    public Config(Config base) {
        this.clientUrl = base.clientUrl;
        this.welcomeUrl = base.welcomeUrl;
        this.serverUrl = base.serverUrl;
        this.serverWelcomeUrl = base.serverWelcomeUrl;
        this.eduidEntityId = base.eduidEntityId;
        this.pastDateAllowed = base.pastDateAllowed;
        this.roleSearchRequired = base.roleSearchRequired;
        this.groupUrnPrefix = base.groupUrnPrefix;
        this.eduidIdpSchacHomeOrganization = base.eduidIdpSchacHomeOrganization;
    }

    public Config withAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        return this;
    }

    public Config withName(String name) {
        this.name = name;
        return this;
    }

    public Config withMissingAttributes(List<String> missingAttributes) {
        this.missingAttributes = missingAttributes;
        return this;
    }

    public Config withGroupUrnPrefix(String groupUrnPrefix) {
        this.groupUrnPrefix = groupUrnPrefix;
        return this;
    }

}
