import {isEmpty} from "./Utils";

//Search input fields are not shared between components, so when tabs are switched
//the previous tab's search text is persisted here and restored into the next tab.
const storageKey = key => `search-query:${key}`;

export const getStoredSearchQuery = key => {
    try {
        return sessionStorage.getItem(storageKey(key)) || "";
    } catch {
        return "";
    }
};

export const storeSearchQuery = (key, query) => {
    try {
        if (isEmpty(query)) {
            sessionStorage.removeItem(storageKey(key));
        } else {
            sessionStorage.setItem(storageKey(key), query);
        }
    } catch {
        //Ignore - e.g. sessionStorage not available in private browsing mode
    }
};
