package invite.security;

import invite.model.Authority;
import invite.model.Invitation;
import invite.repository.InvitationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.function.Consumer;

public class AuthorizationRequestCustomizer implements Consumer<OAuth2AuthorizationRequest.Builder> {

    private final InvitationRepository invitationRepository;
    private final String eduidEntityId;

    public AuthorizationRequestCustomizer(InvitationRepository invitationRepository, String eduidEntityId) {
        this.invitationRepository = invitationRepository;
        this.eduidEntityId = eduidEntityId;
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
                    if (invitation.isEduIDOnly() && invitation.getIntendedAuthority().equals(Authority.GUEST)) {
                        params.put("login_hint", eduidEntityId);
                    }
                });
            }
        });
    }
}
