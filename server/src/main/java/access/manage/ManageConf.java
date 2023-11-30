package access.manage;


import com.fasterxml.jackson.databind.ObjectMapper;
import crypto.KeyStore;
import crypto.RSAKeyStore;
import io.micrometer.core.instrument.util.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
@Configuration
public class ManageConf {

    @Bean
    public Manage manage(@Value("${manage.url}") String url,
                         @Value("${manage.user}") String user,
                         @Value("${manage.password}") String password,
                         @Value("${manage.enabled}") boolean enabled,
                         @Value("${manage.local}") boolean local,
                                 ObjectMapper objectMapper) throws IOException {
        return enabled ? new RemoteManage(url, user, password, objectMapper) : new LocalManage(objectMapper, local);
    }

    @Bean
    public KeyStore keyStore(@Value("${crypto.development-mode}") Boolean developmentMode,
                             @Value("${crypto.private-key-location}") Resource privateKey) throws IOException {
        return developmentMode ? new RSAKeyStore() : new RSAKeyStore(IOUtils.toString(privateKey.getInputStream()));
    }


}
