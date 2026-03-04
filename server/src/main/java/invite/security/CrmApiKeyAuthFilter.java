package invite.security;

import invite.exception.UserRestrictionException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static invite.security.SecurityConfig.API_KEY_HEADER;

public class CrmApiKeyAuthFilter extends OncePerRequestFilter {

    private final String crmApiKeyHeader;
    private final SecurityContextRepository securityContextRepository;

    public CrmApiKeyAuthFilter(String crmApiKeyHeader,
                               SecurityContextRepository securityContextRepository) {
        this.crmApiKeyHeader = crmApiKeyHeader;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String headerValue = request.getHeader(API_KEY_HEADER);

        if (crmApiKeyHeader.equals(headerValue)) {
            // Build an Authentication with ROLE_CRM granted authority
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Scope.crm.name().toUpperCase()));
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken("crm-client", null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            //New in Spring security 6.x,
            // See https://docs.spring.io/spring-security/reference/5.8/migration/servlet/session-management.html#_require_explicit_saving_of_securitycontextrepository
            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
        } else {
            throw new UserRestrictionException("Wrong or absent API-KEY header");
        }


        filterChain.doFilter(request, response);
    }
}
