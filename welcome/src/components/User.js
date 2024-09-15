import React from "react";
import "./User.scss";
import I18n from "../locale/I18n";
import {isEmpty} from "../utils/Utils";
import {RoleCard} from "./RoleCard";
import {reduceApplicationFromUserRoles} from "../utils/Manage";

export const User = ({user, invitationRoles = []}) => {

    const renderApplication = (application, index) => {
        return (
            <RoleCard index={index} application={application}/>
        );
    }

    const rolesToExclude = invitationRoles.map(invitationRole => invitationRole.role.id);
    const filteredUserRoles = user.userRoles
        .filter(userRole => userRole.authority === "GUEST" || userRole.guestRoleIncluded)
        .filter(userRole => !rolesToExclude.includes(userRole.role.id));
    const applications = reduceApplicationFromUserRoles(filteredUserRoles, I18n.locale);
    return (
        <>
            {applications.map((application, index) => renderApplication(application, index))}
            {(isEmpty(applications) && isEmpty(invitationRoles)) &&
                <p>{I18n.t(`users.noRolesFound`)}</p>}
        </>
    );
}