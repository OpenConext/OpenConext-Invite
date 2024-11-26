import {isEmpty} from "./Utils";
import {DateTime} from "luxon";

export function sortObjects(objects, attribute, reverse) {
    return [...objects].sort((a, b) => {
        const val1 = valueForSort(attribute, a);
        const val2 = valueForSort(attribute, b);
        if (typeof val1 === "number" && typeof val2 === "number") {
            return (val1 - val2) * (reverse ? -1 : 1);
        }
        const aS = val1 ? val1.toString() : "";
        const bS = val2 ? val2.toString() : "";
        if (aS.length === 0) {
            return (reverse ? -1 : 1);
        }
        if (bS.length === 0) {
            return (reverse ? 1 : -1);
        }
        return aS.localeCompare(bS) * (reverse ? -1 : 1);
    });
}

export function valueForSort(attribute, obj) {
    if (attribute.endsWith("_date")) {
        return obj[attribute] || Number.MAX_SAFE_INTEGER;
    }
    const val = obj[attribute];
    if (DateTime.isDateTime(val)) {
        return val.toUnixInteger();
    }
    if (!isEmpty(val)) {
        return val;
    }
    if (attribute.indexOf("__") === -1) {
        return val;
    }
    const parts = attribute.replace(/__/g, ".").split(".");
    const res = parts.reduce((acc, e) => {
        if (isEmpty(acc)) {
            return "";
        }
        return acc[e];
    }, obj);
    return res || "";

}
