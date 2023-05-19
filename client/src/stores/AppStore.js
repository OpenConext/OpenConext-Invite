import { create } from 'zustand'

export const useAppStore = create((set) => ({
    csrfToken: null,
    impersonator: null,
}));
