import React from "react";
import {sanitizeUrn} from "../../validations/regExps";

test("Sanitize URN", () => {
    const urn = sanitizeUrn(" !@#$%^&*(9IIOO   UU  plp ")
    expect(urn).toEqual("9iioo_uu_plp")
});