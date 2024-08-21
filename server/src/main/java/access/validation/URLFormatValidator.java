package access.validation;

import java.util.regex.Pattern;

public class URLFormatValidator implements FormatValidator {

    private final Pattern pattern = Pattern.compile("^(http|https)://(.*)$");

    public boolean isValid(String subject) {
        return pattern.matcher(subject).matches();
    }

    @Override
    public String formatName() {
        return "url";
    }
}
