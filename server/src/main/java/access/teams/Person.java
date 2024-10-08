package access.teams;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Person implements Serializable {

    private String urn;
    private String name;
    private String email;
    private String schacHomeOrganization;
    private Instant created;
}
