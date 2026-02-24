package invite.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class InvitationUniqueCrmOrganisationException extends AuthenticationException {

    public InvitationUniqueCrmOrganisationException(String msg) {
        super(msg);
    }
}
