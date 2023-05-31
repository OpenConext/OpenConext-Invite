package access.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConf {

    @Bean
    public MailBox mailBox(@Value("${email.from}") String emailFrom,
                           @Value("${email.base-url}") String baseUrl,
                           @Value("${email.enabled}") boolean enabled,
                           @Value("${email.environment}") String environment,
                           JavaMailSender mailSender) {
        return enabled ? new MailBox(mailSender, emailFrom, baseUrl, environment) : new MockMailBox(mailSender, emailFrom, baseUrl, environment);
    }


}
