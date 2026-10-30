const ORGANIZATION_KEY = "organization";
const ORGANIZATION_PARAM = "organizationId";

// SURF Access can run on another domain, so the organization travels in the url and is remembered here per origin.
export const readRequestedOrganization = () => {
    const fromUrl = new URLSearchParams(window.location.search).get(ORGANIZATION_PARAM);
    if (fromUrl) {
        return fromUrl;
    }
    try {
        return localStorage.getItem(ORGANIZATION_KEY);
    } catch {
        return null;
    }
}

export const persistOrganization = organizationId => {
    try {
        localStorage.setItem(ORGANIZATION_KEY, String(organizationId));
    } catch {
        // storage unavailable, the organization is then only valid for this page view
    }
}

export const stripOrganizationParam = () => {
    const search = new URLSearchParams(window.location.search);
    if (!search.has(ORGANIZATION_PARAM)) {
        return;
    }
    search.delete(ORGANIZATION_PARAM);
    const rest = search.toString();
    window.history.replaceState(window.history.state, "",
        window.location.pathname + (rest ? `?${rest}` : "") + window.location.hash);
}
