import {distinctValues, sanitizeURL} from "../../utils/Utils";

test("Test sanitizeURL", () => {
    expect(sanitizeURL("https://invite.test2.surfconext.nl")).toEqual("https://invite.test2.surfconext.nl");
});

test("Test distinctValues", () => {
    const res = distinctValues([{id: "1", val: "val1"}, {id: "1", val: "valX"}, {id: "2", val: "val2"}, {
            id: "3",
            val: "val3"
        }],
        "id");
    expect(res.length).toEqual(3);

    const resId = distinctValues([{id: 1, val: "val1"}, {id: 1, val: "valX"}, {id: 2, val: "val2"}, {
            id: 3,
            val: "val3"
        }],
        "id");
    expect(res.length).toEqual(3);
});