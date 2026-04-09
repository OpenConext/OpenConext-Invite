package invite.cron;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Optional;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {

    @Bean
    @Profile("!test")
    public LockProvider lockProvider(DataSource dataSource) {
        LockProvider delegate = new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime() // Use DB time, not app-node time — avoids clock skew
                        .build()
        );
        return new DeadlockRetryLockProvider(delegate);
    }

    @Bean
    @Profile("test")
    public LockProvider noOpLockProvider() {
        return lockConfiguration -> Optional.of(() -> {});
    }
}
