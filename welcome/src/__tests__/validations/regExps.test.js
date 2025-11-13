import { describe, it, expect } from 'vitest';
import {constructShortName} from "../../validations/regExps";
describe('regExps', () => {
    it("Sanitize URN", () => {
        const urn = constructShortName(" !@#$%^&*(9IIOO   UU  plp ")
        expect(urn).toEqual("9iioo_uu_plp")
    });
});