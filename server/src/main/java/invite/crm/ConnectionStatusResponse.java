package invite.crm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConnectionStatusResponse(String fullname,
                                       String email,
                                       String idp,
                                       String uid,
                                       String status,
                                       @JsonProperty("status_code")  String statusCode) {
}
