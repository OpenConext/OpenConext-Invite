import {isEmpty} from "./Utils";
import {ChipType} from "@surfnet/sds";
import {AUTHORITIES} from "./UserRole";

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