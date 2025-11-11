import React from "react";
import I18n from "../locale/I18n";
import "./Profile.scss";
import {Toaster, ToasterType, Tooltip} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import {User} from "../components/User";
import HighFive from "../icons/high-five.svg";
import {login} from "../utils/Login";
import {stopEvent} from "../utils/Utils";
import DOMPurify from "dompurify";
import {logout} from "../api";

export const Profile = () => {
    const {user: currentUser, config} = useAppStore(state => state);

    const doLogin = e => {
        stopEvent(e);
        logout().then(() => login(config, true));
    }

    const toasterChildren = <div>
        <span>{I18n.t("profile.toaster", {institution: currentUser.schacHomeOrganization})}</span>
        <a href="/logout" onClick={doLogin}>{I18n.t("profile.changeThis")}</a>
        <span>)</span>
    </div>

    return (
        <div className="mod-profile">
            <div className="profile-container">
                <div className="welcome-logo">
                    <img src={HighFive} alt={I18n.t("notFound.alt")}/>
                </div>
                <h2>{I18n.t("profile.welcome", {name: currentUser.name})}</h2>
                <div>
                    <span className={"info"}
                          dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("profile.info"))}}/>
                    <Tooltip tip={I18n.t("profile.tooltipApps")}/>
                </div>

                <Toaster toasterType={ToasterType.Info}
                         large={true}
                         children={toasterChildren}/>
                <User user={currentUser}/>
            </div>
        </div>);
};
