export function login(config, force = false, hash = null) {
    let params = force ? `?force=true` : "";
    if (hash) {
        params += `&hash=${hash}`
    }
    window.location.href = `${config.serverUrl}/api/v1/users/login?${params}`;

}
