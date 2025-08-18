package invite;

import io.restassured.filter.cookie.CookieFilter;
import org.springframework.security.web.csrf.CsrfToken;

public record AccessCookieFilter(CookieFilter cookieFilter, String apiURL, CsrfToken csrfToken) {
}
