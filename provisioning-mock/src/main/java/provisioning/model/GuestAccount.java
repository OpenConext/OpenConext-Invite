package provisioning.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class GuestAccount {

    private String id;
    private String name;
    private String email;
    private String dateFrom;
    private String dateTill;
    private String notifyByEmail;
    private String preferredLanguage;
}


