import {create} from 'zustand'

const DARK_KEY = "darkMode";

const darkQuery = window.matchMedia?.("(prefers-color-scheme: dark)");

const storedDark = () => {
    try {
        return localStorage.getItem(DARK_KEY);
    } catch {
        return null;
    }
};

// An explicit choice wins, otherwise follow the OS setting
const readDark = () => {
    const stored = storedDark();
    return stored === null ? Boolean(darkQuery?.matches) : stored === "true";
};

export const useAppStore = create(set => ({
    dark: readDark(),
    toggleDark: () => set(state => {
        const dark = !state.dark;
        try {
            localStorage.setItem(DARK_KEY, String(dark));
        } catch {
            // storage unavailable, the preference just isn't remembered
        }
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

// The store is the single source of truth, the class on <html> just mirrors it
const applyDark = ({dark}) => document.documentElement.classList.toggle("dark", dark);
applyDark(useAppStore.getState());
useAppStore.subscribe(applyDark);

// Follow live OS changes until the user has made an explicit choice
darkQuery?.addEventListener("change", e => {
    if (storedDark() === null) {
        useAppStore.setState({dark: e.matches});
    }
});
