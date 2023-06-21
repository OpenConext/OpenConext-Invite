import React from "react";
import {constructShortName} from "../../validations/regExps";

test("Sanitize URN", () => {
    const urn = constructShortName(" !@#$%^&*(9IIOO   UU  plp ")
    expect(urn).toEqual("9iioo_uu_plp")
});