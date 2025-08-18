package invite.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
