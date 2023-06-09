package access.provision.scim;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class Member implements Serializable {

    private final String value;

}
