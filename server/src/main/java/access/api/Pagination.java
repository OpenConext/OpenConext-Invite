package access.api;

import access.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class Pagination {

    private Pagination() {
    }

    static <T> ResponseEntity<Page<T>> of(List<T> content, String sortDirection, String sort) {
        PageRequest pageRequest = PageRequest.of(0, content.size(), Sort.by(Sort.Direction.fromString(sortDirection), sort));
        return ResponseEntity.ok(new PageImpl<T>(content, pageRequest, content.size()));
    }
}
