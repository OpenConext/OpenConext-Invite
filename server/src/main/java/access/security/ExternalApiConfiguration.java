package access.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "external-api-configuration")
@Getter
@Setter
public class ExternalApiConfiguration {

    private List<RemoteUser> remoteUsers;

}
