package invite.crm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RemoveRoles(@JsonProperty("org_id")  String crmOrganisationId,
                          @JsonProperty("guid")  String crmContatcId) {
}
