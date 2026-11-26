import {afterEach, beforeEach, describe, expect, it} from "vitest";
import I18n from "../../locale/I18n";
import {withCrossAppParams} from "../../utils/MenuItems";

describe("withCrossAppParams", () => {
    let locale;
    beforeEach(() => {
        locale = I18n.locale;
        I18n.locale = "nl";
    });
    afterEach(() => {
        I18n.locale = locale;
    });

    it("adds the language without an organization", () => {
        expect(withCrossAppParams("https://access.example.com/profile")).toBe("https://access.example.com/profile?lang=nl");
    });

    it("adds the organization and the language", () => {
        expect(withCrossAppParams("https://access.example.com/home", 7))
            .toBe("https://access.example.com/home?organizationId=7&lang=nl");
    });

    it("appends to an existing query", () => {
        expect(withCrossAppParams("https://access.example.com/home?a=1", "a b"))
            .toBe("https://access.example.com/home?a=1&organizationId=a+b&lang=nl");
    });
});
