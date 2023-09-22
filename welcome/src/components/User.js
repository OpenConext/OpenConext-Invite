import React from "react";
import "./User.scss";
import I18n from "../locale/I18n";
import {isEmpty} from "../utils/Utils";
import {RoleCard} from "./RoleCard";

export const User = ({user, invitationRoles = []}) => {

    const renderUserRole = (userRole, index) => {
        const role = userRole.role;
        return (
            <RoleCard role={role} index={index}/>
        );
    }
    const rolesToExclude = invitationRoles.map(invitationRole => invitationRole.role.id);
    const filteredUserRoles = user.userRoles
        .filter(userRole => user.superUser || userRole.authority === "GUEST")
        .filter(userRole => !rolesToExclude.includes(userRole.role.id));
    return (
        <>
            {(isEmpty(user.userRoles) && isEmpty(invitationRoles)) &&
                <p className={"span-row "}>{I18n.t("users.noRolesInfo")}</p>}
            {!isEmpty(user.userRoles) &&
                <>
                    {filteredUserRoles
                        .map((userRole, index) => renderUserRole(userRole, index))}
                    {(isEmpty(user.userRoles) && isEmpty(invitationRoles)) &&
                        <p>{I18n.t(`users.noRolesFound`)}</p>}
                </>}
        </>
    );
}