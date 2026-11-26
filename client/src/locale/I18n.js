import en from "./en";
import nl from "./nl";
import pt from "./pt";

import {I18n as I18nRemote} from "i18n-js";

import {getParameterByName} from "../utils/QueryParameters";
import {isEmpty} from "../utils/Utils";
import Cookies from "js-cookie";
import {reportError} from "../api";
import {useAppStore} from "../stores/AppStore";

const I18n = new I18nRemote({
    en: en,
    nl: nl,
    pt: pt,
});

// DetermineLanguage based on parameter, cookie and finally navigator
const requestedLanguage = getParameterByName("lang", window.location.search);
let parameterByName = requestedLanguage;

if (isEmpty(parameterByName)) {
    parameterByName = Cookies.get("lang");
}

if (isEmpty(parameterByName)) {
    parameterByName = navigator.language.toLowerCase().substring(0, 2);
}
if (["nl", "en", "pt"].indexOf(parameterByName) === -1) {
    parameterByName = "en";
}
I18n.locale = parameterByName;

// The other application (Access / Invite) runs on another domain and passes the language in the url.
// Remember it for this domain, before a possible login redirect drops the parameter.
if (!isEmpty(requestedLanguage) && requestedLanguage === parameterByName) {
    Cookies.set("lang", parameterByName, {expires: 356, secure: document.location.protocol.endsWith("https")});
}

I18n.missingTranslation.register("report-error", (i18n, scope) => {
    const {user, config} = useAppStore.getState();
    if (user && user.id && !isEmpty(config.environment)) {
        reportError({"Missing translation": scope});
    }
    return `[missing "${scope}" translation]`;
});
I18n.missingBehavior = "report-error";

export default I18n;
