import I18n from "../locale/I18n";


export const AUTHORITIES = {
    SUPER_USER: "SUPER_USER",
    MANAGER: "MANAGER",
    INVITER: "INVITER",
    GUEST: "GUEST"
}

const AUTHORITIES_HIERARCHY = {
    [AUTHORITIES.SUPER_USER]: 1,
    [AUTHORITIES.MANAGER]: 2,
    [AUTHORITIES.INVITER]: 3,
    [AUTHORITIES.GUEST]: 4
}

export const highestAuthority = user => {
    if (user.superUser) {
        return AUTHORITIES.SUPER_USER;
    }
    return (user.userRoles || []).reduce((acc, u) => {
        if (AUTHORITIES_HIERARCHY[acc] > AUTHORITIES_HIERARCHY[AUTHORITIES[u.authority]]) {
            return u.authority
        }
        return acc;
    }, AUTHORITIES.GUEST);
}

export function isUserAllowed(minimalAuthority, user) {
    if (user.superUser) {
        return true;
    }
    highestAuthority(user);
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

export function globalUserRole(user) {
    const authority = highestAuthority(user);
    return I18n.t(`access.${authority}`);
}
