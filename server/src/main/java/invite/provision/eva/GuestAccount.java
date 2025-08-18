package invite.provision.eva;

import invite.exception.InvalidInputException;
import invite.model.User;
import invite.provision.Provisioning;
import invite.provision.ProvisioningType;
import lombok.Getter;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Comparator;


@Getter
public class GuestAccount {

    protected static final String EVA_DATE_PATTERN = "yyyy-MM-dd";

    private final MultiValueMap<String, String> request;

    public GuestAccount(User user, Provisioning provisioning) {
        assert provisioning.getProvisioningType().equals(ProvisioningType.eva) : "Must be eva provisioning";
        Instant now = Instant.now();
        Instant dateTill = user.userRolesForProvisioning(provisioning)
                .stream()
                .map(userRole -> userRole.getEndDate())
                .filter(date -> date != null)
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new AssertionError("No userRoles found for provisioning:" + provisioning.getEntityId()));
        if (dateTill.isBefore(now)) {
            throw new InvalidInputException(String.format("dateTill %s is before now %s. No EVA provisioning possible", dateTill, now));
        }
        String language = LocaleContextHolder.getLocale().getLanguage();
        request = new LinkedMultiValueMap<>();
        request.add("name", user.getName());
        request.add("email", user.getEmail());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(EVA_DATE_PATTERN);
        request.add("dateFrom", simpleDateFormat.format(Date.from(now)));
        request.add("dateTill", simpleDateFormat.format(Date.from(dateTill)));
        request.add("notifyByEmail", Boolean.TRUE.toString());
        request.add("notifyBySms", Boolean.FALSE.toString());
        request.add("preferredLanguage", language);
    }

}
