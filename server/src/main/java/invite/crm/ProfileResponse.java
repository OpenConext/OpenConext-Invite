package invite.crm;

import java.util.List;

public record ProfileResponse(String message, int code, List<Profile> profiles) {
}
