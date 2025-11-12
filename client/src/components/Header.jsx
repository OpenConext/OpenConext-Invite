import React from "react";
import "./Header.scss";
import {UserMenu} from "./UserMenu";
import {useAppStore} from "../stores/AppStore";
import {BreadCrumb} from "./BreadCrumb";

export const Header = () => {
    const {user, authenticated} = useAppStore(state => state);

    const actions = []

    return (
        <div className="header-container">
            <div className="header-inner">
                {authenticated && <BreadCrumb/>}
                {(user?.id) &&
                    <UserMenu user={user}
                              actions={actions}
                    />
                }
            </div>
        </div>
    );
}
