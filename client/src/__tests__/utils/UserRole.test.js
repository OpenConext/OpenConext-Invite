import {
    allowedAuthoritiesForInvitation,
    allowedToDeleteInvitation,
    allowedToEditRole,
    allowedToRenewUserRole,
    AUTHORITIES,
    highestAuthority,
    isUserAllowed
} from "../../utils/UserRole";

const applicationUsagesForManageId = manageId => {
    return [{application: {manageId: manageId}}];
}

test("Test isUserAllowed", () => {
    let user = {userRoles: [{authority: AUTHORITIES.GUEST}]}
    expect(isUserAllowed(AUTHORITIES.INVITER, user)).toBeFalsy();

    user = {superUser: true}
    expect(isUserAllowed(AUTHORITIES.SUPER_USER, user)).toBeTruthy();

    user = {userRoles: [{authority: AUTHORITIES.GUEST}, {authority: AUTHORITIES.MANAGER}]}
    expect(isUserAllowed(AUTHORITIES.SUPER_USER, user)).toBeFalsy();
    expect(isUserAllowed(AUTHORITIES.MANAGER, user)).toBeTruthy();

    expect(() => isUserAllowed("nope", user)).toThrow(Error);
});

test("Allowed authorities for invitation - superUser", () => {
    const user = {superUser: true}

    const authorities = allowedAuthoritiesForInvitation(user, []);
    expect(authorities).toEqual([AUTHORITIES.SUPER_USER, AUTHORITIES.MANAGER, AUTHORITIES.INVITER, AUTHORITIES.GUEST]);
});

test("Allowed authorities for invitation - manager", () => {
    const researchUserRole = {authority: AUTHORITIES.MANAGER, role: {id: "1", manageId: "2"}};
    const wikiRole = {id: "2", manageId: "2"};
    const mailUserRole = {authority: AUTHORITIES.INVITER, role: {id: "3", manageId: "9"}};
    const user = {userRoles: [researchUserRole, mailUserRole]}

    let authorities = allowedAuthoritiesForInvitation(user, []);
    expect(authorities).toEqual([AUTHORITIES.INVITER, AUTHORITIES.GUEST]);

    authorities = allowedAuthoritiesForInvitation(user, [wikiRole]);
    expect(authorities).toEqual([]);

    authorities = allowedAuthoritiesForInvitation(user, [mailUserRole.role]);
    expect(authorities).toEqual([AUTHORITIES.GUEST]);

});

test("Allowed to renew UserRole", () => {
    const research = {
        authority: AUTHORITIES.MANAGER,
        role: {id: "1", applicationUsages: applicationUsagesForManageId("2")}
    };
    const mail = {
        authority: AUTHORITIES.INVITER,
        role: {id: "3", applicationUsages: applicationUsagesForManageId("9")}
    };
    const calendar = {
        authority: AUTHORITIES.GUEST,
        role: {id: "3", applicationUsages: applicationUsagesForManageId("9")}
    };
    let user = {superUser: true}
    expect(allowedToRenewUserRole(user, null)).toBeTruthy();

    user = {userRoles: [calendar]}
    expect(allowedToRenewUserRole(user, calendar)).toBeFalsy();

    user = {userRoles: [research]}
    expect(allowedToRenewUserRole(user, {authority: AUTHORITIES.SUPER_USER})).toBeFalsy();
    expect(allowedToRenewUserRole(user, {authority: AUTHORITIES.MANAGER})).toBeFalsy();
    expect(allowedToRenewUserRole(user, {
        authority: AUTHORITIES.INVITER,
        role: {id: "9", applicationUsages: applicationUsagesForManageId("9")}
    })).toBeFalsy();
    expect(allowedToRenewUserRole(user, {
        authority: AUTHORITIES.INVITER,
        role: {id: "1", applicationUsages: applicationUsagesForManageId("9")}
    })).toBeTruthy();
    expect(allowedToRenewUserRole(user, {
        authority: AUTHORITIES.INVITER,
        role: {id: "9", applicationUsages: applicationUsagesForManageId("2")}
    })).toBeTruthy();

    user = {userRoles: [mail]}
    expect(allowedToRenewUserRole(user, {
        authority: AUTHORITIES.INVITER,
        role: {id: "3", applicationUsages: applicationUsagesForManageId("9")}
    })).toBeFalsy();
    expect(allowedToRenewUserRole(user, {
        authority: AUTHORITIES.GUEST,
        role: {id: "1", applicationUsages: applicationUsagesForManageId("11")}
    })).toBeFalsy();
    expect(allowedToRenewUserRole(user, {
        authority: AUTHORITIES.GUEST,
        role: {id: "3", applicationUsages: applicationUsagesForManageId("11")}
    })).toBeTruthy();

    // An institution admin is allowed to CRUD for every role that is owned by the organization of the insitution admin
    user = {institutionAdmin: true, applications: [{id: "2"}]}
    expect(allowedToRenewUserRole(user, research)).toBeTruthy();
    expect(allowedToRenewUserRole(user, mail)).toBeFalsy();

})

test("Allowed to renew guestIncluded UserRole as Inviter", () => {
    const calendar = {
        authority: AUTHORITIES.INVITER,
        role: {id: "1", applicationUsages: applicationUsagesForManageId("1")}
    };
    const user = {userRoles: [calendar]}
    expect(allowedToRenewUserRole(user, {
        authority: AUTHORITIES.MANAGER,
        role: {id: "1", applicationUsages: applicationUsagesForManageId("1")}
    })).toBeFalsy();

    expect(allowedToRenewUserRole(user, {
        authority: AUTHORITIES.MANAGER,
        role: {id: "1", applicationUsages: applicationUsagesForManageId("1")}
    }, true, true)).toBeTruthy();
})

test("Allowed to delete Invitation", () => {
    const mail = {
        authority: AUTHORITIES.INVITER,
        role: {id: "1", applicationUsages: applicationUsagesForManageId("10")}
    };
    const research = {
        authority: AUTHORITIES.INVITER,
        role: {id: "2", applicationUsages: applicationUsagesForManageId("11")}
    };
    const user = {userRoles: [mail, research]}
    const invitation = {intended_authority: AUTHORITIES.GUEST, roles: [mail, research]};
    expect(allowedToDeleteInvitation(user, invitation)).toBeTruthy();

    invitation.intended_authority = AUTHORITIES.INVITER;
    expect(allowedToDeleteInvitation(user, invitation)).toBeFalsy();
});

test("Allowed to edit", () => {
    const role = {id: 1, applicationUsages: applicationUsagesForManageId("1")};
    const user = {
        institutionAdmin: true,
        applications: [{id: "1"}],
        userRoles: [{authority: AUTHORITIES.INVITER, role: role}]
    }
    expect(allowedToEditRole(user, role)).toBeTruthy();
});

test("Highest authority", () => {
    const user = {
        institutionAdmin: true,
    }
    expect(highestAuthority(user, false)).toEqual(AUTHORITIES.INSTITUTION_ADMIN);
});
