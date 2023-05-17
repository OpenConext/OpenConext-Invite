package access.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

public class HashGenerator {

    private static final Random secureRandom = new SecureRandom();

    private HashGenerator() {
    }

    public static String generateHash() {
        byte[] aesKey = new byte[128];
        secureRandom.nextBytes(aesKey);
        String base64 = Base64.getEncoder().encodeToString(aesKey);
        return URLEncoder.encode(base64, StandardCharsets.UTF_8).replaceAll("%", "");
    }

}
