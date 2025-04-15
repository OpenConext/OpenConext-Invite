package access.cron;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentityProviderTest {

    @Test
    void getName() {
        IdentityProvider identityProvider = new IdentityProvider(null, "nl", null, "logo");
        assertEquals("nl", identityProvider.getName());

        LocaleContextHolder.setLocale(Locale.forLanguageTag("nl"));
        identityProvider = new IdentityProvider("en", "nl", "pt", "logo");
        assertEquals("nl", identityProvider.getName());

        LocaleContextHolder.setLocale(Locale.forLanguageTag("pt"));
        identityProvider = new IdentityProvider("en", "nl", "pt", "logo");
        assertEquals("pt", identityProvider.getName());
        //Put back the locale, otherwise the mail tests become flakey
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }
}