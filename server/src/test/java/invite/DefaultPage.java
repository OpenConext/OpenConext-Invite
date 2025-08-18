package invite;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DefaultPage<T> {

    private List<T> content;
    private Pageable pageable;

    private int totalElements;
    private boolean last;
    private int number;
    private int numberOfElements;
}
