import Cookies from "js-cookie";
import I18n from "../locale/I18n";
import {replaceQueryParameter} from "./QueryParameters";

export const switchLocale = locale => {
    Cookies.set("lang", locale, {expires: 356, secure: document.location.protocol.endsWith("https")});
    I18n.locale = locale;
    window.location.search = replaceQueryParameter(window.location.search, "lang", locale);
};
