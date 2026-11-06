import React from "react";
import "./User.scss";
import I18n from "../locale/I18n";
import {isEmpty, sanitize} from "../utils/Utils";
import {RoleCard} from "./RoleCard";
import {groupApplicationsByRole} from "../utils/Manage";

//Renders per role the applications. The roles of the invitationRoles are marked as new
export const User = ({user, invitationRoles = []}) => {

    const newRoleIds = invitationRoles.map(invitationRole => invitationRole.role.id);
    const userRoles = user.userRoles
        .filter(userRole => userRole.authority === "GUEST" || userRole.guestRoleIncluded
            || newRoleIds.includes(userRole.role.id));
    const roles = groupApplicationsByRole(userRoles, I18n.locale);
    return (
        <>
            {roles.map(role =>
                <section className="role-section" key={role.id}>
                    <p dangerouslySetInnerHTML={{__html: sanitize(I18n.t("profile.rolesInfo", {role: role.roleName}))}}/>
                    <div className="role-applications">
                        {role.applications.map((application, index) =>
                            <RoleCard key={index} application={application}
                                      isNew={newRoleIds.includes(role.id)}/>)}
                    </div>
                </section>)}
            {isEmpty(roles) && <p>{I18n.t(`users.noRolesFound`)}</p>}
        </>
    );
}
