import {constructShortName, validEmailRegExp} from "../../validations/regExps";

test("Sanitize URN", () => {
    const urn = constructShortName(" !@#$%^&*(9IIOO   UU  plp ")
    expect(urn).toEqual("9iioo_uu_plp")
});

test("Emails formats", () => {
    expect(validEmailRegExp.test("aa")).toBeFalsy();
    expect(validEmailRegExp.test("a!@a.c")).toBeFalsy();
    expect(validEmailRegExp.test("a!@a.c@")).toBeFalsy();

    expect(validEmailRegExp.test("a@a")).toBeTruthy();
    expect(validEmailRegExp.test("a.x@a")).toBeTruthy();
    expect(validEmailRegExp.test("a@a.c")).toBeTruthy();
})