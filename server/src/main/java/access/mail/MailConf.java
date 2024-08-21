package access.mail;

import access.config.Config;
import access.cron.IdPMetaDataResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(Config.class)
public class MailConf {

    @Bean
    public MailBox mailBox(Config config,
                           @Value("${email.from}") String emailFrom,
                           @Value("${email.contactEmail}") String contactEmail,
                           @Value("${email.environment}") String env,
                           IdPMetaDataResolver idPMetaDataResolver,
                           JavaMailSender mailSender,
                           ObjectMapper objectMapper) throws IOException {
        return new MailBox(objectMapper, idPMetaDataResolver, mailSender, emailFrom, contactEmail, config.getClientUrl(), config.getWelcomeUrl(), env);
    }


}
