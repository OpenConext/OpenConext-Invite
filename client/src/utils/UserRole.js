import {isEmpty} from "./Utils";

export const INVITATION_STATUS = {
    OPEN: "OPEN",
    ACCEPTED: "ACCEPTED",
    EXPIRED: "EXPIRED"
}


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
        .every(invitationRole => allowedToRenewUserRole(user, {...invitationRole, authority: invitation.intendedAuthority}))
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

//TODO this now has two usages. Showing all roles in the roles for your overview and in new invitation - refactor to two
export const markAndFilterRoles = (user, allRoles) => {
    const userRoles = user.userRoles;
    userRoles.forEach(userRole => {
        userRole.isUserRole = true;
        userRole.name = userRole.role.name;
        userRole.label = userRole.role.name;
        userRole.value = userRole.role.id;
        userRole.landingPage = userRole.role.landingPage;
        userRole.description = userRole.role.description;
        userRole.defaultExpiryDays = userRole.role.defaultExpiryDays;
        userRole.eduIDOnly = userRole.role.eduIDOnly;
        userRole.enforceEmailEquality = userRole.role.enforceEmailEquality;
    })
    allRoles.forEach(role => {
        role.isUserRole = false;
        role.label = role.name;
        role.value = role.id;
    });
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
        //TODO Remove this hack and require only really roles
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

