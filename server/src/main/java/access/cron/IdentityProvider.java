package access.cron;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class IdentityProvider implements Serializable {

    private String displayNameEn;
    private String displayNameNl;
    private String logoUrl;

    public String getName() {
        String language = LocaleContextHolder.getLocale().getLanguage();
        if ("en".equalsIgnoreCase(language)) {
            return StringUtils.hasText(displayNameEn) ? displayNameEn : displayNameNl;
        }
        return StringUtils.hasText(displayNameNl) ? displayNameNl : displayNameEn;
    }
}
