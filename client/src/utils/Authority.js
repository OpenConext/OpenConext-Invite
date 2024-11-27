import {isEmpty} from "./Utils";
import {Chip, ChipType} from "@surfnet/sds";
import {AUTHORITIES, INVITATION_STATUS} from "./UserRole";
import {shortDateFromEpoch} from "./Date";
import I18n from "../locale/I18n";
import React from "react";

export const authorityForUserOverview = user => {
    if (user.super_user) {
        return AUTHORITIES.SUPER_USER;
    }
    if (user.institution_admin) {
        return AUTHORITIES.INSTITUTION_ADMIN;
    }
    return isEmpty(user.authority) ? null : user.authority.split(",")[0];
}

export const chipTypeForUserRole = authority => {
    if (isEmpty(authority)) {
        return ChipType.Status_warning;
    }
    switch (authority) {
        case AUTHORITIES.SUPER_USER:
            return ChipType.Status_success;
        case AUTHORITIES.INSTITUTION_ADMIN:
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

export const invitationExpiry = invitation => {
    const expired = new Date(invitation.expiryDate * 1000) < new Date();
    if (expired) {
        return <Chip
            type={ChipType.Status_error}
            label={I18n.t("invitations.statuses.expired")}/>
    }
    return shortDateFromEpoch(invitation.expiryDate);
}
