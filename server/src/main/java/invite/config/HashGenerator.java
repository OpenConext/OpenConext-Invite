package invite.config;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

public class HashGenerator {

    private static final Random secureRandom = new SecureRandom();
    public static final DigestUtils digestUtils = new DigestUtils("SHA3-256");

    private HashGenerator() {
    }

    public static String generateRandomHash() {
        return generateRandomHash(128);
    }

    public static String generateRandomHash(int byteLength) {
        byte[] aesKey = new byte[byteLength];
        secureRandom.nextBytes(aesKey);
        //Avoid decoding / encoding as URL parameter problems
        return Base64.getUrlEncoder().withoutPadding().encodeToString(aesKey);
    }

    public static String generateToken() {
        return RandomStringUtils.secure().next(36, true, true);
    }

    public static String hashToken(String token) {
        return digestUtils.digestAsHex(token);
    }

}
