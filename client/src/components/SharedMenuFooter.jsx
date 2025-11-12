import "./SharedMenuFooter.scss";
import {LanguageSelector} from "./LanguageSelector";
import I18n from "../locale/I18n";
import React from "react";

export const SharedMenuFooter = () => {
    return (
        <footer className="shared-menu-footer">
            <div className="sds--footer--inner">
                <LanguageSelector />
                <nav className="menu sds--text--body--small">
                    <ul>
                        <li>
                            <a href={I18n.t("footer.termsLink")} target="_blank"
                               rel="noopener noreferrer"><span>{I18n.t("footer.terms")}</span></a>
                        </li>
                        <li>
                            <span className="sds--language-sds--divider">|</span>
                        </li>
                        <li>
                            <a href={I18n.t("footer.privacyLink")} target="_blank"
                               rel="noopener noreferrer"><span>{I18n.t("footer.privacy")}</span></a>
                        </li>
                    </ul>
                </nav>
            </div>
        </footer>
    );
}
