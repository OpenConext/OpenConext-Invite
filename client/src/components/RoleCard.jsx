import {Button} from "@surfnet/curve-react";
import React from "react";
import "./RoleCard.scss";
import Logo from "./Logo";
import I18n from "../locale/I18n";
import {useNavigate} from "react-router";
import {isEmpty} from "../utils/Utils.js";
import {deriveRemoteApplicationAttributes} from "../utils/Manage";

export const RoleCard = ({userRole}) => {
    const navigate = useNavigate();
    const role = userRole.role;
    const applications = role.applicationMaps || [];
    applications.forEach(app => deriveRemoteApplicationAttributes(app, I18n.locale));

    return (
        <div className="role-card" data-slot="card">
            <div className="role-card-header">
                <div className="role-card-title">
                    <h3>{role.name}</h3>
                    {userRole.authority && <p>{I18n.t(`access.${userRole.authority}`)}</p>}
                </div>
                <Button variant="outline" onClick={() => navigate(`/roles/${role.id}`)}>{I18n.t("users.viewRole")}</Button>
            </div>
            {!isEmpty(applications) &&
                <div className="role-card-footer">
                    <p>{I18n.t("users.roleAccessInfo")}</p>
                    <div className="role-card-applications">
                        {applications.map((application, index) =>
                            <div key={index} className="role-card-application">
                                <Logo src={application.logo} alt={"provider"} className={"provider"}/>
                                <div className="application-info">
                                    <h4>{(isEmpty(application.name) || application.name === "undefined") ? I18n.t("roles.unknownInManage") : application.name}</h4>
                                    {!isEmpty(application.organizationName) && <p>{application.organizationName}</p>}
                                </div>
                            </div>)}
                    </div>
                </div>}
        </div>
    );
}
