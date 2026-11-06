import React from "react";
import I18n from "../locale/I18n";
import "./Footer.scss"
import surfLogo from "../icons/surf-logo.svg?url";

export const Footer = () => {

    return (
        <footer className="public-footer">
            <a className="terms" href={I18n.t("footer.termsLink")} target="_blank" rel="noopener noreferrer">
                {I18n.t("footer.privacyTerms")}
            </a>
            <a className="surf-logo" href={I18n.t("footer.surfLink")} target="_blank" rel="noopener noreferrer">
                <img src={surfLogo} alt="SURF"/>
            </a>
        </footer>
    );
}
