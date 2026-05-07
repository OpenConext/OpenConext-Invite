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
                                       Map<String, Object> organisation,
                                       Map<String, Object> link,
                                       String status,
                                       @JsonProperty("status_code")  String statusCode) {
}
