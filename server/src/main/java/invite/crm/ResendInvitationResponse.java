package invite.crm;

public record ResendInvitationResponse(String timestamp, int status, String key, String message) {
}
