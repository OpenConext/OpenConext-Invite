package access.manage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ManageConf {

    @Bean
    public Manage manage(@Value("${manage.url}") String url,
                         @Value("${manage.user}") String user,
                         @Value("${manage.password}") String password,
                         @Value("${manage.enabled}") boolean enabled,
                                 ObjectMapper objectMapper) {
        return enabled ? new RemoteManage(url, user, password, objectMapper) : new LocalManage(objectMapper);
    }


}
