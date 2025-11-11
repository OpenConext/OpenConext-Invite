import { describe, it, expect } from 'vitest';
import {distinctValues, sanitizeURL} from "../../utils/Utils";
describe('Utils', () => {
    it("Test sanitizeURL", () => {
        expect(sanitizeURL("https://invite.test2.surfconext.nl")).toEqual("https://invite.test2.surfconext.nl");
    });

    it("Test distinctValues", () => {
        const res = distinctValues([{id: "1", val: "val1"}, {id: "1", val: "valX"}, {id: "2", val: "val2"}, {
                id: "3",
                val: "val3"
            }],
            "id");
        expect(res.length).toEqual(3);
    });
});