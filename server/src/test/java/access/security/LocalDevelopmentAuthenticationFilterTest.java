package access.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class LocalDevelopmentAuthenticationFilterTest {

    private final LocalDevelopmentAuthenticationFilter authenticationFilter = new LocalDevelopmentAuthenticationFilter();

    @Test
    void doFilter() throws ServletException, IOException {
        ServletRequest request = new MockHttpServletRequest();
        ServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();
        authenticationFilter.doFilter(request, response, filterChain);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
    }
}