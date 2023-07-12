import React from "react";
import I18n from "../locale/I18n";
import Cookies from "js-cookie";
import {replaceQueryParameter} from "../utils/QueryParameters";
import {stopEvent} from "../utils/Utils";
import "./LanguageSelector.scss"
import {languageSwitched} from "../utils/Date";

export const LanguageSelector = () => {

    const handleChooseLocale = locale => e => {
        stopEvent(e);
        Cookies.set("lang", locale, {expires: 356, secure: document.location.protocol.endsWith("https")});
        I18n.locale = locale;
        languageSwitched();
        window.location.search = replaceQueryParameter(window.location.search, "lang", locale);
    };

    const  renderLocaleChooser = locale => {
        return (
            <a href={"locale"} className={`${I18n.locale === locale ? "is-active" : ""}`}
               title={I18n.t("select_locale")}
               onClick={handleChooseLocale(locale)}>
                {I18n.translations[locale].code}
            </a>
        );
    }

        return (
            <nav className="sds--language-switcher sds--text--body--small" aria-label="Language">
                <ul>
                    <li>{renderLocaleChooser("nl")}
                        <span className="sds--language-sds--divider">|</span>
                    </li>
                    <li>
                        {renderLocaleChooser("en")}
                    </li>
                </ul>
            </nav>
        );
}
