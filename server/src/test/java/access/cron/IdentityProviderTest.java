package access.cron;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class IdentityProviderTest {

    @Test
    void getName() {
        IdentityProvider identityProvider = new IdentityProvider(null, "nl", "logo");
        assertEquals("nl", identityProvider.getName());

        LocaleContextHolder.setLocale(Locale.forLanguageTag("nl"));
        identityProvider = new IdentityProvider("en", "nl", "logo");
        assertEquals("nl", identityProvider.getName());
    }
}