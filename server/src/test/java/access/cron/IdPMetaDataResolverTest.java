package access.cron;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class IdPMetaDataResolverTest {

    @Test
    public void resolveIdpMetaDataNoException() {
        new IdPMetaDataResolver(null).resolveIdpMetaData();
    }

    @Test
    public void resolveIdpMetaDataNoExceptionFileNotFound() {
        new IdPMetaDataResolver(new ClassPathResource("metadata/nope")).resolveIdpMetaData();
    }

    @Test
    public void resolveIdentityProvider() {
        IdPMetaDataResolver metaDataResolver = new IdPMetaDataResolver(new ClassPathResource("metadata/idps-metatdata-prod.xml"));
        List<String> schacHomes = Arrays.asList("student.ahk.nl", "ahknl.onmicrosoft.com", "ahk.nl");
        schacHomes.forEach(schacHome -> {
            IdentityProvider identityProvider = metaDataResolver.getIdentityProvider(schacHome).get();

            assertEquals("Amsterdam University of the Arts", identityProvider.getDisplayNameEn());
            assertEquals("Amsterdamse Hogeschool voor de Kunsten", identityProvider.getDisplayNameNl());
            assertEquals("https://static.surfconext.nl/media/idp/ahk-logo.png", identityProvider.getLogoUrl());
        });

    }
}