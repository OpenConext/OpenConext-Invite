package access.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class Results {

    private Results() {
    }

    public static ResponseEntity<Map<String, Integer>> createResult() {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", HttpStatus.CREATED.value()));
    }

    public static ResponseEntity<Map<String, Integer>> okResult() {
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("status", HttpStatus.OK.value()));
    }

    public static ResponseEntity<Void> deleteResult() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
