import {isEmpty} from "../utils/Utils";
import I18n from "../locale/I18n";
import {useAppStore} from "../stores/AppStore";
import {paginationQueryParams} from "../utils/Pagination";

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
    return fetchJson(`/api/v1/users/other/${id}`, {}, {}, false);
}

export function searchUsers(pagination = {}) {
    const queryPart = paginationQueryParams(pagination, {})
    return fetchJson(`/api/v1/users/search?${queryPart}`);
}

export function searchUsersByApplication(pagination = {}) {
    const queryPart = paginationQueryParams(pagination, {})
    return fetchJson(`/api/v1/users/search-by-application?${queryPart}`);
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
    return fetchJson(`/api/v1/invitations/public?hash=${hash}`, {}, {}, false);
}

export function newInvitation(invitationRequest) {
    return postPutJson("/api/v1/invitations", invitationRequest, "POST");
}

export function acceptInvitation(hash, invitationId) {
    const body = {hash: hash, invitationId: invitationId};
    return postPutJson("/api/v1/invitations/accept", body, "POST", false);
}

export function invitationsByRoleId(roleId) {
    return fetchJson(`/api/v1/invitations/roles/${roleId}`, {}, {}, false);
}

export function resendInvitation(invitationId) {
    return postPutJson(`/api/v1/invitations/${invitationId}`, {}, "PUT");
}

export function deleteInvitation(invitationId) {
    return fetchDelete(`/api/v1/invitations/${invitationId}`);
}

export function allInvitations() {
    return fetchJson(`/api/v1/invitations/all`, {}, {}, false);
}

export function searchInvitations(roleId, pagination = {}) {
    if (roleId) {
        pagination.roleId = roleId;
    }
    const queryPart = paginationQueryParams(pagination, {})
    return fetchJson(`/api/v1/invitations/search?${queryPart}`, {}, {}, false);
}

//Manage
export function allProviders() {
    return fetchJson("/api/v1/manage/providers");
}

export function allApplications() {
    return fetchJson("/api/v1/manage/applications")
}

export function organizationGUIDValidation(organizationGUID) {
    return fetchJson(`/api/v1/manage/organization-guid-validation/${organizationGUID}`, {}, {}, false);
}

//Roles
export function rolesByApplication(force = true, pagination = {}) {
    const queryPart = paginationQueryParams(pagination, {force: !!force})
    return fetchJson(`/api/v1/roles?${queryPart}`);
}

export function rolesPerApplicationManageId(manageId) {
    return fetchJson(`/api/v1/roles/application/${manageId}`);
}

export function roleByID(roleID, showErrorDialog = true) {
    return fetchJson(`/api/v1/roles/${roleID}`, {}, {}, showErrorDialog);
}

export function createRole(role) {
    return postPutJson("/api/v1/roles", role, "POST", false);
}

export function updateRole(role) {
    return postPutJson("/api/v1/roles", role, "PUT", false);
}

export function deleteRole(role) {
    return fetchDelete(`/api/v1/roles/${role.id}`, false);
}

//User roles
export function managersByRoleId(roleId) {
    return fetchJson(`/api/v1/user_roles/managers/${roleId}`, {}, {}, false);
}

export function searchUserRolesByRoleId(roleId, isGuests, pagination = {}) {
    const queryPart = paginationQueryParams(pagination, {})
    return fetchJson(`/api/v1/user_roles/search/${roleId}/${isGuests}?${queryPart}`, {}, {}, false);
}

export function updateUserRoleEndData(userRoleId, endDate) {
    return postPutJson("/api/v1/user_roles", {userRoleId, endDate}, "PUT");
}

export function consequencesRoleDeletion(roleId) {
    return fetchJson(`/api/v1/user_roles//consequences/${roleId}`, {}, {}, true);
}

export function deleteUserRole(userRoleId, isGuest) {
    return fetchDelete(`/api/v1/user_roles/${userRoleId}/${isGuest}`, false);
}

//API tokens
export function apiTokens() {
    return fetchJson("/api/v1/tokens");
}

export function generateToken() {
    return fetchJson("/api/v1/tokens/generate-token");
}

export function createToken(description) {
    return postPutJson("/api/v1/tokens", {description: description}, "POST");
}

export function deleteToken(token) {
    return fetchDelete(`/api/v1/tokens/${token.id}`);
}


//Validations
export function validate(type, value) {
    return isEmpty(value) ? Promise.resolve({valid: true}) :
        postPutJson("/api/v1/validations/validate", {type, value}, "POST");
}

//System
export function cronCleanup() {
    return fetchJson("/api/v1/system/cron/cleanup")
}

export function cronExpiryNotifications() {
    return fetchJson("/api/v1/system/cron/expiry-notifications")
}

export function expiryUserRoles() {
    return fetchJson("/api/v1/system/expiry-user-roles")
}

export function performanceSeed() {
    return postPutJson("/api/v1/system/performance-seed", {}, "PUT")
}

export function rolesUnknownInManage() {
    return fetchJson("/api/v1/system/unknown-roles")
}
