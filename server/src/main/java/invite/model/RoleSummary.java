package invite.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class RoleSummary {

    private Long id;
    private String roleName;
    private String authority;
    private Instant endDate;

}
