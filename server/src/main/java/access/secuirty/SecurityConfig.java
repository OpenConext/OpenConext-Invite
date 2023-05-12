package access.secuirty;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@EnableScheduling
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain oidcngSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeHttpRequests()
                .requestMatchers("/", "/api/v1/users/config", "ui/**")
                .permitAll()
                .requestMatchers("/api/**")
                .authenticated()
                .and()
                .oauth2Login(withDefaults());
        return http.build();
    }
}
