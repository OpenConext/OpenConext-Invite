package invite.crm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ConnectionStatusResponse(String fullname,
                                       String email,
                                       Map<String, String> link,
                                       String status,
                                       @JsonProperty("status_code")  String statusCode) {
}
