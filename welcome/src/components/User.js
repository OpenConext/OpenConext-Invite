import React from "react";
import "./User.scss";
import I18n from "../locale/I18n";
import {isEmpty} from "../utils/Utils";
import {providerInfo} from "../utils/Manage";
import {RoleCard} from "./RoleCard";

export const User = ({user, invitationRoles = []}) => {

    const renderUserRole = (userRole, index) => {
        const role = userRole.role;
        const provider = providerInfo(user.providers.find(data => data.id === role.manageId));
        return (
            <RoleCard role={role} provider={provider} index={index}/>
        );
    }
    const rolesToExclude = invitationRoles.map(invitationRole => invitationRole.role.id);
    const filteredUserRoles = user.userRoles
        .filter(userRole => user.superUser || userRole.authority === "GUEST")
        .filter(userRole => !rolesToExclude.includes(userRole.role.id));
    return (
        <>
            {isEmpty(user.userRoles) && <p className={"span-row "}>{I18n.t("users.noRolesInfo")}</p>}
            {!isEmpty(user.userRoles) &&
                <>
                    {filteredUserRoles
                        .map((userRole, index) => renderUserRole(userRole, index))}
                    {filteredUserRoles.length === 0 &&
                        <p>{I18n.t(`users.noRolesFound`)}</p>}
                </>}
        </>
    );
}