package provisioning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    public Database database() {
        return new InMemoryDatabase();
    }


}
