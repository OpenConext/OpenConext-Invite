package access.validation;

import java.util.regex.Pattern;

public class EmailFormatValidator implements FormatValidator {

    private static final Pattern pattern =
            Pattern.compile("^\\S+@\\S+$", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean isValid(String subject) {
        return pattern.matcher(subject).matches();
    }

    @Override
    public String formatName() {
        return "email";
    }

}
