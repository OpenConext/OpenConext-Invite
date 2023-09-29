import React from "react";
import "./UnitHeaderInviter.scss";

import Logo from "./Logo";

import {Button} from "@surfnet/sds";
import I18n from "../locale/I18n";
import {MoreLessText} from "./MoreLessText";
import {isEmpty} from "../utils/Utils";
import DOMPurify from "dompurify";
import {useNavigate} from "react-router-dom";

export const UnitHeaderInviter = ({
                                      role,
                                      userRole,
                                      managerEmails
                                  }) => {

    const navigate = useNavigate();
    const endDateDays = (userRole && userRole.endDate) ? Math.ceil((new Date(userRole.endDate * 1000) - new Date().getTime()) / (1000 * 60 * 60 * 24)) : null;
    const adminHref = managerEmails.join(",");
    return (
        <div className="unit-header-inviter-container">
            <div className="unit-header">
                <div className="unit-header-inner">
                    <div className={`image`}>
                        <Logo src={role.logo}/>
                    </div>
                    <div className="obj-name">
                        <p>{role.applicationName}</p>
                        <h1>{role.name}</h1>
                        <MoreLessText txt={role.description} type={"compact"}/>
                    </div>
                    <div className={"role-info"}>
                        <span dangerouslySetInnerHTML={{
                            __html: DOMPurify.sanitize(I18n.t(`role.roleInfo${endDateDays ? "" : "NoEndDate"}`, {
                                days: endDateDays
                            }))
                        }}/>
                        {!isEmpty(managerEmails) &&
                            <a href={`mailto:${adminHref}`}>
                                {I18n.t("role.contactAdmin")}
                            </a>
                        }
                    </div>
                </div>
                <div className="action-menu-container">
                    <Button
                        onClick={() => navigate(`/invitation/new?maintainer=false`, {state: role.id})}
                        txt={I18n.t("invitations.newGuest")}/>

                </div>
            </div>
        </div>
    )
}
