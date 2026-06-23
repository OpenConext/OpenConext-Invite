package invite.crm;

import lombok.Getter;

public enum CRMSync {

    crm_applications_missing_in_manage("CRM config applications missing in Manage"),
    applications_missing_in_crm("Applications linked to Role, but not in CRM config"),
    applications_missing_in_invite("Applications configurd in CRM config, but not connnected to Role");

    @Getter
    private final String value;

    CRMSync(String value) {
        this.value = value;
    }
}
