import {stopEvent} from "./Utils";
import {getParameterByName} from "./QueryParameters";
import {logout} from "../api";
import {useAppStore} from "../stores/AppStore";

export function login(e, currentUrl = window.location.href) {
    stopEvent(e);
    const state = getParameterByName("state", window.location.search) || currentUrl;
    localStorage.setItem("location", state);
    const config = useAppStore.getState().config;
    window.location.href =  `${config.serverUrl}/login`;
}

export function logoutUser(e) {
    stopEvent(e);
    const config = useAppStore.getState().config;
    logout().then(() => window.location.href = `${config.clientUrl}/login`)
}
