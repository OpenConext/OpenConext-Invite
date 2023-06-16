import {ChipType} from "@surfnet/sds";

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

export const chipTypeForUserRole = userRole => {
    switch (userRole.authority) {
        case AUTHORITIES.SUPER_USER: return ChipType.Support_500;
        case AUTHORITIES.MANAGER: return ChipType.Support_400;
        case AUTHORITIES.INVITER: return ChipType.Support_100;
        case AUTHORITIES.GUEST: return ChipType.Status_default;
    }
}

export const urnFromRole = (groupUrnPrefix, role) => `${groupUrnPrefix}:${role.manageId}:${role.shortName}`;


