package invite.crm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConnectionStatus(@JsonProperty("org_guid") String organisationId, boolean connected) {
}
