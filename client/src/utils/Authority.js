import {isEmpty} from "./Utils";
import {ChipType} from "@surfnet/sds";
import {AUTHORITIES, INVITATION_STATUS} from "./UserRole";

export const chipTypeForUserRole = authority => {
    if (isEmpty(authority)) {
        return ChipType.Status_warning;
    }
    switch (authority) {
        case AUTHORITIES.SUPER_USER:
            return ChipType.Support_500;
        case AUTHORITIES.MANAGER:
            return ChipType.Support_400;
        case AUTHORITIES.INVITER:
            return ChipType.Support_100;
        case AUTHORITIES.GUEST:
            return ChipType.Status_default;
        default:
            return ChipType.Status_default;
    }
}

export const chipTypeForInvitationStatus = invitation => {
    const status = invitation.status;
    switch (status) {
        case INVITATION_STATUS.OPEN:
            return ChipType.Status_info;
        case INVITATION_STATUS.ACCEPTED:
            return ChipType.Status_success;
        case INVITATION_STATUS.EXPIRED:
            return ChipType.Status_error;
        default:
            return ChipType.Status_default;
    }
}