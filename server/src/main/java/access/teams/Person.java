package access.teams;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Person implements Serializable {

    private String urn;
    private String name;
    private String email;
    private String schacHomeOrganization;
}
