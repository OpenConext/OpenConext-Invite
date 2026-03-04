package invite.crm;

import invite.model.Organisation;

import java.util.List;
import java.util.Map;

public record Profile(String firstname,
                      String middlename,
                      String surname,
                      String email,
                      String mobile,
                      String idp,
                      String uid,
                      String guid,
                      Map<String, Object> organisation,
                      List<Authorisation> authorisations) {
}
