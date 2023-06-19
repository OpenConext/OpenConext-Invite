package access.validation;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EmailFormatValidator implements FormatValidator {

    private static final Pattern pattern =
            Pattern.compile("^\\S+@\\S+$", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean isValid(String subject) {
        return !StringUtils.hasText(subject) || pattern.matcher(subject).matches();
    }

    @Override
    public String formatName() {
        return "email";
    }

    public Set<String> validateEmails(List<String> emails) {
        return emails.stream().filter(email -> pattern.matcher(email).matches()).collect(Collectors.toSet());
    }
}
