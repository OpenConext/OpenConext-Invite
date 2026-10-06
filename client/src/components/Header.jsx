import React from "react";
import "./Header.scss";
import {Logo, LogoColor, LogoType} from "@surfnet/sds";
import {Link} from "react-router";
import I18n from "../locale/I18n";

export const Header = () => {
    return (
        <div className="header-container">
            <div className="header-inner">
                <Link className="logo" to={"/"}>
                    <Logo label={I18n.t("header.title")}
                          position={LogoType.Bottom}
                          color={LogoColor.White}/>
                </Link>
            </div>
        </div>
    );
}

