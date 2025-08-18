package invite.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "super-admin")
@Getter
@Setter
public class SuperAdmin {

    private List<String> users;

}
