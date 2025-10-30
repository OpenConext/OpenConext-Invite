package invite.security;

import invite.manage.Manage;
import invite.model.ApplicationUsage;
import invite.model.Authority;
import invite.model.Invitation;
import invite.repository.InvitationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuthorizationRequestCustomizer implements Consumer<OAuth2AuthorizationRequest.Builder> {

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
                    boolean guestInvitation = invitation.getIntendedAuthority().equals(Authority.GUEST);
                    if (invitation.isEduIDOnly() && guestInvitation) {
                        params.put("login_hint", eduidEntityId);
                    } else if (!invitation.isEduIDOnly() && guestInvitation) {
                        //Fetch all IdentityProviders that have one the manage role applications in their allowList
                        // First, get all entity identifiers of the applications connected to the roles of the invitation
                        List<String> entityIdentifiers = invitation.getRoles().stream()
                                .map(role -> role.getRole().getApplicationUsages())
                                .flatMap(Collection::stream)
                                .map(applicationUsage -> applicationUsage.getApplication())
                                .map(application -> manage.providerById(application.getManageType(), application.getManageId()))
                                .filter(provider -> !CollectionUtils.isEmpty(provider))
                                .map(provider -> (String) ((Map) provider.get("data")).get("entityid"))
                                .distinct()
                                .toList();
                        //Now get all entityIdentifiers of the IdP's
                        List<String> idpList = manage.idpEntityIdentifiersByServiceEntityId(entityIdentifiers);
                        params.put("login_hint", idpList.stream().collect(Collectors.joining(",")));
                    }
                });
            }
        });
    }
}
