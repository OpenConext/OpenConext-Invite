package access.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConf {

    @Bean
    public MailBox mailBox(@Value("${email.from}") String emailFrom,
                           @Value("${email.base-url}") String baseUrl,
                           @Value("${email.welcome-url}") String welcomeUrl,
                           @Value("${email.enabled}") boolean enabled,
                           @Value("${email.environment}") String environment,
                           JavaMailSender mailSender,
                           ObjectMapper objectMapper) throws IOException {
        return enabled ? new MailBox(objectMapper, mailSender, emailFrom, baseUrl, welcomeUrl, environment) :
                new MockMailBox(objectMapper, mailSender, emailFrom, baseUrl, welcomeUrl, environment);
    }


}
