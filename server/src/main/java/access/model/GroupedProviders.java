package access.model;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public record GroupedProviders(Map<String, Object> provider, List<Role> roles, String logoName) {

    public String getName() {
        return getAttribute("name:");
    }

    public String getOrganisation() {
        return getAttribute("OrganizationName:");
    }

    public String getRolesDisplay() {
        return roles.stream().map(role -> role.getName()).collect(Collectors.joining(", "));
    }

    private Map<String, Object> metaDataFields() {
        return (Map<String, Object>) ((Map) provider.get("data")).get("metaDataFields");
    }

    public String getLogo() {
        Map<String, Object> metaData = metaDataFields();
        return (String) metaData.get("logo:0:url");
    }

    private String getAttribute(String name) {
        Map<String, Object> metaData = metaDataFields();
        String[] languages = preferredLanguageWithFallback();
        return (String) metaData.getOrDefault(name + languages[0], metaData.get(name + languages[1]));
    }


    private String[] preferredLanguageWithFallback() {
        String language = LocaleContextHolder.getLocale().getLanguage();
        return new String[]{language, language.equals("en") ? "nl" : "en"};
    }

}
