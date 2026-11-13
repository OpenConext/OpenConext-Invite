import React from "react";
import "./Header.scss";
import I18n from "../locale/I18n";
import {LanguageSwitcher} from "./LanguageSwitcher";
import {ThemeToggle} from "./ThemeToggle";

export const Header = () => {
    return (
        <div className="header-container">
            <div className="header-inner">
                <LanguageSwitcher/>
                <ThemeToggle/>
                <a className="support-link" href={I18n.t("header.supportLink")} target="_blank"
                   rel="noopener noreferrer">{I18n.t("header.support")}</a>
            </div>
        </div>
    );
}
