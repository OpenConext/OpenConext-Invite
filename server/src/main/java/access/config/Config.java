package access.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "config")
@Getter
public class Config {

    private String clientUrl;
    private String serverUrl;
    private String eduidEntityId;
    private boolean authenticated;
    private String name;

    public Config withAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        return this;
    }

    public Config withName(String name) {
        this.name = name;
        return this;
    }

    public void setClientUrl(String clientUrl) {
        this.clientUrl = clientUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setEduidEntityId(String eduidEntityId) {
        this.eduidEntityId = eduidEntityId;
    }
}
