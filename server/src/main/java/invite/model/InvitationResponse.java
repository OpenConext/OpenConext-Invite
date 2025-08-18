package invite.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse implements Serializable {

    private int status;

    private List<RecipientInvitationURL> recipientInvitationURLs;
}
