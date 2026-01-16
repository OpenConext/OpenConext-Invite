package invite.config;

import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HashGeneratorTest {

    @Test
    void generateRandomHash() {
        String defaultHash = HashGenerator.generateRandomHash();
        assertEquals(171, defaultHash.length());

        String hash = HashGenerator.generateRandomHash(128);
        assertEquals(171, hash.length());

        String encoded = URLEncoder.encode(defaultHash, Charset.defaultCharset());
        String decoded = URLDecoder.decode(encoded, Charset.defaultCharset());
        assertEquals(encoded, decoded);
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