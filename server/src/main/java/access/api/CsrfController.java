package access.api;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller is used to get the CSRF token from the server.
 * See https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-mobile
 */
@RestController
public class CsrfController {

    @GetMapping("/api/v1/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }
}
