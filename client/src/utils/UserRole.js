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

export const highestAuthority = (user, forceApplications = true) => {
    if (user.superUser) {
        return AUTHORITIES.SUPER_USER;
    }
    if (user.institutionAdmin && (!isEmpty(user.applications) || !forceApplications)) {
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
    if (user.institutionAdmin && (user.applications || []).some(app => app.id === role.manageId)) {
        return true;
    }
    //One the userRoles must have the same manageId as the role
    const userRole = user.userRoles.find(userRole => userRole.role.manageId === role.manageId || userRole.role.id === role.id);
    return !isEmpty(userRole) && AUTHORITIES_HIERARCHY[userRole.authority] <= AUTHORITIES_HIERARCHY[AUTHORITIES.MANAGER];
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
    const allowedByApplication = user.institutionAdmin && (user.applications || [])
        .some(application => application.id === userRole.role.manageId);
    switch (userRole.authority) {
        case AUTHORITIES.SUPER_USER:
            return false;
        case AUTHORITIES.INSTITUTION_ADMIN:
            return false;
        case AUTHORITIES.MANAGER:
            return allowedByApplication;
        case AUTHORITIES.INVITER :
            return isUserAllowed(AUTHORITIES.MANAGER, user) &&
                (user.userRoles.some(ur => userRole.role.manageId === ur.role.manageId || userRole.role.id === ur.role.id) || allowedByApplication) ;
        case  AUTHORITIES.GUEST:
            return isUserAllowed(AUTHORITIES.INVITER, user) &&
                (user.userRoles.some(ur => userRole.role.id === ur.role.id) || allowedByApplication);
        default:
            return false
    }
}

export const urnFromRole = (groupUrnPrefix, role) => `${groupUrnPrefix}:${role.identifier}:${role.shortName}`;

export const markAndFilterRoles = (user, allRoles, locale, multiple, separator) => {
    allRoles.forEach(role => {
        role.isUserRole = false;
        role.label = role.name;
        role.value = role.id;
        deriveApplicationAttributes(role, locale, multiple, separator);
    });
    const userRoles = user.userRoles;
    userRoles.forEach(userRole => {
        userRole.isUserRole = true;
        const role = userRole.role;
        deriveApplicationAttributes(role, locale, multiple, separator);
        userRole.name = role.name;
        userRole.label = role.name;
        userRole.value = role.id;
        userRole.landingPage = role.landingPage;
        userRole.description = role.description;
        userRole.defaultExpiryDays = role.defaultExpiryDays;
        userRole.eduIDOnly = role.eduIDOnly;
        userRole.enforceEmailEquality = role.enforceEmailEquality;
        userRole.overrideSettingsAllowed = role.overrideSettingsAllowed;
        userRole.applicationName = role.applicationName;
        userRole.applicationOrganizationName = role.applicationOrganizationName;
        userRole.applicationMaps = role.applicationMaps;
        userRole.applications = role.applications;
        userRole.logo = role.logo;
        userRole.userRoleCount = role.userRoleCount;
    })
    return allRoles
        .filter(role => userRoles.every(userRole => userRole.role.id !== role.id))
        .concat(userRoles);
}

export const allowedAuthoritiesForInvitation = (user, selectedRoles) => {
    if (user.superUser) {
        return Object.keys(AUTHORITIES)
            //The superuser has no organization guid
            .filter(authority => authority !== AUTHORITIES.INSTITUTION_ADMIN);
    }
    if (user.institutionAdmin && !isEmpty(user.applications)) {
        return Object.keys(AUTHORITIES)
            .filter(authority => authority !== AUTHORITIES.SUPER_USER);
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

