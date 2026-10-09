import {Badge} from "@surfnet/curve-react";
import {isEmpty} from "./Utils";
import {AUTHORITIES} from "./UserRole";
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
    if (!isEmpty(user.userApplications)) {
        return AUTHORITIES.APPLICATION_MANAGER;
    }
    if (isEmpty(user.authority)) {
        return null;
    }
    let authorities = user.authority.split(",");
    return authorities[authorities.length - 1];
}

export const badgeVariantForUserRole = authority => {
    if (isEmpty(authority)) {
        return "warning";
    }
    switch (authority) {
        case AUTHORITIES.SUPER_USER:
            return "success";
        case AUTHORITIES.INSTITUTION_ADMIN:
            return "info";
        case AUTHORITIES.MANAGER:
            return "secondary";
        case AUTHORITIES.INVITER:
            return "outline";
        case AUTHORITIES.GUEST:
            return "outline";
        default:
            return "outline";
    }
}

export const invitationExpiry = invitation => {
    const expiryDate = isEmpty(invitation.expiry_date) ? new Date(invitation.expiryDate * 1000) :
        new Date(invitation.expiry_date);
    const expired = expiryDate < new Date();
    if (expired) {
        return <Badge variant="danger">{I18n.t("invitations.statuses.expired")}</Badge>
    }
    return shortDateFromEpoch(invitation.expiry_date || invitation.expiryDate * 1000, false);
}

export const authorityForRole = (user, role) => {
    if (role.isUserRole && role.authority) {
        return role.authority;
    }
    const userRole = (user.userRoles || []).find(userRole => userRole.role.id === role.id);
    return userRole ? userRole.authority : null;
}
