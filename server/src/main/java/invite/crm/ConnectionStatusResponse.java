package invite.crm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ConnectionStatusResponse(String guid,
                                       String firstname,
                                       String middlename,
                                       String surname,
                                       String fullname,
                                       String email,
                                       String mobile,
                                       Map<String, String> organisation,
                                       Map<String, String> link,
                                       String status,
                                       @JsonProperty("status_code")  String statusCode) {
}
