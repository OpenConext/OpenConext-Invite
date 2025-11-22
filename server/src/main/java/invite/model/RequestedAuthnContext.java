package invite.model;

import lombok.Getter;

public enum RequestedAuthnContext {

    EduIDLinkedInstitution("https://eduid.nl/trust/linked-institution"),
    EduIDValidatedName("https://eduid.nl/trust/validate-names"),
    ValidateNamesExternal("https://eduid.nl/trust/validate-names-external"),
    EduIDRequireStudentAffiliation("https://eduid.nl/trust/affiliation-student"),
    TransparentAuthnContext("transparent_authn_context");

    @Getter
    private final String url;

    RequestedAuthnContext(String url) {
        this.url = url;
    }


}
