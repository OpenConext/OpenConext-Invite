package invite.validation;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class EmailFormatValidator implements FormatValidator {

    private static final Pattern pattern =
            Pattern.compile("^.+@.+$", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean isValid(String subject) {
        return StringUtils.hasText(subject) && !hasMoreThanOneAt(subject) && pattern.matcher(subject).matches();
    }

    private boolean hasMoreThanOneAt(String input) {
        return input.indexOf('@') != -1 && input.indexOf('@') != input.lastIndexOf('@');
    }
    @Override
    public String formatName() {
        return "email";
    }

}
