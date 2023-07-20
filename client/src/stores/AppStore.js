import { create } from 'zustand'

export const useAppStore = create(set => ({
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

export const useManageStore = create(set => ({
    providers: [],
    provisionings: [],
    providerById: {}
}));