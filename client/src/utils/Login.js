export function login(config, force = false, hash = null) {
    const params = force ? `?force=true&hash=${hash}` : "";
    if (force) {
        window.location.href = `${config.serverUrl}/api/v1/users/login${params}`;
    } else {
        window.location.href = `${config.serverUrl}/api/v1/users/login${params}`;
    }

}
