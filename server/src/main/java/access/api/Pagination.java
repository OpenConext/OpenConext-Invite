package access.api;

import access.model.User;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public class Pagination {

    private Pagination() {
    }

    static <T> ResponseEntity<Page<T>> of(List<T> content, String sortDirection, String sort) {
        PageRequest pageRequest = PageRequest.of(0, content.size(), Sort.by(Sort.Direction.fromString(sortDirection), sort));
        return ResponseEntity.ok(new PageImpl<>(content, pageRequest, content.size()));
    }

    static <T> ResponseEntity<Page<T>> of(Page<Map<String, Object>> page, List<T> content) {
        return ResponseEntity.ok(new PageImpl<>(content, page.getPageable(), page.getTotalElements()));
    }
}
