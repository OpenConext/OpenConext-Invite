package invite;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Pageable {

    private int pageNumber;
    private int pageSize;
    private int offset;

}
