package access.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@Configuration
@ConditionalOnProperty(value = "spring.session.enabled", havingValue = "true", matchIfMissing = false)
@EnableJdbcHttpSession
public class HttpSessionConfig {
}
