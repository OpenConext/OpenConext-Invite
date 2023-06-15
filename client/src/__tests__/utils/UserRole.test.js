import React from "react";
import {AUTHORITIES, isUserAllowed} from "../../utils/UserRole";

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