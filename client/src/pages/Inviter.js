import React, {useEffect} from "react";
import I18n from "../locale/I18n";
import "./Inviter.scss";
import {Button, Tooltip} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import HappyLogo from "../icons/landing/undraw_startled_-8-p0r.svg";
import DOMPurify from "dompurify";
import {stopEvent} from "../utils/Utils";
import {useNavigate} from "react-router-dom";
import {InvitationRoleCard} from "../components/InvitationRoleCard";

export const Inviter = () => {

    const {user} = useAppStore(state => state);
    const navigate = useNavigate();

    useEffect(() => {
            useAppStore.setState({
                breadcrumbPath: [
                    {path: "/inviter", value: I18n.t("tabs.home")}
                ]
            });
        },
        [])

    const gotoHistory = e => {
        stopEvent(e);
        navigate("/invitations")
    }

    const renderUserRole = (role, index) => {
        const applicationMaps = role.applicationMaps;
        return (
            <InvitationRoleCard role={role}
                                index={index}
                                applicationMaps={applicationMaps}
                                key={index}
                                isNew={false}

            />
        )
    }

    return (
        <div className="mod-inviter">
            <div className="inviter-container">
                <div className="welcome-logo">
                    <img src={HappyLogo} alt={I18n.t("notFound.alt")}/>
                </div>
                <h2>{I18n.t("inviter.welcome", {name: user.name})}</h2>
                <div className={"info"}>
                    <span dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("inviter.info"))}}/>
                    <Tooltip tip={I18n.t("tooltips.inviter")} standalone={true}/>
                </div>
                <div className={"actions"}>
                    <Button txt={I18n.t("inviter.sendInvite")}
                            onClick={() => navigate("/invitation/new")} />
                    {/*<div className={"history"}>*/}
                    {/*    <span>{I18n.t("forms.or")}*/}
                    {/*        <a href="/#" onClick={gotoHistory}>{I18n.t("inviter.viewHistory")}</a>*/}
                    {/*    </span>*/}
                    {/*</div>*/}
                </div>
                <h3 className={"sub-info"}>{I18n.t("inviter.manage")}</h3>
                {user.userRoles.map((userRole, index) => renderUserRole(userRole.role, index))}
            </div>
        </div>);
};
