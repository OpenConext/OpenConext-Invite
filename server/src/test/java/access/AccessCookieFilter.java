package access;

import io.restassured.filter.cookie.CookieFilter;

public record AccessCookieFilter(CookieFilter cookieFilter, String apiURL) {}
