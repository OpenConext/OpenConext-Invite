import React from "react";
import "./AuthorizedHeader.scss";
import {BreadCrumb} from "./BreadCrumb";
import {UserMenu} from "./UserMenu";
import {LanguageSwitcher} from "./LanguageSwitcher";
import {useAppStore} from "../stores/AppStore";

export const AuthorizedHeader = () => {
    const user = useAppStore(state => state.user);

    return (
        <div className="authorized-header">
            <BreadCrumb/>
            <div className="authorized-header-actions">
                <LanguageSwitcher/>
                {(user && user.id) && <UserMenu user={user}/>}
            </div>
        </div>
    );
}
