package access.cron;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class IdentityProvider implements Serializable {

    private String displayNameEn;
    private String displayNameNl;
    private String logoUrl;

    public String getName() {
        String language = LocaleContextHolder.getLocale().getLanguage();
        if ("en".equals(language.toLowerCase())) {
            return StringUtils.hasText(displayNameEn) ? displayNameEn : displayNameNl;
        }
        return StringUtils.hasText(displayNameNl) ? displayNameNl : displayNameEn;
    }
}
