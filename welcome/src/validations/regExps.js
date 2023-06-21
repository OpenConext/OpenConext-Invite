export const constructShortName = name => name.normalize("NFC")
    .replace(/\s\s+/g, '_')
    .replace(/ /g, '_')
    .replace(/[^A-Za-z0-9_.]/g, '')
    .toLowerCase();
