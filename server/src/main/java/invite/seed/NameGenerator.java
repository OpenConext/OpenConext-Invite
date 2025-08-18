package invite.seed;


import java.util.Locale;
import java.util.Random;

public class NameGenerator {

    private NameGenerator() {
    }

    private static final Random random = new Random();

    private static final String[] consonants = new String[]{
            "al", "an", "ar", "as", "at", "ea", "ed", "en", "er", "es", "ha", "he", "hi", "in", "is", "it",
            "le", "me", "nd", "ne", "ng", "nt", "on", "or", "ou", "re", "se", "st", "te", "th", "ti", "to",
            "ve", "wa", "it"
    };
    private static final String[] vowels = new String[]{"a", "e", "i", "o", "u", "y"};


    public static String generate() {
        int baseMaxLength = 18;
        int maxLength = random.nextInt((int) (baseMaxLength * 0.5), baseMaxLength + 1);
        StringBuilder result = new StringBuilder();
        while (result.length() < maxLength) {
            String part = maxLength % 2 == 0 ? vowels[random.nextInt(vowels.length)] : consonants[random.nextInt(consonants.length)];
            result.append(part);
        }

        while (result.length() > maxLength) {
            result.deleteCharAt(result.length() - 1);
        }
        return capitalize(clean(result));
    }

    private static String capitalize(final StringBuilder sb) {
        return sb.substring(0, 1).toUpperCase(Locale.ROOT) + sb.substring(1);
    }

    private static StringBuilder clean(final StringBuilder sb) {
        int codePoint = sb.codePointAt(0);
        if (!Character.isAlphabetic(codePoint)) {
            sb.deleteCharAt(0);
        }
        codePoint = sb.codePointAt(sb.length() - 1);
        if (!Character.isAlphabetic(codePoint)) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb;
    }

}
