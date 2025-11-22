package invite.security;

import invite.manage.EntityType;
import invite.manage.Manage;
import invite.model.*;
import invite.repository.InvitationRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.when;

class AuthorizationRequestCustomizerTest {

    private final InvitationRepository invitationRepository = Mockito.mock(InvitationRepository.class);

    private final Manage manage = Mockito.mock(Manage.class);

    private final AuthorizationRequestCustomizer customizer =
            new AuthorizationRequestCustomizer(invitationRepository, "eduid-entity-id", manage);

    @Test
    void testForceParameterAddsPromptLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("force", "true");
        DefaultSavedRequest savedRequest = new DefaultSavedRequest(request);

        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_SAVED_REQUEST", savedRequest);

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://auth")
                .clientId("client");

        customizer.accept(builder);
        OAuth2AuthorizationRequest requestResult = builder.build();
        assertEquals("login", requestResult.getAdditionalParameters().get("prompt"));
    }

    @Test
    void testHashParameterGuestEduIdOnlyAddsLoginHint() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String hash = "abc123";
        request.setParameter("hash", hash);
        DefaultSavedRequest savedRequest = new DefaultSavedRequest(request);
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_SAVED_REQUEST", savedRequest);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Invitation invitation = new Invitation();
        invitation.setIntendedAuthority(Authority.MANAGER);
        invitation.setEduIDOnly(true);
        when(invitationRepository.findByHash(hash)).thenReturn(Optional.of(invitation));

        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://auth")
                .clientId("client");

        customizer.accept(builder);
        OAuth2AuthorizationRequest requestResult = builder.build();
        assertEquals("eduid-entity-id", requestResult.getAdditionalParameters().get("login_hint"));
    }

    @Test
    void testACRParameterGuestEduIdOnly() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String hash = "abc123";
        request.setParameter("hash", hash);
        DefaultSavedRequest savedRequest = new DefaultSavedRequest(request);
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_SAVED_REQUEST", savedRequest);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Invitation invitation = new Invitation();
        invitation.setIntendedAuthority(Authority.GUEST);
        invitation.setEduIDOnly(true);
        invitation.setRequestedAuthnContext(RequestedAuthnContext.EduIDLinkedInstitution);
        when(invitationRepository.findByHash(hash)).thenReturn(Optional.of(invitation));

        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://auth")
                .clientId("client");

        customizer.accept(builder);
        OAuth2AuthorizationRequest requestResult = builder.build();
        assertEquals("eduid-entity-id", requestResult.getAdditionalParameters().get("login_hint"));
        assertEquals(RequestedAuthnContext.EduIDLinkedInstitution.getUrl(), requestResult.getAdditionalParameters().get("acr_values"));
    }

    @Test
    void testHashParameterGuestNonEduIdOnlyAddsIdpListLoginHint() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String hash = "abc123";
        request.setParameter("hash", hash);
        DefaultSavedRequest savedRequest = new DefaultSavedRequest(request);

        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_SAVED_REQUEST", savedRequest);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Application application = new Application("manage-id", EntityType.SAML20_SP);
        ApplicationUsage appUsage = new ApplicationUsage(application, "https://landing.com");

        Role role = new Role();
        role.setApplicationUsages(Set.of(appUsage));

        Invitation invitation = new Invitation();
        invitation.setIntendedAuthority(Authority.GUEST);
        invitation.setRoles(Set.of(new InvitationRole(role)));
        when(invitationRepository.findByHash(hash)).thenReturn(Optional.of(invitation));

        Map<String, Object> providerData = Map.of("entityid", "idp-entity");
        when(manage.providerById(application.getManageType(), application.getManageId())).thenReturn(providerData);
        when(manage.idpEntityIdentifiersByServiceEntityId(anyList()))
                .thenReturn(List.of("idp-entity-1", "idp-entity-2"));

        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://auth")
                .clientId("client");

        customizer.accept(builder);
        OAuth2AuthorizationRequest requestResult = builder.build();

        assertEquals("idp-entity-1,idp-entity-2",
                requestResult.getAdditionalParameters().get("login_hint"));
    }
}