import {describe, expect, it} from "vitest";
import {routerPath} from "../../utils/Utils";

describe("routerPath", () => {
    it("strips the basename the router adds itself", () => {
        expect(routerPath("/roles/home/roles")).toBe("/home/roles");
        expect(routerPath("/roles")).toBe("/");
    });
    it("only strips the basename once, as /roles/:id is a valid route", () => {
        expect(routerPath("/roles/roles/16137/maintainers")).toBe("/roles/16137/maintainers");
    });
    it("leaves paths without the basename untouched", () => {
        expect(routerPath("/invitation/accept")).toBe("/invitation/accept");
        expect(routerPath("/rolesx/1")).toBe("/rolesx/1");
    });
});
