export function stopEvent(e) {
    if (e !== undefined && e !== null) {
        e.preventDefault();
        e.stopPropagation();
        return false;
    }
    return true;
}

export function isEmpty(obj) {
    if (obj === undefined || obj === null) {
        return true;
    }
    if (Array.isArray(obj)) {
        return obj.length === 0;
    }
    if (typeof obj === "string") {
        return obj.trim().length === 0;
    }
    if (obj && obj.getTime) {
        // eslint-disable-next-line
        return obj.getTime() !== obj.getTime();
    }
    if (typeof obj === "object") {
        return Object.keys(obj).length === 0;
    }
    return false;
}

export function pseudoGuid() {
    return (crypto.randomUUID && crypto.randomUUID()) || Math.round((new Date().getTime() * Math.random() * 1000)).toString()
}

export const splitListSemantically = (arr, lastSeparator) => {
    return [arr.slice(0, -1).join(", "), arr.slice(-1)[0]].join(arr.length < 2 ? "" : ` ${lastSeparator} `);
}

export const sanitizeURL = url => {
    const protocol = new URL(url).protocol;
    return ["https:", "http:"].includes(protocol) ? url : "about:blank";
}

export const distinctValues = (arr, attribute) => {
    const distinctList = {};
    return arr.filter(obj => {
        if (distinctList[obj[attribute]]) {
            return false;
        }
        distinctList[obj[attribute]] = true;
        return true;
    })
}
