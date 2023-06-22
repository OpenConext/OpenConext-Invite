import React from "react";
import "./Header.scss";
import {Logo, LogoColor, LogoType} from "@surfnet/sds";
import {UserMenu} from "./UserMenu";
import {Link} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import {AUTHORITIES, isUserAllowed} from "../utils/UserRole";
import {stopEvent} from "../utils/Utils";

export const Header = () => {

    const {user, config} = useAppStore(state => state);
    const actions = [];
    if (isUserAllowed(AUTHORITIES.INVITER, user)) {
        actions.push({
            href: "switch", perform: e => {
                stopEvent(e);
                window.location.href = `${config.serverUrl}/api/v1/users/switch?app=access`
            }, name: I18n.t("header.links.switchApp", {app: I18n.t("header.links.access")})
        })
    }

    return (
        <div className="header-container">
            <div className="header-inner">
                <Link className="logo" to={"/"}>
                    <Logo label={I18n.t("header.title")}
                          position={LogoType.Bottom}
                          color={LogoColor.White}/>
                </Link>
                {(user && user.id) &&
                    <UserMenu user={user}
                              config={config}
                              actions={actions}
                    />
                }
            </div>
        </div>
    );
}

