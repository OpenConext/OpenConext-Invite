import React from "react";
import {AUTHORITIES, isUserAllowed, allowedAuthoritiesForInvitation} from "../../utils/UserRole";

test("Test isUserAllowed", () => {
    let user = {userRoles: [{authority: AUTHORITIES.GUEST}]}
    expect(isUserAllowed(AUTHORITIES.INVITER, user)).toBeFalsy();

    user = {superUser:true}
    expect(isUserAllowed(AUTHORITIES.SUPER_USER, user)).toBeTruthy();

    user = {userRoles: [{authority: AUTHORITIES.GUEST}, {authority: AUTHORITIES.MANAGER}]}
    expect(isUserAllowed(AUTHORITIES.SUPER_USER, user)).toBeFalsy();
    expect(isUserAllowed(AUTHORITIES.MANAGER, user)).toBeTruthy();

    expect(() => isUserAllowed("nope", user)).toThrow(Error);
});

test("Allowed authorities for invitation - superUser", () => {
    const user = { superUser: true}

    const authorities = allowedAuthoritiesForInvitation(user, []);
    expect(authorities).toEqual([AUTHORITIES.SUPER_USER, AUTHORITIES.MANAGER, AUTHORITIES.INVITER, AUTHORITIES.GUEST]);
});

test("Allowed authorities for invitation - manager", () => {
    const research = {authority: AUTHORITIES.MANAGER, role: {id: "1", manageId: "2"}};
    const wikiRole = {id: "2", manageId: "2"};
    const mail = {authority: AUTHORITIES.INVITER, role: {id: "3", manageId: "9"}};
    const user = { userRoles: [research, mail]}

    let authorities = allowedAuthoritiesForInvitation(user, []);
    expect(authorities).toEqual([AUTHORITIES.INVITER, AUTHORITIES.GUEST]);

    authorities = allowedAuthoritiesForInvitation(user, [wikiRole]);
    expect(authorities).toEqual([AUTHORITIES.INVITER, AUTHORITIES.GUEST]);

    authorities = allowedAuthoritiesForInvitation(user, [mail.role]);
    expect(authorities).toEqual([AUTHORITIES.GUEST]);

});