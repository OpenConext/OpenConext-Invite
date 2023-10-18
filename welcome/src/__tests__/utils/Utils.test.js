import React from "react";
import {
    sanitizeURL
} from "../../utils/Utils";

test("Test sanitizeURL", () => {
    expect(sanitizeURL("https://invite.test2.surfconext.nl")).toEqual("https://invite.test2.surfconext.nl");
});
