import {isEmpty, sanitizeURL} from "./Utils";


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
    window.location.href = sanitizeURL(`${serverUrl}/api/v1/users/login${params}`);
}
