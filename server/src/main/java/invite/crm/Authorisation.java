package invite.crm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Authorisation(@JsonProperty("short") String abbbrevationn, String role) {
}
