import React from "react";
import I18n from "../locale/I18n";
import "./InviteFooter.scss"

export const InviteFooter = () => {

    return (
        <footer className="invite-footer">
            <a href={I18n.t("footer.termsLink")} target="_blank" rel="noopener noreferrer">
                {I18n.t("footer.privacyTerms")}
            </a>
        </footer>
    );
}
