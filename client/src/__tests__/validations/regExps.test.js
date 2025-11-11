import { describe, it, expect } from 'vitest';
import {constructShortName, validEmailRegExp} from "../../validations/regExps";
describe('regExps', () => {
    it('Sanitize URN', () => {
        expect(true).toBe(true);
        const urn = constructShortName(" !@#$%^&*(9IIOO   UU  plp ")
        expect(urn).toEqual("9iioo_uu_plp")
    });

    it('Emails formats', () => {
        expect(validEmailRegExp.test("aa")).toBeFalsy();
        expect(validEmailRegExp.test("a!@a.c")).toBeFalsy();
        expect(validEmailRegExp.test("a!@a.c@")).toBeFalsy();

        expect(validEmailRegExp.test("a@a")).toBeTruthy();
        expect(validEmailRegExp.test("a.x@a")).toBeTruthy();
        expect(validEmailRegExp.test("a@a.c")).toBeTruthy();
    });
});
