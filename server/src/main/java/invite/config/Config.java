package invite.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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
    private boolean performanceSeedAllowed;
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
        this.performanceSeedAllowed = base.performanceSeedAllowed;
        this.roleSearchRequired = base.roleSearchRequired;
        this.groupUrnPrefix = base.groupUrnPrefix;
        this.eduidIdpSchacHomeOrganization = base.eduidIdpSchacHomeOrganization;
    }

    public Config withAuthenticated(boolean authenticated) {
        this.setAuthenticated(authenticated);
        return this;
    }

    public Config withName(String name) {
        this.setName(name);
        return this;
    }

    public Config withMissingAttributes(List<String> missingAttributes) {
        this.setMissingAttributes(missingAttributes);
        return this;
    }

    public Config withGroupUrnPrefix(String groupUrnPrefix) {
        this.setGroupUrnPrefix(groupUrnPrefix);
        return this;
    }

}
