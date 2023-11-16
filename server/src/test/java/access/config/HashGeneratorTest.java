package access.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashGeneratorTest {

    @Test
    void generateHash() {
        String hash = HashGenerator.generateHash();
        assertTrue(hash.length() > 172);
    }

    @Test
    void generateToken() {
        String token = HashGenerator.generateToken();
        assertEquals(36, token.length());

        String hashToken = HashGenerator.hashToken(token);
        String hashTokenCheck = HashGenerator.hashToken(token);
        assertEquals(hashToken, hashTokenCheck);
        assertEquals(64, hashToken.length());
    }

}