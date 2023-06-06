export function login(config, force = false, hash = null) {
    const params = force ? `?force=true&hash=${hash}` : "";
    window.location.href = `${config.serverUrl}/api/v1/users/login${params}`;
}
