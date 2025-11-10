import React from "react";
import "./Header.scss";
import {UserMenu} from "./UserMenu";
import {useAppStore} from "../stores/AppStore";

export const Header = () => {
    const {user} = useAppStore(state => state);

    const actions = []

    return (
        <div className="header-container">
            <div className="header-inner">
                {/* Todo Breadcrumb here */}
                {(user && user.id) &&
                    <UserMenu user={user}
                              actions={actions}
                    />
                }
            </div>
        </div>
    );
}
