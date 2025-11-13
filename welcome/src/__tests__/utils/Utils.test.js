import { describe, it, expect } from 'vitest';
import {
    sanitizeURL
} from "../../utils/Utils";
describe('Utils', () => {
    it("Test sanitizeURL", () => {
        expect(sanitizeURL("https://invite.test2.surfconext.nl")).toEqual("https://invite.test2.surfconext.nl");
    });
});
