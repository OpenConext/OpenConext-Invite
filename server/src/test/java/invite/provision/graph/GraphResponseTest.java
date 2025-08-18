package invite.provision.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void graphResponse() throws JsonProcessingException {
        GraphResponse graphResponse = new GraphResponse(null, null, true);
        String json = objectMapper.writeValueAsString(graphResponse);
        Map<String, String> asMap = objectMapper.readValue(json, new TypeReference<>() {
        });

        assertTrue(asMap.containsKey("errorResponse"));
        assertTrue(asMap.containsKey("graphResponse"));
    }
}