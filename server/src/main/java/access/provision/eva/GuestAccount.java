package access.provision.eva;

import access.model.User;
import access.provision.Provisioning;
import access.provision.ProvisioningType;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.Serializable;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class GuestAccount {

    private static final String EVA_DATE_PATTERN = "yyyy-MM-dd";
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(EVA_DATE_PATTERN);
    private final MultiValueMap<String, String> request;

    public GuestAccount(User user, Provisioning provisioning) {
        assert provisioning.getProvisioningType().equals(ProvisioningType.eva) : "Must be eva provisioning";
        Instant now = Instant.now();
        Instant dateTill = now.plus(provisioning.getEvaGuestAccountDuration(), ChronoUnit.DAYS);
        String language = LocaleContextHolder.getLocale().getLanguage();
        request = new LinkedMultiValueMap<>();
        request.add("name", user.getName());
        request.add("email", user.getEmail());
        request.add("dateFrom", simpleDateFormat.format(Date.from(now)));
        request.add("dateTill", simpleDateFormat.format(Date.from(dateTill)));
        request.add("notifyByEmail", Boolean.TRUE.toString());
        request.add("notifyBySms", Boolean.FALSE.toString());
        request.add("preferredLanguage", language);
    }

    public MultiValueMap<String, String> getRequest() {
        return request;
    }
}
