package invite.crm;

import invite.model.Organisation;

import java.util.List;

public record Profile(String firstname,
                      String middlename,
                      String surname,
                      String email,
                      String mobile,
                      String idp,
                      String uid,
                      String guid,
                      Organisation organisation,
                      List<Authorisation> authorisations) {
}
