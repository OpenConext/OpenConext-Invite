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
@NoArgsConstructor
public class Config {

    private String clientUrl;
    private String welcomeUrl;
    private String serverUrl;
    private String serverWelcomeUrl;
    private String eduidEntityId;
    private boolean roleSearchRequired;
    private String groupUrnPrefix;
    private boolean authenticated;
    private String name;
    private List<String> missingAttributes;

    public Config(Config base) {
        this.clientUrl = base.clientUrl;
        this.welcomeUrl = base.welcomeUrl;
        this.serverUrl = base.serverUrl;
        this.serverWelcomeUrl = base.serverWelcomeUrl;
        this.eduidEntityId = base.eduidEntityId;
        this.roleSearchRequired = base.roleSearchRequired;
        this.groupUrnPrefix = base.groupUrnPrefix;
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

    public void setRoleSearchRequired(boolean roleSearchRequired) {
        this.roleSearchRequired = roleSearchRequired;
    }

    public void setClientUrl(String clientUrl) {
        this.clientUrl = clientUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setServerWelcomeUrl(String serverWelcomeUrl) {
        this.serverWelcomeUrl = serverWelcomeUrl;
    }

    public void setEduidEntityId(String eduidEntityId) {
        this.eduidEntityId = eduidEntityId;
    }

    public void setWelcomeUrl(String welcomeUrl) {
        this.welcomeUrl = welcomeUrl;
    }
}
