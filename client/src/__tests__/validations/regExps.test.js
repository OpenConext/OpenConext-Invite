import React from "react";
import {sanitizeShortName} from "../../validations/regExps";

test("Sanitize URN", () => {
    const urn = sanitizeShortName(" !@#$%^&*(9IIOO   UU  plp ")
    expect(urn).toEqual("9iioo_uu_plp")
});