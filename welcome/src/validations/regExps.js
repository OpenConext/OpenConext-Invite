export const validUrlRegExp = /(https?|ssh|ftp):\/\/(((www\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\.?[a-z]{0,63})|(localhost))\b([-a-zA-Z0-9@:%_+.~#?&/=]*)/i

export const constructShortName = name => name.normalize("NFC")
    .trim()
    .replace(/\s\s+/g, '_')
    .replace(/ /g, '_')
    .replace(/[^A-Za-z0-9_.]/g, '')
    .toLowerCase();
