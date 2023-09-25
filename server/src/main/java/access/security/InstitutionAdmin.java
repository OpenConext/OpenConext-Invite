package access.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "institution-admin")
@Getter
@Setter
public class InstitutionAdmin {

    private String entitlement;
    private String organizationGuidPrefix;

}
