package invite.crm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SendInvitation(@JsonProperty("org_id")  String crmOrganisationId,
                             @JsonProperty("guid")  String crmContatcId,
                             String email,
                             List<CRMRole> roles) {
}
