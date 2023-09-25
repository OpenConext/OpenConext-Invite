import {isEmpty} from "./Utils";
import {deriveApplicationAttributes} from "./Manage";

export const INVITATION_STATUS = {
    OPEN: "OPEN",
    ACCEPTED: "ACCEPTED",
    EXPIRED: "EXPIRED"
}


export const AUTHORITIES = {
    SUPER_USER: "SUPER_USER",
    INSTITUTION_ADMIN: "INSTITUTION_ADMIN",
    MANAGER: "MANAGER",
    INVITER: "INVITER",
    GUEST: "GUEST"
}

const AUTHORITIES_HIERARCHY = {
    [AUTHORITIES.SUPER_USER]: 1,
    [AUTHORITIES.INSTITUTION_ADMIN]: 2,
    [AUTHORITIES.MANAGER]: 3,
    [AUTHORITIES.INVITER]: 4,
    [AUTHORITIES.GUEST]: 5
}

export const highestAuthority = user => {
    if (user.superUser) {
        return AUTHORITIES.SUPER_USER;
    }
    if (user.institutionAdmin) {
        return AUTHORITIES.INSTITUTION_ADMIN;
    }
    return (user.userRoles || []).reduce((acc, u) => {
        if (AUTHORITIES_HIERARCHY[acc] > AUTHORITIES_HIERARCHY[AUTHORITIES[u.authority]]) {
            return u.authority
        }
        return acc;
    }, AUTHORITIES.GUEST);
}

export const isUserAllowed = (minimalAuthority, user) => {
    if (user.superUser) {
        return true;
    }
    if (!Object.keys(AUTHORITIES).includes(minimalAuthority)) {
        throw new Error(`${minimalAuthority} is not a valid authority`);
    }
    const authority = highestAuthority(user);
    return AUTHORITIES_HIERARCHY[authority] <= AUTHORITIES_HIERARCHY[minimalAuthority];
}

export const allowedToEditRole = (user, role) => {
    if (user.superUser) {
        return true;
    }
    if (!isUserAllowed(AUTHORITIES.MANAGER, user)) {
        return false;
    }
    //One the userRoles must have the same manageId as the role
    return user.userRoles.some(userRole => userRole.role.manageId === role.manageId || userRole.role.id === role.id);
}

export const allowedToDeleteInvitation = (user, invitation) => {
    return invitation.roles
        .every(invitationRole => allowedToRenewUserRole(user, {
            ...invitationRole,
            authority: invitation.intendedAuthority
        }))
}

export const allowedToRenewUserRole = (user, userRole) => {
    if (user.superUser) {
        return true;
    }
    switch (userRole.authority) {
        case AUTHORITIES.SUPER_USER:
        case AUTHORITIES.MANAGER:
            return false;
        case AUTHORITIES.INVITER :
            return isUserAllowed(AUTHORITIES.MANAGER, user) &&
                user.userRoles.some(ur => userRole.role.manageId === ur.role.manageId || userRole.role.id === ur.role.id);
        case  AUTHORITIES.GUEST:
            return isUserAllowed(AUTHORITIES.INVITER, user) &&
                user.userRoles.some(ur => userRole.role.id === ur.role.id);
        default:
            return false
    }
}

export const urnFromRole = (groupUrnPrefix, role) => `${groupUrnPrefix}:${role.manageId}:${role.shortName}`;

export const markAndFilterRoles = (user, allRoles, locale) => {
    allRoles.forEach(role => {
        role.isUserRole = false;
        role.label = role.name;
        role.value = role.id;
        deriveApplicationAttributes(role, locale);
    });
    const userRoles = user.userRoles;
    userRoles.forEach(userRole => {
        userRole.isUserRole = true;
        const role = userRole.role;
        deriveApplicationAttributes(role, locale);
        userRole.name = role.name;
        userRole.label = role.name;
        userRole.value = role.id;
        userRole.landingPage = role.landingPage;
        userRole.description = role.description;
        userRole.defaultExpiryDays = role.defaultExpiryDays;
        userRole.eduIDOnly = role.eduIDOnly;
        userRole.enforceEmailEquality = role.enforceEmailEquality;
        userRole.applicationName = role.applicationName;
        userRole.applicationOrganizationName = role.applicationOrganizationName;
        userRole.logo = role.logo;
        userRole.userRoleCount = role.userRoleCount;
    })

    return allRoles
        .filter(role => userRoles.every(userRole => userRole.role.id !== role.id))
        .concat(userRoles);
}

export const allowedAuthoritiesForInvitation = (user, selectedRoles) => {
    if (user.superUser) {
        return Object.keys(AUTHORITIES);
    }
    if (!isUserAllowed(AUTHORITIES.INVITER, user)) {
        return [];
    }
    if (isEmpty(selectedRoles)) {
        const authority = highestAuthority(user);
        return Object.keys(AUTHORITIES)
            .filter(auth => AUTHORITIES_HIERARCHY[auth] > AUTHORITIES_HIERARCHY[authority]);
    }
    //Return only the AUTHORITIES where the user has the correct authority per selectedRole
    const userRolesForSelectedRoles = selectedRoles
        .map(role => role.isUserRole ? role.role : role)
        .map(role => user.userRoles.find(userRole => userRole.role.manageId === role.manageId || userRole.role.id === role.id))
        .filter(userRole => !isEmpty(userRole));
    const leastImportantAuthority = userRolesForSelectedRoles
        .reduce((acc, userRole) => {
            if (AUTHORITIES_HIERARCHY[userRole.authority] < AUTHORITIES_HIERARCHY[acc]) {
                return userRole.authority;
            }
            return acc;
        }, AUTHORITIES.GUEST);
    return Object.keys(AUTHORITIES)
        .filter(auth => AUTHORITIES_HIERARCHY[auth] > AUTHORITIES_HIERARCHY[leastImportantAuthority]);

}

