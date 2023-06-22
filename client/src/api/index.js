import {isEmpty} from "../utils/Utils";
import I18n from "../locale/I18n";
import {useAppStore} from "../stores/AppStore";

//Internal API
function validateResponse(showErrorDialog) {
    return res => {
        if (!res.ok) {
            if (res.type === "opaqueredirect") {
                setTimeout(() => window.location.reload(true), 100);
                return res;
            }
            const error = new Error(res.statusText);
            error.response = res;
            if (showErrorDialog && res.status === 401) {
                window.location.reload(true);
                return;
            }
            if (showErrorDialog) {
                setTimeout(() => {
                    throw error;
                }, 250);
            }
            throw error;
        }
        const sessionAlive = res.headers.get("x-session-alive");
        if (sessionAlive !== "true") {
            window.location.reload(true);
        }
        return res;

    };
}

// It is not allowed to put non ASCI characters in HTTP headers
function sanitizeHeader(s) {
    if (typeof s === 'string' || s instanceof String) {
        s = s.replace(/[^\x00-\x7F]/g, ""); // eslint-disable-line no-control-regex
    }
    return isEmpty(s) ? "NON_ASCII_ONLY" : s;
}

function validFetch(path, options, headers = {}, showErrorDialog = true) {

    const contentHeaders = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "Accept-Language": I18n.locale,
        "X-CSRF-TOKEN": useAppStore.getState().csrfToken,
        ...headers
    };
    const impersonator = useAppStore.getState().impersonator;
    if (impersonator) {
        contentHeaders["X-IMPERSONATE-ID"] = sanitizeHeader(useAppStore.getState().user.id);
    }
    const fetchOptions = Object.assign({}, {headers: contentHeaders}, options, {
        credentials: "same-origin",
        redirect: "manual",
        changeOrigin: false,
    });
    return fetch(path, fetchOptions).then(validateResponse(showErrorDialog))

}

function fetchJson(path, options = {}, headers = {}, showErrorDialog = true) {
    return validFetch(path, options, headers, showErrorDialog)
        .then(res => res.json());
}

function postPutJson(path, body, method, showErrorDialog = true) {
    const jsonBody = JSON.stringify(body);
    return fetchJson(path, {method: method, body: jsonBody}, {}, showErrorDialog);
}

function fetchDelete(path, showErrorDialog = true) {
    return validFetch(path, {method: "delete"}, {}, showErrorDialog);
}

//Base
export function health() {
    return fetchJson("/internal/health");
}

export function configuration() {
    return fetchJson("/api/v1/users/config", {}, {}, false);
}

//Users
export function me() {
    return fetchJson("/api/v1/users/me", {}, {}, false);
}

export function other(id) {
    return fetchJson(`/api/v1/users/other/${id}`);
}

export function searchUsers(query) {
    return fetchJson(`/api/v1/users/search?query=${query}`);
}

export function csrf() {
    return fetchJson("/api/v1/csrf", {}, {}, false);
}

export function logout() {
    return fetchJson("/api/v1/users/logout");
}

export function reportError(error) {
    return postPutJson("/api/v1/users/error", error, "post");
}

//Invitations
export function invitationByHash(hash) {
    return fetchJson(`/api/v1/invitations/public?hash=${hash}`);
}

export function newInvitation(invitationRequest) {
    return postPutJson("/api/v1/invitations", invitationRequest, "POST");
}

export function acceptInvitation(hash, invitationId) {
    const body = {hash: hash, invitationId: invitationId};
    return postPutJson("/api/v1/invitations/accept", body, "POST", false);
}

export function invitationsByRoleId(roleId) {
    return fetchJson(`/api/v1/invitations/roles/${roleId}`);
}

//Manage
export function allProviders() {
    return fetchJson("/api/v1/manage/providers");
}

export function applications() {
    return fetchJson("/api/v1/manage/applications")
}

//Roles
export function rolesByApplication() {
    return fetchJson("/api/v1/roles");
}

export function roleByID(roleID) {
    return fetchJson(`/api/v1/roles/${roleID}`);
}

export function createRole(role) {
    return postPutJson("/api/v1/roles", role, "POST", false);
}

export function updateRole(role) {
    return postPutJson("/api/v1/roles", role, "PUT", false);
}

export function deleteRole(role) {
    return fetchDelete(`/api/v1/roles/${role.id}`,false);
}

export function searchRoles(query) {
    return fetchJson(`/api/v1/roles/search?query=${query}`);
}

export function shortNameExists(shortName, manageId, roleId) {
    const body = {shortName: shortName, manageId: manageId, id: roleId};
    return postPutJson("/api/v1/roles/validation/short_name", body, "POST");
}

//User roles
export function userRolesByRoleId(roleId) {
    return fetchJson(`/api/v1/user_roles/roles/${roleId}`);
}

//Validations
export function validate(type, value) {
    return isEmpty(value) ? Promise.resolve({valid: true}) :
        postPutJson("/api/v1/validations/validate", {type, value}, "POST");
}
