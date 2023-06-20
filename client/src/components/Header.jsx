import React from "react";
import "./Header.scss";
import {Logo, LogoColor, LogoType} from "@surfnet/sds";
import {UserMenu} from "./UserMenu";
import {Link} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";

export const Header = () => {

    const {user, config} = useAppStore(state => state);
    //TODO determine actions based on user and config
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
                          actions={[]}
                />
                }
            </div>
        </div>
    );
}

