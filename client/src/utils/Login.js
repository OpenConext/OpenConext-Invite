import {isEmpty, sanitizeURL} from "./Utils";

const LOGGED_OUT_KEY = "explicitly_logged_out";

export const markExplicitLogout = () => {
    try {
        localStorage.setItem(LOGGED_OUT_KEY, "true");
    } catch {
        // storage unavailable, fall back to SSO
    }
}

// True only once after the user explicitly logged out
export const consumeExplicitLogout = () => {
    try {
        const loggedOut = localStorage.getItem(LOGGED_OUT_KEY) === "true";
        localStorage.removeItem(LOGGED_OUT_KEY);
        return loggedOut;
    } catch {
        return false;
    }
}

export function login(config, force = false, hash = null) {
    let params = force ? `?force=true` : "";
    if (hash) {
        params += `&hash=${hash}`
    }
    let serverUrl = config.serverUrl;
    if (isEmpty(serverUrl)) {
        const local = window.location.hostname === "localhost";
        serverUrl = local ? "http://localhost:8888" :
            `${window.location.protocol}//${window.location.host}`
    }
    // In local development the browser goes directly to the server, which does not know the /roles prefix that
    // is stripped by the proxy in deployed environments
    const prefix = import.meta.env.DEV ? "" : "/roles";
    window.location.href = sanitizeURL(`${serverUrl}${prefix}/api/v1/users/login${params}`);
}
