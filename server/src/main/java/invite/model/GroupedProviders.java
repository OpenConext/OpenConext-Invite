package invite.model;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
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

    public String getLogo() {
        return (String) provider.get("logo");
    }

    private String getAttribute(String name) {
        String[] languages = preferredLanguageWithFallback();
        return (String) provider.getOrDefault(name + languages[0], provider.get(name + languages[1]));
    }


    private String[] preferredLanguageWithFallback() {
        String language = LocaleContextHolder.getLocale().getLanguage();
        return new String[]{language, language.equals("en") ? "nl" : "en"};
    }

}
