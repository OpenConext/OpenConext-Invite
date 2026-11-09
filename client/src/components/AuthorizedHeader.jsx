import React from "react";
import "./AuthorizedHeader.scss";
import {BreadCrumb} from "./BreadCrumb";
import {UserMenu} from "./UserMenu";
import {LanguageSwitcher} from "./LanguageSwitcher";
import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import {accessUrl, supportMenuItem} from "../utils/MenuItems";

export const AuthorizedHeader = () => {
    const user = useAppStore(state => state.user);
    const config = useAppStore(state => state.config);
    const menuItems = useAppStore(state => state.menuItems);
    //Without the Access menu there is no sidebar, so the user menu stays in the header
    const accessMenuEnabled = config.accessMenuEnabled !== false;

    return (
        <div className="authorized-header">
            <BreadCrumb/>
            <div className="authorized-header-actions">
                <LanguageSwitcher/>
                {accessMenuEnabled ?
                    menuItems.includes(supportMenuItem.name) &&
                    <a href={`${accessUrl(config)}${supportMenuItem.path}`} className="support-link">
                        {I18n.t(`navigation.${supportMenuItem.name}`)}
                    </a> :
                    (user && user.id) && <UserMenu user={user} variant="header"/>}
            </div>
        </div>
    );
}
