import React from "react";
import "./Welcome.scss";
import I18n from "../locale/I18n";
import {initials} from "../utils/Manage";
import {stopEvent} from "../utils/Utils";
import {login} from "../utils/Login";
import {logout} from "../api";
import {useAppStore} from "../stores/AppStore";

//Avatar with the initials, the greeting and the institution the user is logged in with
export const Welcome = ({user}) => {
    const config = useAppStore(state => state.config);

    const changeAccount = e => {
        stopEvent(e);
        logout().then(() => login(config, true));
    }

    return (
        <div className="welcome">
            <div className="avatar" aria-hidden="true">{initials(user.name)}</div>
            <div className="identity">
                <h1>{I18n.t("profile.welcome", {name: user.name})}</h1>
                <div className="logged-in-via">
                    <span>{I18n.t("profile.loggedInVia", {institution: user.schacHomeOrganization})}</span>
                    <a href="/logout" onClick={changeAccount}>{I18n.t("profile.useOtherAccount")}</a>
                </div>
            </div>
        </div>
    );
}
