import {create} from 'zustand'

const DARK_KEY = "darkMode";

const readDark = () => {
    try {
        return localStorage.getItem(DARK_KEY) === "true";
    } catch {
        return false;
    }
};

const applyDark = dark => document.documentElement.classList.toggle("dark", dark);

export const useAppStore = create(set => ({
    dark: readDark(),
    toggleDark: () => set(state => {
        const dark = !state.dark;
        try {
            localStorage.setItem(DARK_KEY, String(dark));
        } catch {
            // storage unavailable, the preference just isn't remembered
        }
        applyDark(dark);
        return {dark};
    }),
    csrfToken: null,
    impersonator: null,
    authenticated: false,
    reload: false,
    startImpersonation: otherUser => {
        set(state => ({impersonator: state.user, user: otherUser}));
    },
    stopImpersonation: () => {
        set(state => ({impersonator: null, user: state.impersonator}));
    },
    config: {},
    //The ids of the visible menu items, decided by SURF Access and set by the SharedMenu
    menuItems: [],
    missingAttributes: [],
    user: {},
    objectRole: "",
    flash: {msg: "", className: "hide", type: "info"},
    setFlash: (message, type) => {
        set({flash: {msg: message, type: type || "info"}});
        if (!type || type === "info") {
            setTimeout(() => set({flash: {}}), 5000);
        }
    },
    clearFlash: () => set({flash: {}}),
    //[{path: "/roles/4", value: role.name}]
    breadcrumbPath: []
}));

applyDark(useAppStore.getState().dark);
