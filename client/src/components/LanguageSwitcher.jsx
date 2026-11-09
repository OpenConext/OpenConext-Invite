import React from "react";
import "./LanguageSwitcher.scss";
import I18n from "../locale/I18n";
import Cookies from "js-cookie";
import {replaceQueryParameter} from "../utils/QueryParameters";
import {
    Button,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger
} from "@surfnet/curve-react";
import {CaretDownIcon, CheckIcon} from "@phosphor-icons/react";
import {useAppStore} from "../stores/AppStore";

export const LanguageSwitcher = () => {

    const config = useAppStore(state => state.config);
    const languages = (config.languages || "nl,en").split(",").map(lang => lang.trim());

    const handleChooseLocale = locale => {
        Cookies.set("lang", locale, {expires: 356, secure: document.location.protocol.endsWith("https")});
        I18n.locale = locale;
        window.location.search = replaceQueryParameter(window.location.search, "lang", locale);
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger render={
                <Button variant="ghost">
                    <span>{I18n.translations[I18n.locale].code}</span>
                    <CaretDownIcon/>
                </Button>
            }/>
            <DropdownMenuContent align="end">
                {languages.map(locale =>
                    <DropdownMenuItem key={locale} onClick={() => handleChooseLocale(locale)}>
                        <span className="language-option-name">{I18n.translations[locale].code}</span>
                        {I18n.locale === locale && <CheckIcon/>}
                    </DropdownMenuItem>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
