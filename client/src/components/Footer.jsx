import React from "react";
import I18n from "../locale/I18n";
import "./Footer.scss"
import {LanguageSelector} from "./LanguageSelector";

export const Footer = () => {

    return (
        <footer className="public-footer">
            <nav className="menu">
                <ul>
                    <li>
                        <a href={I18n.t("footer.termsLink")} target="_blank"
                           rel="noopener noreferrer"><span>{I18n.t("footer.terms")}</span></a>
                    </li>
                    <li>
                        <a href={I18n.t("footer.privacyLink")} target="_blank"
                           rel="noopener noreferrer"><span>{I18n.t("footer.privacy")}</span></a>
                    </li>
                </ul>
            </nav>
            <LanguageSelector/>
        </footer>
    );
}
