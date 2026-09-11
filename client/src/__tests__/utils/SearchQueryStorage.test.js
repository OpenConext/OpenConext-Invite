import {afterEach, beforeEach, describe, expect, it} from "vitest";
import {getStoredSearchQuery, storeSearchQuery} from "../../utils/SearchQueryStorage";

describe("SearchQueryStorage", () => {

    beforeEach(() => sessionStorage.clear());
    afterEach(() => sessionStorage.clear());

    it("returns an empty string when nothing was stored yet", () => {
        expect(getStoredSearchQuery("group:home")).toEqual("");
    });

    it("stores and restores a query for a key", () => {
        storeSearchQuery("group:home", "test-query");
        expect(getStoredSearchQuery("group:home")).toEqual("test-query");
    });

    it("does not leak a stored query between different keys", () => {
        storeSearchQuery("group:home", "test-query");
        expect(getStoredSearchQuery("group:system")).toEqual("");
    });

    it("removes the stored query when the query is emptied", () => {
        storeSearchQuery("group:home", "test-query");
        storeSearchQuery("group:home", "");
        expect(getStoredSearchQuery("group:home")).toEqual("");
    });

});
