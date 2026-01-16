package invite.security;

import invite.manage.Manage;
import invite.manage.ManageIdentifier;
import invite.model.Authority;
import invite.model.Invitation;
import invite.repository.InvitationRepository;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AuthorizationRequestCustomizer implements Consumer<OAuth2AuthorizationRequest.Builder> {

    private static final Log LOG = LogFactory.getLog(AuthorizationRequestCustomizer.class);

    private final InvitationRepository invitationRepository;
    private final String eduidEntityId;
    private final Manage manage;

    public AuthorizationRequestCustomizer(InvitationRepository invitationRepository,
                                          String eduidEntityId,
                                          Manage manage) {
        this.invitationRepository = invitationRepository;
        this.eduidEntityId = eduidEntityId;
        this.manage = manage;
    }

    @Override
    public void accept(OAuth2AuthorizationRequest.Builder builder) {
        builder.additionalParameters(params -> {
            RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
            HttpSession session = ((ServletRequestAttributes) requestAttributes)
                    .getRequest().getSession(false);
            if (session == null) {
                return;
            }
            DefaultSavedRequest savedRequest = (DefaultSavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
            String[] force = savedRequest.getParameterValues("force");
            if (force != null && force.length == 1) {
                params.put("prompt", "login");
            }
            String[] hash = savedRequest.getParameterValues("hash");
            if (hash != null && hash.length == 1) {
                Optional<Invitation> optionalInvitation = invitationRepository.findByHash(hash[0]);
                optionalInvitation.ifPresent(invitation -> {
                    if (invitation.isEduIDOnly() && invitation.getRequestedAuthnContext() != null) {
                        String url = invitation.getRequestedAuthnContext().getUrl();

                        LOG.debug("Adding acr_values to authorization request query parameters: " + url);

                        params.put("acr_values", url);
                    }
                    boolean guestInvitation = invitation.getIntendedAuthority().equals(Authority.GUEST);
                    if (invitation.isEduIDOnly()) {
                        LOG.debug("Adding login_hint to authorization request query parameters: " + eduidEntityId);

                        params.put("login_hint", eduidEntityId);
                    } else if (guestInvitation) {
                        //Fetch all IdentityProviders that have one of the manage role applications in their allowList
                        //We only want IdP's that have access to ALL the applications of the invitation
                        Set<ManageIdentifier> manageIdentifiers = invitation.getRoles().stream()
                                .map(role -> role.getRole().getApplicationUsages().stream()
                                        .map(applicationUsage -> new ManageIdentifier(
                                                applicationUsage.getApplication().getManageId(),
                                                applicationUsage.getApplication().getManageType()

                                        )).collect(Collectors.toSet()))
                                .reduce((set1, set2) -> {
                                    set1.retainAll(set2);
                                    return set1;
                                })
                                .orElse(Set.of());
                        List<String> entityIdentifiers = manageIdentifiers.stream()
                                .map(manageIdentifier -> manage.providerById(manageIdentifier.manageType(), manageIdentifier.manageId()))
                                .filter(provider -> !CollectionUtils.isEmpty(provider))
                                .map(provider -> (String) provider.get("entityid"))
                                .distinct()
                                .toList();
                        //Now get all entityIdentifiers of the IdP's
                        List<String> idpList = manage.idpEntityIdentifiersByServiceEntityId(entityIdentifiers);
                        //to avoid HeadersTooLargeException, IdP scoping for more than 20 IdentityProviders is not helpful
                        String loginHint = String.join(",", idpList);
                        // Default Tomcat maxHttpHeaderSize is 8KB (8192 bytes), but we need some buffer for other headers
                        int loginHintSize = loginHint.getBytes(Charset.defaultCharset()).length;
                        if (loginHintSize < 6000) {
                            LOG.debug("Adding login_hint to authorization request query parameters: " + loginHint);
                            params.put("login_hint", loginHint);
                        } else {
                            LOG.warn("Not adding login_hint to authorization request query parameters as the size is to large: " + loginHintSize);
                        }
                    }
                });
            }
        });
    }
}
