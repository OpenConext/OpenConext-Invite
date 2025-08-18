package invite.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationInfo {

    private String landingPage;
    private String nameEn;
    private String nameNl;
    private String organisationEn;
    private String organisationNl;
    private String logo;
}
