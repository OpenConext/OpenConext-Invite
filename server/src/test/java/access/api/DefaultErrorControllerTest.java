package access.api;

import access.exception.ExtendedErrorAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultErrorControllerTest {

    private final DefaultErrorController errorController = new DefaultErrorController(new ExtendedErrorAttributes());

    @Test
    void error() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<Map<String, Object>> responseEntity = errorController.error(request);
        Map<String, Object> body = responseEntity.getBody();
        assertEquals(500, body.get("status"));
    }

}