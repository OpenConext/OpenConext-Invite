import React from "react";
import "./Header.scss";
import {Logo, LogoColor, LogoType} from "@surfnet/sds";
import {UserMenu} from "./UserMenu";
import {Link} from "react-router-dom";

export const Header = ({user, config}) => {

    return (
        <div className="header-container">
            <div className="header-inner">
                <Link className="logo" to={"/"}>
                    <Logo label={"Access"}
                          position={LogoType.Bottom}
                          color={LogoColor.White}/>
                </Link>
                {user &&
                <UserMenu currentUser={user}
                          config={config}
                          actions={[]}
                />
                }
            </div>
        </div>
    );
}

