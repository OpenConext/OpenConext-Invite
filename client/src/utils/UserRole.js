import I18n from "../locale/I18n";
import {ChipType} from "@surfnet/sds";


export const ROLES = {
    SUPER_USER: "superUser",
    MANAGER: "manager",
    INVITER: "inviter",
    GUEST: "guest"
}

const ROLES_HIERARCHY = {
    [ROLES.SUPER_USER]: 1,
    [ROLES.MANAGER]: 2,
    [ROLES.INVITER]: 3,
    [ROLES.GUEST]: 4
}

export function isUserAllowed(minimalRole, user) {
    if (user.superUser) {
        return true;
    }
    // if (user.guest || !user.organisation_memberships || !user.collaboration_memberships) {
    //     return false;
    // }
    // const adminOrganisationMembership = organisation_id ?
    //     user.organisation_memberships.find(m => m.organisation_id === organisation_id && m.role === "admin") :
    //     user.organisation_memberships.find(m => m.role === "admin");
    // if (adminOrganisationMembership) {
    //     return ROLES_HIERARCHY[ROLES.ORG_ADMIN] <= ROLES_HIERARCHY[minimalRole];
    // }
    //
    // const managerOrganisationMembership = organisation_id ?
    //     user.organisation_memberships.find(m => m.organisation_id === organisation_id && m.role === "manager") :
    //     user.organisation_memberships.find(m => m.role === "manager");
    // if (managerOrganisationMembership) {
    //     return ROLES_HIERARCHY[ROLES.ORG_MANAGER] <= ROLES_HIERARCHY[minimalRole];
    // }
    //
    // const adminCollaborationMembership = collaboration_id ?
    //     user.collaboration_memberships.find(m => m.collaboration_id === collaboration_id && m.role === "admin") :
    //     user.collaboration_memberships.find(m => m.collaboration_id === collaboration_id);
    // if (adminCollaborationMembership) {
    //     return ROLES_HIERARCHY[ROLES.COLL_ADMIN] <= ROLES_HIERARCHY[minimalRole];
    // }
    //
    // const memberCollaborationMembership = collaboration_id ?
    //     user.collaboration_memberships.find(m => m.collaboration_id === collaboration_id && m.role === "member") :
    //     user.collaboration_memberships.find(m => m.collaboration_id === collaboration_id);
    // if (memberCollaborationMembership) {
    //     return ROLES_HIERARCHY[ROLES.COLL_MEMBER] <= ROLES_HIERARCHY[minimalRole];
    // }
    return false;
}

export function rawGlobalUserRole(user) {
 debugger
}

export function globalUserRole(user) {
    return I18n.t(`access.${rawGlobalUserRole(user)}`);
}

// export function actionMenuUserRole(user, organisation, collaboration, service, membershipRequired) {
//     const userRole = rawGlobalUserRole(user, organisation, collaboration, service, membershipRequired);
//     return I18n.t(`actionRoles.${userRole}`);
// }
//
// export function chipType(entity) {
//     const role = entity.invite ? entity.intended_role : entity.role;
//     return role === "admin" ? ChipType.Main_400 : role === "manager" ? ChipType.Main_300 : ChipType.Main_100;
// }
//
// export function chipTypeForStatus(entity) {
//     const status = entity.status;
//     return status === "approved" ? ChipType.Status_success : status === "open" ? ChipType.Status_info : ChipType.Status_error;
//     // "suspended", "expired", "active"
// }
