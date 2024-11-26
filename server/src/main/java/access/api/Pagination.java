package access.api;

import access.model.User;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public class Pagination {

    private Pagination() {
    }

    static <T> ResponseEntity<Page<T>> of(Page<Map<String, Object>> page, List<T> content) {
        return ResponseEntity.ok(new PageImpl<>(content, page.getPageable(), page.getTotalElements()));
    }
}
