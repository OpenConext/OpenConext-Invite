import {isEmpty, sanitizeURL} from "./Utils";

export function login(config, force = false, hash = null) {
    let params = "?app=welcome&"
    if (force) {
        params += "force=true"
    }
    if (hash) {
        params += `&hash=${hash}`
    }
    let serverWelcomeUrl = config.serverWelcomeUrl;
    if (isEmpty(serverWelcomeUrl)) {
        const local = window.location.hostname === "localhost";
        serverWelcomeUrl = local ? "http://localhost:8888" :
            `${window.location.protocol}//${window.location.host}`
    }
    window.location.href = sanitizeURL(`${serverWelcomeUrl}/api/v1/users/login${params}`);
}
