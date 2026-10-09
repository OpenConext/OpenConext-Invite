import React from "react";
import "./Header.scss";
import {Link} from "react-router";
import logoUrl from "../icons/logo2.svg?url";

export const Header = () => {
    return (
        <div className="header-container">
            <div className="header-inner">
                <Link className="logo" to={"/"}>
                    <img src={logoUrl} alt="SURF Access"/>
                </Link>
            </div>
        </div>
    );
}
