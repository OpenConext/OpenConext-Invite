import {afterEach, beforeEach, describe, expect, it, vi} from "vitest";
import {act} from "react";
import {createRoot} from "react-dom/client";
import {MemoryRouter} from "react-router";
import {InstitutionAdmins} from "../../tabs/InstitutionAdmins";
import {applicationManagers, institutionAdmins, removeApplicationManager, removeInstitutionAdmin} from "../../api";
import {useAppStore} from "../../stores/AppStore";
import I18n from "../../locale/I18n";

vi.mock("../../api", () => ({
    applicationManagers: vi.fn(),
    institutionAdmins: vi.fn(),
    removeApplicationManager: vi.fn(),
    removeInstitutionAdmin: vi.fn()
}));

const manager = {id: 2, name: "App Manager", email: "app@manager.com", roles: "Wiki"};
const admin = {
    id: 3, name: "Inst Admin", email: "inst@admin.com", institution_admin: true, institution_admin_by_invite: true
};
const httpError = (status, body) => {
    const error = new Error("error");
    error.response = {status: status, json: () => Promise.resolve(body)};
    return error;
};

let root;
let container;

const waitFor = callback => vi.waitFor(async () => {
    await act(async () => {
    });
    callback();
});

const renderTab = async () => {
    container = document.createElement("div");
    document.body.appendChild(container);
    root = createRoot(container);
    await act(async () => root.render(<MemoryRouter><InstitutionAdmins/></MemoryRouter>));
};

const cleanup = async () => {
    await act(async () => root.unmount());
    document.body.innerHTML = "";
};

const click = async element => {
    await act(async () => element.click());
};

const buttonByText = text => Array.from(document.querySelectorAll("button"))
    .find(button => button.textContent.trim() === text);

const dialog = () => document.querySelector("[role='dialog']");

const removeFirstUser = async () => {
    await renderTab();
    await waitFor(() => expect(container.querySelector("tbody tr td:last-child span")).not.toBeNull());
    await click(container.querySelector("tbody tr td:last-child span"));
    await waitFor(() => expect(buttonByText(I18n.t("confirmationDialog.confirm"))).toBeDefined());
    await click(buttonByText(I18n.t("confirmationDialog.confirm")));
};

const flash = () => useAppStore.getState().flash;

describe("InstitutionAdmins", () => {

    beforeEach(() => {
        globalThis.IS_REACT_ACT_ENVIRONMENT = true;
        vi.resetAllMocks();
        useAppStore.setState({user: {id: 1, institutionAdmin: true, organizationGUID: "guid"}, flash: {}});
        applicationManagers.mockResolvedValue([manager]);
        institutionAdmins.mockResolvedValue([]);
    });

    afterEach(cleanup);

    it("Shows the success flash when the application manager is removed", async () => {
        removeApplicationManager.mockImplementation(() => {
            applicationManagers.mockResolvedValue([]);
            return Promise.resolve({});
        });
        await removeFirstUser();
        await waitFor(() => expect(flash()).toEqual({
            msg: I18n.t("users.removeInstitutionAdminFlash", {userName: manager.name}), type: "info"
        }));
        expect(dialog()).toBeNull();
    });

    it("Shows a warning when the application manager still manages other applications", async () => {
        removeApplicationManager.mockResolvedValue({});
        await removeFirstUser();
        await waitFor(() => expect(flash()).toEqual({
            msg: I18n.t("users.removeApplicationManagerPartialFlash", {userName: manager.name}), type: "warning"
        }));
        expect(dialog()).toBeNull();
    });

    it("Closes the dialog and shows an error when the server refuses", async () => {
        removeApplicationManager.mockRejectedValue(httpError(403, {}));
        await removeFirstUser();
        await waitFor(() => expect(flash()).toEqual({
            msg: I18n.t("users.removeApplicationManagerNotAllowed", {userName: manager.name}), type: "error"
        }));
        expect(dialog()).toBeNull();
        expect(container.textContent).toContain(manager.name);
    });

    it.each([
        ["server error", httpError(500, {reference: 123})],
        ["network error", new TypeError("Failed to fetch")]
    ])("Shows the error dialog for a %s", async (_, error) => {
        removeApplicationManager.mockRejectedValue(error);
        await removeFirstUser();
        await waitFor(() => expect(dialog()?.textContent).toContain(I18n.t("confirmationDialog.error")));
        expect(flash()).toEqual({});
        await click(buttonByText(I18n.t("forms.ok")));
        await waitFor(() => expect(dialog()).toBeNull());
    });

    it("Does not use the application manager messages for institution admins", async () => {
        applicationManagers.mockResolvedValue([]);
        institutionAdmins.mockResolvedValue([admin]);
        removeInstitutionAdmin.mockRejectedValue(httpError(403, {}));
        await removeFirstUser();
        await waitFor(() => expect(dialog()?.textContent).toContain(I18n.t("confirmationDialog.error")));
        expect(removeApplicationManager).not.toHaveBeenCalled();
        expect(flash()).toEqual({});
    });

});
